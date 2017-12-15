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
import org.edgexfoundry.domain.meta.ResourceOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.google.gson.JsonObject;

@Repository
public class ObjectStoreImpl implements ObjectStore {

  @Value("${data.cache.size:1}")
  private int cacheSize;

  @Autowired
  private ProfileStore profileStore;

  // map to each device's value cache by composition of device and object name
  private Map<String, List<String>> valueCache = new HashMap<>();

  private String buildValueKey(String deviceName, String objectName) {
    return String.format("%S~%S", deviceName, objectName);
  }

  private List<String> getValue(String deviceName, String objectName) {
    return valueCache.get(buildValueKey(deviceName, objectName));
  }

  private void putValue(String deviceName, String objectName, List<String> value) {
    valueCache.put(buildValueKey(deviceName, objectName), value);
  }

  // map to each device's reading cache by composition of device and operation id
  private Map<String, List<Reading>> readingCache = new HashMap<>();

  private String buildReadingKey(String deviceName, String operationId) {
    return String.format("%S~%S", deviceName, operationId);
  }

  private List<Reading> getReadings(String deviceName, String operationId) {
    return readingCache.get(buildReadingKey(deviceName, operationId));
  }

  private void putReadings(String deviceName, String operationId, List<Reading> readings) {
    readingCache.put(buildReadingKey(deviceName, operationId), readings);
  }

  private String generateOperationId(ResourceOperation operation, String deviceName) {
    List<ServiceObject> objectsList = createObjectsList(operation, deviceName);
    return objectsList.stream().map(o -> o.getName()).collect(Collectors.toList()).toString();
  }

  /**
   * The value could be transformed or not depending on caller
   *
   * @param device
   * @param operation
   * @param value
   */
  @Override
  public void put(String deviceName, ResourceOperation operation, String value) {
    if (value == null || value.equals("") || value.equals("{}")) {
      return;
    }

    List<ServiceObject> objectsList = createObjectsList(operation, deviceName);
    List<Reading> readings = new ArrayList<>();

    for (ServiceObject obj : objectsList) {
      String objectName = obj.getName();
      readings.add(buildReading(deviceName, objectName, value));

      synchronized (valueCache) {
        List<String> valueList = getValue(deviceName, objectName);
        if (valueList == null) {
          valueList = new ArrayList<String>();
        }

        valueList.add(0, value);

        if (valueList.size() > cacheSize) {
          valueList.remove(cacheSize - 1);
        }

        putValue(deviceName, objectName, valueList);
      }
    }

    String operationId = generateOperationId(operation, deviceName);

    synchronized (readingCache) {
      putReadings(deviceName, operationId, readings);
    }
  }

  @Override
  public String get(String deviceName, String object) {
    return get(deviceName, object, 1).get(0);
  }

  @Override
  public JsonObject get(String deviceName, ResourceOperation operation) {
    JsonObject jsonObject = new JsonObject();
    List<ServiceObject> objectsList = createObjectsList(operation, deviceName);

    for (ServiceObject obj : objectsList) {
      String objectName = obj.getName();
      jsonObject.addProperty(objectName, get(deviceName, objectName));
    }

    return jsonObject;
  }

  @Override
  public List<Reading> getResponses(String deviceName, ResourceOperation operation) {
    String operationId = generateOperationId(operation, deviceName);
    List<Reading> readings = getReadings(deviceName, operationId);

    if (readings == null) {
      readings = new ArrayList<>();
    }

    return readings;
  }

  private List<ServiceObject> createObjectsList(ResourceOperation operation, String deviceName) {
    List<ServiceObject> objectsList = new ArrayList<>();

    if (operation != null) {
      ServiceObject object = profileStore.getServiceObject(deviceName, operation.getObject());

      if (object != null) {
        if (profileStore.descriptorExists(operation.getParameter())) {
          object.setName(operation.getParameter());
          objectsList.add(object);
        } else if (profileStore.descriptorExists(object.getName())) {
          objectsList.add(object);
        }

        if (operation.getSecondary() != null) {
          for (String secondary : operation.getSecondary()) {
            if (profileStore.descriptorExists(secondary)) {
              objectsList.add(profileStore.getServiceObject(deviceName, secondary));
            }
          }
        }
      }
    }

    return objectsList;
  }

  private List<String> get(String deviceName, String object, int i) {
    List<String> values = getValue(deviceName, object);
    if (values == null || values.size() < i) {
      return new ArrayList<>();
    }

    return values.subList(0, i);
  }

  private Reading buildReading(String deviceName, String key, String value) {
    Reading reading = new Reading(key, value);
    reading.setDevice(deviceName);
    return reading;
  }
}
