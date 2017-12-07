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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.edgexfoundry.device.domain.ServiceObject;
import org.edgexfoundry.device.store.ObjectStore;
import org.edgexfoundry.device.store.ProfileStore;
import org.edgexfoundry.domain.core.Reading;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.ResourceOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.google.gson.JsonObject;

// TODO - jpw - needs to be simplified internally
@Repository
public class ObjectStoreImpl implements ObjectStore {

  @Value("${data.cache.size:1}")
  private int cacheSize;

  @Autowired
  private ProfileStore profileStore;

  private Map<String, Map<String, List<String>>> objectCache = new HashMap<>();

  private Map<String, Map<String, List<Reading>>> responseCache = new HashMap<>();

  /**
   * The value could be transformed or not depending on caller
   * 
   * @param device
   * @param operation
   * @param value
   */
  @Override
  public void put(Device device, ResourceOperation operation, String value) {
    if (value == null || value.equals("") || value.equals("{}")) {
      return;
    }

    List<ServiceObject> objectsList = createObjectsList(operation, device);
    String deviceId = device.getId();
    List<Reading> readings = new ArrayList<>();

    for (ServiceObject obj : objectsList) {
      String objectName = obj.getName();

      Reading reading = buildReading(objectName, value, device.getName());
      readings.add(reading);

      synchronized (objectCache) {
        if (objectCache.get(deviceId) == null) {
          objectCache.put(deviceId, new HashMap<String, List<String>>());
        }

        if (objectCache.get(deviceId).get(objectName) == null) {
          objectCache.get(deviceId).put(objectName, new ArrayList<String>());
        }

        objectCache.get(deviceId).get(objectName).add(0, value);

        if (objectCache.get(deviceId).get(objectName).size() == cacheSize) {
          objectCache.get(deviceId).get(objectName).remove(cacheSize - 1);
        }
      }
    }

    String operationId =
        objectsList.stream().map(o -> o.getName()).collect(Collectors.toList()).toString();

    synchronized (responseCache) {
      if (responseCache.get(deviceId) == null) {
        responseCache.put(deviceId, new HashMap<String, List<Reading>>());
      }

      responseCache.get(deviceId).put(operationId, readings);
    }
  }

  @Override
  public String get(String deviceId, String object) {
    return get(deviceId, object, 1).get(0);
  }

  @Override
  public JsonObject get(Device device, ResourceOperation operation) {
    JsonObject jsonObject = new JsonObject();
    List<ServiceObject> objectsList = createObjectsList(operation, device);

    for (ServiceObject obj : objectsList) {
      String objectName = obj.getName();
      jsonObject.addProperty(objectName, get(device.getId(), objectName));
    }

    return jsonObject;
  }

  @Override
  public List<Reading> getResponses(Device device, ResourceOperation operation) {
    String deviceId = device.getId();
    List<ServiceObject> objectsList = createObjectsList(operation, device);

    String operationId =
        objectsList.stream().map(o -> o.getName()).collect(Collectors.toList()).toString();

    if (responseCache.get(deviceId) == null
        || responseCache.get(deviceId).get(operationId) == null) {
      return new ArrayList<>();
    }

    return responseCache.get(deviceId).get(operationId);
  }

  private List<ServiceObject> createObjectsList(ResourceOperation operation, Device device) {
    Map<String, ServiceObject> objects = profileStore.getObjects().get(device.getName());
    List<ServiceObject> objectsList = new ArrayList<>();

    if (operation != null && objects != null) {
      ServiceObject object = objects.get(operation.getObject());

      if (profileStore.descriptorExists(operation.getParameter())) {
        object.setName(operation.getParameter());
        objectsList.add(object);
      } else if (profileStore.descriptorExists(object.getName())) {
        objectsList.add(object);
      }

      if (operation.getSecondary() != null) {
        for (String secondary : operation.getSecondary()) {
          if (profileStore.descriptorExists(secondary)) {
            objectsList.add((ServiceObject) objects.get(secondary));
          }
        }
      }
    }

    return objectsList;
  }

  private List<String> get(String deviceId, String object, int i) {
    if (objectCache.get(deviceId) == null || objectCache.get(deviceId).get(object) == null
        || objectCache.get(deviceId).get(object).size() < i) {
      return new ArrayList<>();
    }

    return objectCache.get(deviceId).get(object).subList(0, i);
  }

  private Reading buildReading(String key, String value, String deviceName) {
    Reading reading = new Reading();
    reading.setName(key);
    reading.setValue(value);
    reading.setDevice(deviceName);
    return reading;
  }
}
