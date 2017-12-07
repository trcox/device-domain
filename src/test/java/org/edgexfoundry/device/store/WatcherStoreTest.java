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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import javax.ws.rs.NotFoundException;

import org.edgexfoundry.controller.DeviceProfileClient;
import org.edgexfoundry.controller.DeviceServiceClient;
import org.edgexfoundry.controller.ProvisionWatcherClient;
import org.edgexfoundry.device.domain.configuration.BaseProvisionWatcherConfiguration;
import org.edgexfoundry.device.store.impl.WatcherStoreImpl;
import org.edgexfoundry.domain.meta.Addressable;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.domain.meta.DeviceService;
import org.edgexfoundry.domain.meta.ProvisionWatcher;
import org.edgexfoundry.test.category.RequiresNone;
import org.edgexfoundry.test.data.AddressableData;
import org.edgexfoundry.test.data.ProfileData;
import org.edgexfoundry.test.data.ProvisionWatcherData;
import org.edgexfoundry.test.data.ServiceData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Category(RequiresNone.class)
public class WatcherStoreTest {

  private static final String TEST_PW_ID = "1234";
  private static final String[] TEST_WATCHER_NAMES = {"testWatcher1", "testWatcher2"};
  private static final String[] TEST_WATCHER_SERVICE = {"test-service1", "test-service2"};
  private static final String[] TEST_WATCHER_PROFIE = {"test-profile1", "test-profile2"};
  private static final String[] TEST_WATCHER_IDS = {"test-identifier1*.*", "test-identifier2*.*"};
  private static final String TEST_SERVICE_ID = "TestService";

  @InjectMocks
  private WatcherStoreImpl watcherStore;

  @Mock
  private ProvisionWatcherClient provisionClient;

  @Mock
  private DeviceProfileClient profileClient;

  @Mock
  private DeviceServiceClient serviceClient;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testAdd() {
    ProvisionWatcher pw = ProvisionWatcherData.newTestInstance();
    when(provisionClient.provisionWatcher(TEST_PW_ID)).thenReturn(pw);
    assertTrue("Add provision watcher did not succeed", watcherStore.add(TEST_PW_ID));
    assertEquals("Provision Watcher stored is not the same as the one retreived", pw,
        watcherStore.getWatchers().get(ProvisionWatcherData.NAME));
  }

  @Test(expected = NotFoundException.class)
  public void testAddNotFound() {
    when(provisionClient.provisionWatcher(TEST_PW_ID)).thenThrow(new NotFoundException());
    watcherStore.add(TEST_PW_ID);
  }

  @Test
  public void testAddWithWatcher() {
    ProvisionWatcher pw = ProvisionWatcherData.newTestInstance();
    DeviceProfile profile = ProfileData.newTestInstance();
    profile.setId("1234");
    pw.setProfile(profile);
    when(provisionClient.add(pw)).thenReturn(TEST_PW_ID);
    assertTrue("Add provision watcher did not succeed", watcherStore.add(pw));
    assertEquals("Provision Watcher stored is not the same as the one retreived", pw,
        watcherStore.getWatchers().get(ProvisionWatcherData.NAME));
  }

  @Test
  public void testAddWithWatcherException() {
    ProvisionWatcher pw = ProvisionWatcherData.newTestInstance();
    DeviceProfile profile = ProfileData.newTestInstance();
    profile.setId("1234");
    pw.setProfile(profile);
    when(provisionClient.add(pw)).thenThrow(new RuntimeException());
    assertFalse("Add provision watcher should not succeed due to persistence error",
        watcherStore.add(pw));
  }

  @Test
  public void testAddWithNull() {
    ProvisionWatcher watcher = null;
    assertFalse("Add provision watcher should not succeed with null", watcherStore.add(watcher));
  }

  @Test
  public void testAddWithNoPersist() {
    ProvisionWatcher pw = ProvisionWatcherData.newTestInstance();
    assertFalse("Add provision watcher should not succeed when not persisted",
        watcherStore.add(pw));
  }

  @Test
  public void testRemove() {
    ProvisionWatcher pw = ProvisionWatcherData.newTestInstance();
    pw.setId(TEST_PW_ID);
    watcherStore.getWatchers().put(pw.getName(), pw);
    assertFalse("Watcher store empty before trying remove test",
        watcherStore.getWatchers().isEmpty());
    assertTrue("Remove provision watcher did not succeed", watcherStore.remove(pw));
    assertTrue("Watcher store not empty after trying remove test",
        watcherStore.getWatchers().isEmpty());
  }

  @Test
  public void testRemoveWithNull() {
    ProvisionWatcher pw = null;
    assertFalse("Remove provision watcher should not succeed with null", watcherStore.remove(pw));
  }

  @Test
  public void testRemoveWithId() {
    ProvisionWatcher pw = ProvisionWatcherData.newTestInstance();
    pw.setId(TEST_PW_ID);
    watcherStore.getWatchers().put(pw.getName(), pw);
    assertFalse("Watcher store empty before trying remove test",
        watcherStore.getWatchers().isEmpty());
    assertTrue("Remove provision watcher did not succeed", watcherStore.remove(TEST_PW_ID));
    assertTrue("Watcher store not empty after trying remove test",
        watcherStore.getWatchers().isEmpty());
  }

