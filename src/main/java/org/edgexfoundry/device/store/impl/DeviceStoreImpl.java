/*******************************************************************************
 * Copyright 2016-2017 Dell Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * @microservice: device-domain
 * @author: Jim White, Dell
 * @version: 1.0.0
 *******************************************************************************/

package org.edgexfoundry.device.store.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.edgexfoundry.controller.AddressableClient;
import org.edgexfoundry.controller.DeviceClient;
import org.edgexfoundry.device.store.DeviceStore;
import org.edgexfoundry.device.store.ProfileStore;
import org.edgexfoundry.domain.meta.Addressable;
import org.edgexfoundry.domain.meta.AdminState;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.OperatingState;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.service.handler.ServiceHandler;
import org.edgexfoundry.support.logging.client.EdgeXLogger;
import org.edgexfoundry.support.logging.client.EdgeXLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceStoreImpl implements DeviceStore {

  private static final EdgeXLogger logger =
      EdgeXLoggerFactory.getEdgeXLogger(DeviceStoreImpl.class);

  @Autowired
  private DeviceClient deviceClient;

  @Autowired
  private AddressableClient addressableClient;

  @Autowired
  private ProfileStore profileStore;

  @Value("${service.name}")
  private String serviceName;

  // cache for devices
  private Map<String, Device> deviceCache = new HashMap<>();

  @Override
  public Map<String, Device> initialize(String deviceServiceId, ServiceHandler handler) {
    List<Device> metaDevices = deviceClient.devicesForService(deviceServiceId);
    deviceCache = new HashMap<>();
    for (Device device : metaDevices) {
      deviceClient.updateOpState(device.getId(), OperatingState.disabled.name());
      add(device, handler);
    }

    logger.info("Device service has " + deviceCache.size() + " devices.");
    return getDevices();
  }

  @Override
  public boolean remove(String deviceId, ServiceHandler handler) {
    Device d = deviceCache.values().stream().filter(device -> device.getId().equals(deviceId))
        .findAny().orElse(null);

    if (d != null) {
      remove(d, handler);
    }

    return true;
  }

  @Override
  public boolean add(String deviceId, ServiceHandler handler) {
    Device device = deviceClient.device(deviceId);
    return add(device, handler);
  }

  @Override
  public boolean add(Device device, ServiceHandler handler) {
    if (deviceCache.containsKey(device.getName())) {
      deviceCache.remove(device.getName());
      profileStore.removeDevice(device);
    }

    logger.info("Adding managed device:  " + device.getName());
    Device metaDevice = addDeviceToMetaData(device);

    if (metaDevice == null) {
      remove(device, handler);
      return false;
    }

    return true;
  }

  @Override
  public boolean update(String deviceId, ServiceHandler handler) {
    Device device = deviceClient.device(deviceId);
    Device localDevice = getDeviceById(deviceId);
    if (device != null && localDevice != null && compare(device, localDevice)) {
      return true;
    }

    return add(device, handler);
  }

  @Override
  public Map<String, Device> getDevices() {
    return deviceCache;
  }


  @Override
  public Device getDevice(String deviceName) {
    if (deviceCache != null) {
      return deviceCache.get(deviceName);
    } else {
      logger.error("Device store cache is null, not returning any devices");
      return null;
    }
  }

  @Override
  public Device getDeviceById(String deviceId) {
    if (deviceCache != null) {
      return deviceCache.values().stream().filter(device -> device.getId().equals(deviceId))
          .findAny().orElse(null);
    }
    logger.error("Device store cache is null, not returning any devices");
    return null;
  }

  @Override
  public List<Device> getMetaDevices() {
    List<Device> metaDevices;
    metaDevices = deviceClient.devicesForServiceByName(serviceName);
    for (Device metaDevice : metaDevices) {
      Device device = deviceCache.get(metaDevice.getName());

      if (device != null) {
        device.setOperatingState(metaDevice.getOperatingState());
      }
    }
    return metaDevices;
  }

  @Override
  public Device getMetaDevice(String deviceName) {
    List<Device> metaDevices = getMetaDevices();
    return metaDevices.stream().filter(device -> deviceName.equals(device.getName())).findAny()
        .orElse(null);
  }

  @Override
  public Device getMetaDeviceById(String deviceId) {
    List<Device> metaDevices = getMetaDevices();
    return metaDevices.stream().filter(device -> deviceId.equals(device.getId())).findAny()
        .orElse(null);
  }

  @Override
  public List<Device> getDeviceByProfileName(String profileName) {
    return deviceCache.entrySet().stream().map(d -> d.getValue())
        .filter(d -> profileName.equals(d.getProfile().getName())).collect(Collectors.toList());
  }

  @Override
  public boolean isDeviceLocked(String deviceId) {
    Device device = getDeviceById(deviceId);
    if (device == null) {
      device = getMetaDeviceById(deviceId);
      if (device == null) {
        logger.error("Device not present with id " + deviceId);
        throw new NotFoundException("device", deviceId);
      }
    }

    return device.getAdminState().equals(AdminState.locked)
        || device.getOperatingState().equals(OperatingState.disabled);
  }

  private Device addDeviceToMetaData(Device device) {
    Addressable addressable = null;
    try {
      addressableClient.addressableForName(device.getAddressable().getName());
    } catch (javax.ws.rs.NotFoundException e) {
      addressable = device.getAddressable();
      addressable.setOrigin(System.currentTimeMillis());
      logger.info("Creating new Addressable Object with name: " + addressable.getName()
          + ", Address:" + addressable);
      String addressableId = addressableClient.add(addressable);
      addressable.setId(addressableId);
      device.setAddressable(addressable);
    }

    Device d = null;
    try {
      d = deviceClient.deviceForName(device.getName());
      device.setId(d.getId());
      if (!device.getOperatingState().equals(d.getOperatingState())) {
        deviceClient.updateOpState(device.getId(), device.getOperatingState().name());
      }
    } catch (javax.ws.rs.NotFoundException e) {
      logger.info("Adding Device to Metadata:" + device.getName());
      try {
        device.setId(deviceClient.add(device));
      } catch (Exception f) {
        logger.error("Could not add new device " + device.getName() + " to metadata with error "
            + e.getMessage());
        return null;
      }
    }

    profileStore.addDevice(device);
    deviceCache.put(device.getName(), device);
    return device;
  }

  private boolean remove(Device device, ServiceHandler handler) {
    logger.debug("Removing managed device:  " + device.getName());
    if (deviceCache.containsKey(device.getName())) {
      deviceCache.remove(device.getName());
      handler.disconnectDevice(device);
      deviceClient.updateOpState(device.getId(), OperatingState.disabled.name());
      profileStore.removeDevice(device);
    }
    return true;
  }

  private boolean compare(Device a, Device b) {
    return a.getAddressable().equals(b.getAddressable())
        && a.getAdminState().equals(b.getAdminState())
        && a.getDescription().equals(b.getDescription()) && a.getId().equals(b.getId())
        && Arrays.equals(a.getLabels(), b.getLabels()) && a.getLocation().equals(b.getLocation())
        && a.getName().equals(b.getName()) && a.getOperatingState().equals(b.getOperatingState())
        && a.getProfile().equals(b.getProfile()) && a.getService().equals(b.getService());
  }
}
