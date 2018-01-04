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

import java.util.List;
import java.util.Map;

import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.service.handler.ServiceHandler;

public interface DeviceStore {

  Map<String, Device> initialize(String deviceServiceId, ServiceHandler handler);

  boolean remove(String deviceId, ServiceHandler handler);

  boolean add(String deviceId, ServiceHandler handler);

  boolean add(Device device, ServiceHandler handler);

  boolean update(String deviceId, ServiceHandler handler);

  Map<String, Device> getDevices();

  Device getDevice(String deviceName);

  Device getDeviceById(String deviceId);

  Device getMetaDevice(String deviceName);

  Device getMetaDeviceById(String deviceId);

  List<Device> getMetaDevices();

  List<Device> getDeviceByProfileName(String profileName);

  boolean isDeviceLocked(String deviceId);
  
  boolean updateProfile(String profileId, ServiceHandler handler);
}