  @Test
  public void testUpdate() {
    ProvisionWatcher pw = ProvisionWatcherData.newTestInstance();
    pw.setId(TEST_PW_ID);
    DeviceProfile profile = ProfileData.newTestInstance();
    profile.setId("1234");
    pw.setProfile(profile);
    watcherStore.getWatchers().put(pw.getName(), pw);
    when(provisionClient.add(pw)).thenReturn(TEST_PW_ID);
    assertTrue("Update provision watcher did not succeed", watcherStore.update(pw));
    assertFalse("Watcher store is empty after trying remove test",
        watcherStore.getWatchers().isEmpty());
  }

  @Test
  public void testUpdateWithId() {
    ProvisionWatcher pw = ProvisionWatcherData.newTestInstance();
    pw.setId(TEST_PW_ID);
    DeviceProfile profile = ProfileData.newTestInstance();
    profile.setId("1234");
    pw.setProfile(profile);
    watcherStore.getWatchers().put(pw.getName(), pw);
    when(provisionClient.provisionWatcher(TEST_PW_ID)).thenReturn(pw);
    assertTrue("Update provision watcher did not succeed", watcherStore.update(TEST_PW_ID));
    assertFalse("Watcher store is empty after trying remove test",
        watcherStore.getWatchers().isEmpty());
  }

  @Test
  public void testUpdateWithUnsavedProvisionWatcher() {
    ProvisionWatcher pw = ProvisionWatcherData.newTestInstance();
    DeviceProfile profile = ProfileData.newTestInstance();
    profile.setId("1234");
    pw.setProfile(profile);
    when(provisionClient.add(pw)).thenReturn(TEST_PW_ID);
    assertTrue("Update provision watcher did not succeed", watcherStore.update(pw));
    assertFalse("Watcher store is empty after trying remove test",
        watcherStore.getWatchers().isEmpty());
  }

  @Test
  public void testInitialize() {
    BaseProvisionWatcherConfiguration config = new BaseProvisionWatcherConfiguration();
    config.setNames(TEST_WATCHER_NAMES);
    config.setNameIdentifierExpressions(TEST_WATCHER_IDS);
    config.setProfiles(TEST_WATCHER_PROFIE);
    config.setServices(TEST_WATCHER_SERVICE);
    DeviceService service = ServiceData.newTestInstance();
    Addressable addressable = AddressableData.newTestInstance();
    addressable.setName(TEST_WATCHER_SERVICE[0]);
    service.setAddressable(addressable);
    DeviceProfile profile = ProfileData.newTestInstance();
    profile.setId("1234");
    when(serviceClient.deviceService(TEST_SERVICE_ID)).thenReturn(service);
    when(provisionClient.add(anyObject())).thenReturn(TEST_PW_ID);
    when(profileClient.deviceProfileForName(TEST_WATCHER_PROFIE[0])).thenReturn(profile);
    watcherStore.initialize(TEST_SERVICE_ID, config);
    assertEquals("Number of watchers loaded from config is incorrect", 1,
        watcherStore.getWatchers().size());
    assertNotNull("Config watcher not loaded correctly",
        watcherStore.getWatchers().get(TEST_WATCHER_NAMES[0]));
  }

  @Test
  public void testInitializeProfileError() {
    BaseProvisionWatcherConfiguration config = new BaseProvisionWatcherConfiguration();
    config.setNames(TEST_WATCHER_NAMES);
    config.setNameIdentifierExpressions(TEST_WATCHER_IDS);
    config.setProfiles(TEST_WATCHER_PROFIE);
    config.setServices(TEST_WATCHER_SERVICE);
    DeviceService service = ServiceData.newTestInstance();
    Addressable addressable = AddressableData.newTestInstance();
    addressable.setName(TEST_WATCHER_SERVICE[0]);
    service.setAddressable(addressable);
    when(serviceClient.deviceService(TEST_SERVICE_ID)).thenReturn(service);
    when(provisionClient.add(anyObject())).thenReturn(TEST_PW_ID);
    when(profileClient.deviceProfileForName(TEST_WATCHER_PROFIE[0]))
        .thenThrow(new NotFoundException());
    watcherStore.initialize(TEST_SERVICE_ID, config);
    assertEquals("Config watcher should not have loaded", 0, watcherStore.getWatchers().size());
  }
  
  @Test
  public void testInitializeDeviceServiceError() {
    BaseProvisionWatcherConfiguration config = new BaseProvisionWatcherConfiguration();
    config.setNames(TEST_WATCHER_NAMES);
    config.setNameIdentifierExpressions(TEST_WATCHER_IDS);
    config.setProfiles(TEST_WATCHER_PROFIE);
    config.setServices(TEST_WATCHER_SERVICE);
    when(serviceClient.deviceService(TEST_SERVICE_ID)).thenThrow(new RuntimeException());
    watcherStore.initialize(TEST_SERVICE_ID, config);
    assertEquals("Config watcher should not have loaded", 0, watcherStore.getWatchers().size());
  }

}
