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

import org.edgexfoundry.device.domain.ServiceObject;
import org.edgexfoundry.domain.common.ValueDescriptor;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.ResourceOperation;

public interface ProfileStore {

  Map<String, List<ResourceOperation>> getCommands();

  List<ResourceOperation> getCommandList(String deviceName, String resourceName, String opName);

  void putCommandList(String deviceName, String resourceName, String opName,
      List<ResourceOperation> operations);

  Map<String, ServiceObject> getObjects();

  ServiceObject getServiceObject(String deviceName, String objectName);

  void putServiceObject(String deviceName, String objectName, ServiceObject object);

  List<ValueDescriptor> getValueDescriptors();

  boolean descriptorExists(String name);

  void addDevice(Device device);

  void updateDevice(Device device);

  void removeDevice(Device device);

}
