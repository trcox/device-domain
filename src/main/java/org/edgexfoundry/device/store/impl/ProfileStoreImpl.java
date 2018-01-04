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
 * @author: Tyler Cox, Dell
 * @version: 1.0.0
 *******************************************************************************/

package org.edgexfoundry.device.store.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.edgexfoundry.controller.DeviceProfileClient;
import org.edgexfoundry.controller.ValueDescriptorClient;
import org.edgexfoundry.device.domain.ServiceObject;
import org.edgexfoundry.device.domain.ServiceObjectFactory;
import org.edgexfoundry.device.store.ProfileStore;
import org.edgexfoundry.domain.common.IoTType;
import org.edgexfoundry.domain.common.ValueDescriptor;
import org.edgexfoundry.domain.meta.Command;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.DeviceObject;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.domain.meta.ProfileResource;
import org.edgexfoundry.domain.meta.PropertyValue;
import org.edgexfoundry.domain.meta.ResourceOperation;
import org.edgexfoundry.domain.meta.Units;
import org.edgexfoundry.support.logging.client.EdgeXLogger;
import org.edgexfoundry.support.logging.client.EdgeXLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Cache of value descriptors, command and objects based on whats in the DS profile
 *
 * @author Jim White
 *
 */
@Repository
public class ProfileStoreImpl implements ProfileStore {

  private final EdgeXLogger logger =
      EdgeXLoggerFactory.getEdgeXLogger(this.getClass());

  @Autowired
  private ValueDescriptorClient valueDescriptorClient;

  @Autowired
  private DeviceProfileClient deviceProfileClient;

  @Autowired
  private ServiceObjectFactory serviceObjectFactory;

  private Map<String, ValueDescriptor> valueDescriptors = new HashMap<>();

  // map to list of each device's resources keyed by the composition of device,
  // resource, and operation name
  private Map<String, List<ResourceOperation>> commands = new HashMap<>();

  // map to each device's profile objects by the composition of device and profile object name
  private Map<String, ServiceObject> objects = new HashMap<>();

  @Override
  public Map<String, List<ResourceOperation>> getCommands() {
    return commands;
  }

  private String buildCommandKey(String deviceName, String resourceName, String opName) {
    return String.format("%S~%S~%S", deviceName, resourceName, opName);
  }

  @Override
  public List<ResourceOperation> getCommandList(String deviceName, String resourceName,
      String opName) {
    return commands.get(buildCommandKey(deviceName, resourceName, opName));
  }

  @Override
  public void putCommandList(String deviceName, String resourceName, String opName,
      List<ResourceOperation> operations) {
    commands.put(buildCommandKey(deviceName, resourceName, opName), operations);
  }

  private void removeCommandLists(String deviceName) {
    Set<String> commandListKeys = new HashSet<>();
    for (String commandKey: commands.keySet()) {
      if (commandKey.startsWith(deviceName + "~")) {
        commandListKeys.add(commandKey);
      }
    }
    commands.keySet().removeAll(commandListKeys);
  }

  @Override
  public Map<String, ServiceObject> getObjects() {
    return objects;
  }

  private String buildObjectKey(String deviceName, String objectName) {
    return String.format("%S~%S", deviceName, objectName);
  }

  @Override
  public ServiceObject getServiceObject(String deviceName, String objectName) {
    return objects.get(buildObjectKey(deviceName, objectName));
  }

  @Override
  public void putServiceObject(String deviceName, String objectName, ServiceObject object) {
    objects.put(buildObjectKey(deviceName, objectName), object);
  }

  private void removeServiceObjects(String deviceName) {
    Set<String> deviceObjectKeys = new HashSet<>();
    for (String objectKey: objects.keySet()) {
      if (objectKey.startsWith(deviceName + "~")) {
        deviceObjectKeys.add(objectKey);
      }
    }
    objects.keySet().removeAll(deviceObjectKeys);
  }

  @Override
  public List<ValueDescriptor> getValueDescriptors() {
    return valueDescriptors.values().stream().collect(Collectors.toList());
  }

  @Override
  public boolean descriptorExists(String name) {
    return valueDescriptors.containsKey(name);
  }

  @Override
  public void addDevice(Device device) {
    if (completeProfile(device)) {
      updateValueDescriptors();
      List<String> usedDescriptors = retrieveUsedDescriptors(device);
      List<ResourceOperation> ops = retrieveOperations(device);
      ops = buildDeviceObjectsMap(device, ops);
      collectValueDescriptors(device, ops, usedDescriptors);
    } else {
      logger.error(
          "Device is not associated to a profile and cannot therefore be added to the caches");
    }
  }

  @Override
  public void updateDevice(Device device) {
    removeDevice(device);
    addDevice(device);
  }

  @Override
  public void removeDevice(Device device) {
    removeServiceObjects(device.getName());
    removeCommandLists(device.getName());
  }

