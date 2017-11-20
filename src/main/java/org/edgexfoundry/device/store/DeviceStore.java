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

package org.edgexfoundry.device.store;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.edgexfoundry.controller.AddressableClient;
import org.edgexfoundry.controller.DeviceClient;
import org.edgexfoundry.controller.DeviceProfileClient;
import org.edgexfoundry.domain.meta.Addressable;
import org.edgexfoundry.domain.meta.AdminState;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.domain.meta.OperatingState;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.service.handler.ServiceHandler;
import org.edgexfoundry.support.logging.client.EdgeXLogger;
import org.edgexfoundry.support.logging.client.EdgeXLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceStore {

  private static final EdgeXLogger logger = EdgeXLoggerFactory.getEdgeXLogger(DeviceStore.class);

  @Autowired
  private DeviceClient deviceClient;

  @Autowired
  private AddressableClient addressableClient;

  @Autowired
  private DeviceProfileClient profileClient;

  @Autowired
  private ProfileStore profileStore;

  @Value("${service.name}")
  private String serviceName;

  // cache for devices
  private Map<String, Device> deviceCache = new HashMap<>();

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

  public boolean remove(String deviceId, ServiceHandler handler) {
    Device d = deviceCache.values().stream().filter(device -> device.getId().equals(deviceId))
        .findAny().orElse(null);

    if (d != null) {
      remove(d, handler);
    }

    return true;
  }

  public boolean add(String deviceId, ServiceHandler handler) {
    Device device = deviceClient.device(deviceId);
    return add(device, handler);
  }

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

  private Device addDeviceToMetaData(Device device) {
    // Create a new addressable Object with the devicename + last 6 digits of MAC address.
    // Assume this to be unique

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

  public boolean update(String deviceId, ServiceHandler handler) {
    Device device = deviceClient.device(deviceId);
    Device localDevice = getDeviceById(deviceId);
    if (device != null && localDevice != null && compare(device, localDevice)) {
      return true;
    }

    return add(device, handler);
  }

  private boolean compare(Device a, Device b) {
    return a.getAddressable().equals(b.getAddressable())
        && a.getAdminState().equals(b.getAdminState())
        && a.getDescription().equals(b.getDescription()) && a.getId().equals(b.getId())
        && Arrays.equals(a.getLabels(), b.getLabels()) && a.getLocation().equals(b.getLocation())
        && a.getName().equals(b.getName()) && a.getOperatingState().equals(b.getOperatingState())
        && a.getProfile().equals(b.getProfile()) && a.getService().equals(b.getService());
  }

  public Map<String, Device> getDevices() {
    return deviceCache;
  }

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

  public Device getMetaDevice(String deviceName) {
    List<Device> metaDevices = getMetaDevices();
    return metaDevices.stream().filter(device -> deviceName.equals(device.getName())).findAny()
        .orElse(null);
  }

  public Device getMetaDeviceById(String deviceId) {
    List<Device> metaDevices = getMetaDevices();
    return metaDevices.stream().filter(device -> deviceId.equals(device.getId())).findAny()
        .orElse(null);
  }

  public Device getDevice(String deviceName) {
    if (deviceCache != null) {
      return deviceCache.get(deviceName);
    } else {
      return null;
    }
  }

  public Device getDeviceById(String deviceId) {
    if (deviceCache != null) {
      return deviceCache.values().stream().filter(device -> device.getId().equals(deviceId))
          .findAny().orElse(null);
    }

    return null;
  }

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

  public void setDeviceOpState(String deviceName, OperatingState state) {
    deviceClient.updateOpStateByName(deviceName, state.name());
  }

  public void setDeviceByIdOpState(String deviceId, OperatingState state) {
    deviceClient.updateOpState(deviceId, state.name());
  }

  public boolean updateProfile(String profileId, ServiceHandler handler) {
    DeviceProfile profile;
    try {
      profile = profileClient.deviceProfile(profileId);
    } catch (Exception e) {
      // No such profile exists to update
      return true;
    }

    boolean success = true;
    for (Device device : deviceCache.entrySet().stream().map(d -> d.getValue())
        .filter(d -> profile.getName().equals(d.getProfile().getName()))
        .collect(Collectors.toList())) {
      // update all devices that use the profile
      device.setProfile(profile);
      success &= update(device.getId(), handler);
    }
    return success;
  }
}
