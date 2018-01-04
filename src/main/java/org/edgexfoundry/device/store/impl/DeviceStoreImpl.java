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
import org.edgexfoundry.controller.DeviceProfileClient;
import org.edgexfoundry.device.store.DeviceStore;
import org.edgexfoundry.device.store.ProfileStore;
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
public class DeviceStoreImpl implements DeviceStore {

  private final EdgeXLogger logger =
      EdgeXLoggerFactory.getEdgeXLogger(this.getClass());

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
  private Map<String, Device> deviceCache;

  @Override
  public Map<String, Device> initialize(String deviceServiceId, ServiceHandler handler) {
    List<Device> metaDevices = deviceClient.devicesForService(deviceServiceId);
    deviceCache = new HashMap<>();
    for (Device device : metaDevices) {
      device.setOperatingState(OperatingState.DISABLED);
      add(device, handler);
    }
    
    logger.info("Device service has " + deviceCache.size() + " devices.");
    handler.initialize();
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
    return update(deviceId, handler);
  }

  @Override
  public boolean add(Device device, ServiceHandler handler) {
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
    if (device != null && localDevice != null) {
      synchronized (deviceCache.get(device.getName())) {
        if (compare(device,localDevice)) {
          return true;
        }
      }
    }

    return add(device, handler);
  }

  @Override
  public Map<String, Device> getDevices() {
    return deviceCache;
  }


  @Override
  public Device getDevice(String deviceName) {
    return deviceCache.get(deviceName);
  }

  @Override
  public Device getDeviceById(String deviceId) {
    return deviceCache.values().stream().filter(device -> device.getId().equals(deviceId))
        .findAny().orElse(null);
  }

  @Override
  public List<Device> getMetaDevices() {
    List<Device> metaDevices;
    metaDevices = deviceClient.devicesForServiceByName(serviceName);
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

    return device.getAdminState().equals(AdminState.LOCKED)
        || device.getOperatingState().equals(OperatingState.DISABLED);
  }

  private Device addDeviceToMetaData(Device device) {
    synchronized (deviceCache) {
      deviceCache.put(device.getName(), device);
    }
    
    synchronized(deviceCache.get(device.getName())) {
      Addressable addressable = null;
      try {
        addressableClient.addressableForName(device.getAddressable().getName());
      } catch (javax.ws.rs.NotFoundException e) {
        addressable = device.getAddressable();
        addressable.setOrigin(System.currentTimeMillis());
        logger.info("Creating new Addressable Object with name: "
            + addressable.getName() + ", Address:" + addressable);
        String addressableId = addressableClient.add(addressable);
        addressable.setId(addressableId);
        device.setAddressable(addressable);
        synchronized(deviceCache) {
          deviceCache.put(device.getName(), device);
        }
      }
  
      Device d = null;
      try {
        d = deviceClient.deviceForName(device.getName());
        device.setId(d.getId());
      } catch (javax.ws.rs.NotFoundException e) {
        logger.info("Adding Device to Metadata:" + device.getName());
        try {
          device.setId(deviceClient.add(device));
          synchronized(deviceCache) {
            deviceCache.put(device.getName(), device);
          }
        } catch (Exception f) {
          logger.error("Could not add new device " + device.getName()
              + " to metadata with error " + e.getMessage());
          return null;
        }
      }
      
      profileStore.addDevice(device);
      
      try {
        if (d != null && !device.getOperatingState().equals(d.getOperatingState())) {
          deviceClient.updateOpState(device.getId(), device.getOperatingState().name());
        }
      } catch (javax.ws.rs.NotFoundException e) {
        logger.error("Could not update operating state for device " + device.getName());
      }
    }
    
    return device;
  }

  private boolean remove(Device device, ServiceHandler handler) {
    logger.info("Removing managed device:  " + device.getName());
    synchronized(deviceCache) {
      if (deviceCache.containsKey(device.getName())) {
        deviceCache.remove(device.getName());
        deviceClient.updateOpState(device.getId(), OperatingState.DISABLED.name());
        new Thread(() -> handler.disconnectDevice(device)).start();
      }
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

  @Override
  public boolean updateProfile(String profileId, ServiceHandler handler) {
    DeviceProfile profile;
    try {
      profile = profileClient.deviceProfile(profileId);
    } catch (Exception e) {
      // No such profile exists to update
      return true;
    }

    boolean success = true;
    for (Device device: deviceCache.entrySet().stream().map(d -> d.getValue())
        .filter(d -> profile.getName().equals(d.getProfile().getName()))
        .collect(Collectors.toList())) {
      // update all devices that use the profile
      device.setProfile(profile);
      success &= update(device.getId(), handler);
    }
    return success;
  }
}