  private ValueDescriptor buildDescriptor(String name, DeviceObject object) {
    PropertyValue value = object.getProperties().getValue();
    Units units = object.getProperties().getUnits();

    String minimum = value.getMinimum();
    String maximum = value.getMaximum();
    IoTType type = IoTType.valueOf(value.getType().substring(0, 1));
    String uomLabel = units.getDefaultValue();
    String defaultValue = value.getDefaultValue();
    String formatString = "%s";
    String[] labels = null;
    String description = object.getDescription();

    ValueDescriptor descriptor = new ValueDescriptor(name, minimum, maximum, type, uomLabel,
        defaultValue, formatString, labels, description);

    return descriptor;
  }

  private ValueDescriptor createDescriptor(String name, DeviceObject object) {
    ValueDescriptor descriptor = buildDescriptor(name, object);

    try {
      descriptor.setId(valueDescriptorClient.add(descriptor));
    } catch (Exception e) {
      logger.error("Adding Value descriptor: " + descriptor.getName() + " failed with error "
          + e.getMessage());
    }

    return descriptor;
  }

  private void updateValueDescriptors() {
    List<ValueDescriptor> descriptors;

    try {
      descriptors = valueDescriptorClient.valueDescriptors();
    } catch (Exception e) {
      descriptors = new ArrayList<>();
    }

    for (ValueDescriptor valueDescriptor : descriptors) {
      synchronized(valueDescriptors) {
        valueDescriptors.put(valueDescriptor.getName(), valueDescriptor);
      }
    }
  }

  private List<String> retrieveUsedDescriptors(Device device) {
    List<String> usedDescriptors = new ArrayList<>();

    if (device.getProfile() != null && device.getProfile().getCommands() != null) {
      for (Command command : device.getProfile().getCommands()) {
        usedDescriptors.addAll(command.associatedValueDescriptors());
      }
    }

    return usedDescriptors;
  }

  private boolean completeProfile(Device device) {
    if (device.getProfile() != null) {
      if (device.getProfile().getDeviceResources() == null) {
        DeviceProfile profile =
            deviceProfileClient.deviceProfileForName(device.getProfile().getName());
        device.setProfile(profile);
      }
      return true;
    }
    return false;
  }

  private List<ResourceOperation> retrieveOperations(Device device) {
	List<ResourceOperation> ops = new ArrayList<>();

    if (device.getProfile() != null && device.getProfile().getResources() != null) {
      for (ProfileResource resource : device.getProfile().getResources()) {
        if (resource.getGet() != null) {
          putCommandList(device.getName(), resource.getName(), "get", resource.getGet());
          ops.addAll(resource.getGet());
        }

        if (resource.getSet() != null) {
          putCommandList(device.getName(), resource.getName(), "set", resource.getSet());
          ops.addAll(resource.getSet());
        }
      }
    }

    return ops;
  }

  private void collectValueDescriptors(Device device, List<ResourceOperation> ops,
      List<String> usedDescriptors) {
    // Create a value descriptor for each parameter using its underlying object
    for (ResourceOperation op : ops) {
      ValueDescriptor descriptor;

      if (valueDescriptors.containsKey(op.getParameter())) {
        descriptor = valueDescriptors.get(op.getParameter());
      } else {
        if (!usedDescriptors.contains(op.getParameter())) {
          continue;
        }

        DeviceObject object = device.getProfile().getDeviceResources().stream()
            .filter(obj -> obj.getName().equals(op.getObject())).findAny().orElse(null);

        descriptor = createDescriptor(op.getParameter(), object);
      }

      synchronized(valueDescriptors) {
        valueDescriptors.put(descriptor.getName(),descriptor);
      }
    }
  }

  // generate default operations from an object read/write state
  private String getPermissionOp(char permission) {
    switch(permission) {
    case 'r':
      return "get";
    case 'w':
      return "set";
    default:
      return "?";
    }
  }

  private List<ResourceOperation> createResource(String objectName, String operation) {
    ResourceOperation resource = new ResourceOperation(operation, objectName);
    List<ResourceOperation> op = new ArrayList<>();
    op.add(resource);
    return op;
  }

  // put the device's profile objects in the commands map if no resource exists
  private List<ResourceOperation> createResourceMap(String deviceName, String objectName, String readWrite) {
    List<ResourceOperation> ops = new ArrayList<>();

    for (char permission: readWrite.toLowerCase().toCharArray()) {
      String operation = getPermissionOp(permission);
      List<ResourceOperation> opList = createResource(objectName, operation);
      String commandKey = buildCommandKey(deviceName, objectName, operation);

      // if there is no resource defined for an object, create one based on the
      // RW parameters
      if(!opList.isEmpty() && !commands.containsKey(commandKey)) {
        ops.addAll(opList);
        putCommandList(deviceName, objectName, operation, opList);
      }
    }

    return ops;
  }

  private List<ResourceOperation> buildDeviceObjectsMap(Device device,
      List<ResourceOperation> ops) {

    String deviceName = device.getName();

    // put the device's profile objects in the objects map
    for (DeviceObject object : device.getProfile().getDeviceResources()) {
      String objectName = object.getName();
      ServiceObject newServiceObject = serviceObjectFactory.createServiceObject(object);
      putServiceObject(deviceName, objectName, newServiceObject);

      String readWrite = object.getProperties().getValue().getReadWrite();
      ops.addAll(createResourceMap(deviceName, objectName, readWrite));
    }

    return ops;
  }
}
