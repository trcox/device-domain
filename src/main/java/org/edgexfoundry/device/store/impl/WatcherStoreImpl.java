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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;

import org.edgexfoundry.controller.DeviceProfileClient;
import org.edgexfoundry.controller.DeviceServiceClient;
import org.edgexfoundry.controller.ProvisionWatcherClient;
import org.edgexfoundry.device.domain.configuration.BaseProvisionWatcherConfiguration;
import org.edgexfoundry.device.store.WatcherStore;
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
public class WatcherStoreImpl implements WatcherStore {

  private static final EdgeXLogger logger =
      EdgeXLoggerFactory.getEdgeXLogger(WatcherStoreImpl.class);

  @Autowired
  private ProvisionWatcherClient provisionClient;

  @Autowired
  private DeviceProfileClient profileClient;

  @Autowired
  private DeviceServiceClient serviceClient;

  private Map<String, ProvisionWatcher> watcherCache = new HashMap<>();

  @Override
  public void setWatchers(Map<String, ProvisionWatcher> watchers) {
    this.watcherCache = watchers;
  }

  @Override
  public Map<String, ProvisionWatcher> getWatchers() {
    return watcherCache;
  }

  @Override
  public boolean add(String provisionWatcherId) {
    // if watcher not found, 404 exception will be thrown
    ProvisionWatcher watcher = provisionClient.provisionWatcher(provisionWatcherId);
    watcherCache.put(watcher.getName(), watcher);
    return true;
  }

  @Override
  public boolean add(ProvisionWatcher watcher) {
    if (watcher == null) {
      logger.error("Cannot add null watcher to the watcher store");
      return false;
    }
    if (watcher.getId() == null && persistProvisionWatcher(watcher)) {
      watcherCache.put(watcher.getName(), watcher);
      return true;
    }
    logger.error("Cannot add un-persisted watcher to the watcher store");
    return false;
  }

  @Override
  public boolean remove(String provisionWatcherId) {
    ProvisionWatcher watcher = watcherCache.values().stream()
        .filter(w -> w.getId().equals(provisionWatcherId)).findAny().orElse(null);
    return remove(watcher);
  }

  @Override
  public boolean remove(ProvisionWatcher provisionWatcher) {
    if (provisionWatcher != null) {
      return (watcherCache.remove(provisionWatcher.getName()) != null);
    }
    return false;
  }

  @Override
  public boolean update(String provisionWatcherId) {
    remove(provisionWatcherId);
    return add(provisionWatcherId);
  }

  @Override
  public boolean update(ProvisionWatcher provisionWatcher) {
    remove(provisionWatcher);
    if (provisionWatcher.getId() == null)
      return add(provisionWatcher);
    watcherCache.put(provisionWatcher.getName(), provisionWatcher);
    return true;
  }

  @Override
  public void initialize(String deviceServiceId, BaseProvisionWatcherConfiguration configuration) {
    // load existing watchers
    List<ProvisionWatcher> existing = getExistingWatchers(deviceServiceId);
    for (ProvisionWatcher watcher : existing) {
      add(watcher);
    }
    // load watchers from configuration
    addConfiguredWatchers(deviceServiceId, configuration);
  }

  @Override
  public List<ProvisionWatcher> getWatcherByProfileName(String profileName) {
    return watcherCache.entrySet().stream().map(d -> d.getValue())
        .filter(d -> profileName.equals(d.getProfile().getName())).collect(Collectors.toList());
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
