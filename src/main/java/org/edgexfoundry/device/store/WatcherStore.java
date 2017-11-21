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

package org.edgexfoundry.device.store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;

import org.edgexfoundry.controller.DeviceProfileClient;
import org.edgexfoundry.controller.DeviceServiceClient;
import org.edgexfoundry.controller.ProvisionWatcherClient;
import org.edgexfoundry.device.domain.configuration.BaseProvisionWatcherConfiguration;
import org.edgexfoundry.domain.meta.DeviceService;
import org.edgexfoundry.domain.meta.ProvisionWatcher;
import org.edgexfoundry.support.logging.client.EdgeXLogger;
import org.edgexfoundry.support.logging.client.EdgeXLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author Jim
 *
 */
@Repository
public class WatcherStore {

  private static final EdgeXLogger logger = EdgeXLoggerFactory.getEdgeXLogger(WatcherStore.class);

  @Autowired
  private ProvisionWatcherClient provisionClient;

  @Autowired
  private DeviceProfileClient profileClient;

  @Autowired
  private DeviceServiceClient serviceClient;

  private Map<String, ProvisionWatcher> watchers = new HashMap<>();

  public void setWatchers(Map<String, ProvisionWatcher> watchers) {
    this.watchers = watchers;
  }

  public Map<String, ProvisionWatcher> getWatchers() {
    return watchers;
  }

  public boolean add(String provisionWatcherId) {
    // if watcher not found, 404 exception will be thrown
    ProvisionWatcher watcher = provisionClient.provisionWatcher(provisionWatcherId);
    watchers.put(watcher.getName(), watcher);
    return true;
  }

  public boolean add(ProvisionWatcher watcher) {
    if (watcher == null) {
      logger.error("Cannot add null watcher to the watcher store");
      return false;
    }
    if (watcher.getId() == null) {
      if (persistProvisionWatcher(watcher)) {
        watchers.put(watcher.getName(), watcher);
        return true;
      }
    }
    logger.error("Cannot add un-persisted watcher to the watcher store");
    return false;
  }

  public boolean remove(String provisionWatcherId) {
    ProvisionWatcher watcher = watchers.values().stream()
        .filter(w -> w.getId().equals(provisionWatcherId)).findAny().orElse(null);
    return remove(watcher);
  }

  public boolean remove(ProvisionWatcher provisionWatcher) {
    if (provisionWatcher != null) {
      return (watchers.remove(provisionWatcher.getName()) != null);
    }
    return false;
  }

  public boolean update(String provisionWatcherId) {
    remove(provisionWatcherId);
    return add(provisionWatcherId);
  }

  public boolean update(ProvisionWatcher provisionWatcher) {
    remove(provisionWatcher);
    if (provisionWatcher.getId() == null)
      return add(provisionWatcher);
    watchers.put(provisionWatcher.getName(), provisionWatcher);
    return true;
  }

  public void initialize(String deviceServiceId, BaseProvisionWatcherConfiguration configuration) {
    // load existing watchers
    List<ProvisionWatcher> existing = getExistingWatchers(deviceServiceId);
    for (ProvisionWatcher watcher : existing) {
      add(watcher);
    }
    // load watchers from configuration
    addConfiguredWatchers(deviceServiceId, configuration);
  }

  private List<ProvisionWatcher> getExistingWatchers(String deviceServiceId) {
    return provisionClient.provisionWatcherForService(deviceServiceId);
  }

  /**
   * Assuming that a configured watcher must have a name. All other parameters are optional. Also
   * assume that equal number of strings are provided for each parameter provided. In other words,
   * if names = a, b, c then identifiers must have a, b, c and not a, b or just a
   */
  private void addConfiguredWatchers(String deviceServiceId,
      BaseProvisionWatcherConfiguration configuration) {
    try {
      DeviceService service = serviceClient.deviceService(deviceServiceId);
      for (int i = 0; i < configuration.getSize(); i++) {
        if (service.getAddressable().getName().equals(configuration.getServices()[i])) {
          ProvisionWatcher watcher =
              extractProvisionWatcherFromConfiguration(configuration, service, i);
          if (watcher != null)
            add(watcher);
        }
      }
    } catch (Exception e) {
      logger.error("Problem adding Provision Watchers from configuration " + e.getMessage());
    }
  }

  private ProvisionWatcher extractProvisionWatcherFromConfiguration(
      BaseProvisionWatcherConfiguration configuration, DeviceService service, int pos) {
    ProvisionWatcher watcher = new ProvisionWatcher(configuration.getNames()[pos]);
    try {
      if (configuration.getProfiles().length > 0) {
        watcher.setProfile(profileClient.deviceProfileForName(configuration.getProfiles()[pos]));
      }
    } catch (NotFoundException nfe) {
      logger.error("Watcher's associated profile " + configuration.getProfiles()[pos]
          + " not found in metadata.  Watcher will not be created.");
      return null;
    }

    watcher.setService(service);
    if (!configuration.getIdentifiers().isEmpty()) {
      watcher.setIdentifiers(configuration.getIdentifiers().get(pos));
    }
    return watcher;
  }

  private boolean persistProvisionWatcher(ProvisionWatcher watcher) {
    if (watcher.getName() != null && watcher.getProfile() != null
        && watcher.getProfile().getId() != null) {
      try {
        watcher.setId(provisionClient.add(watcher));
        return true;
      } catch (Exception e) {
        logger.error("Error adding new provision watcher " + watcher.getName() + " error is: "
            + e.getMessage());
      }
    }
    return false;
  }
}
