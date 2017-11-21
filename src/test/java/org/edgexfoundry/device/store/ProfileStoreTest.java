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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.edgexfoundry.controller.DeviceProfileClient;
import org.edgexfoundry.controller.ValueDescriptorClient;
import org.edgexfoundry.domain.common.ValueDescriptor;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.test.category.RequiresNone;
import org.edgexfoundry.test.data.DeviceData;
import org.edgexfoundry.test.data.ValueDescriptorData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.yaml.snakeyaml.Yaml;

@Category(RequiresNone.class)
public class ProfileStoreTest {


  @InjectMocks
  private ProfileStore profileStore;

  @Mock
  private ValueDescriptorClient valueDescriptorClient;

  @Mock
  private DeviceProfileClient deviceProfileClient;

  private List<ValueDescriptor> valueDescriptors;
  private ValueDescriptor descriptor;
  private Device device;
  private DeviceProfile profile;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    descriptor = ValueDescriptorData.newTestInstance();
    valueDescriptors = new ArrayList<>();
    valueDescriptors.add(descriptor);
    device = DeviceData.newTestInstance();
    profile = loadProfile();
    device.setProfile(profile);
  }

  private DeviceProfile loadProfile() throws IOException {
    Path path = Paths.get("./src/test/resources/JC.RR-NAE-9.ConfRoom.Padre.Island.profile.yaml");
    byte[] data = Files.readAllBytes(path);
    String yamlContent = new String(data);
    Yaml yaml = new Yaml();
    return yaml.loadAs(yamlContent, DeviceProfile.class);
  }

  @Test
  public void testDescriptorExists() throws IllegalAccessException {
    FieldUtils.writeField(profileStore, "valueDescriptors", valueDescriptors, true);
    assertTrue("Value descriptor not found in cached list of value descriptors",
        profileStore.descriptorExists(descriptor.getName()));
  }

  @Test
  public void testDescriptorExistsNotFound() throws IllegalAccessException {
    FieldUtils.writeField(profileStore, "valueDescriptors", valueDescriptors, true);
    assertFalse("Non existent value descriptor found in list of value descriptors",
        profileStore.descriptorExists("UNKNOWN"));
  }

  @Test
  public void testAddDeviceNoProfile() {
    device.setProfile(null);
    profileStore.addDevice(device);
    assertTrue("No value descriptors should be in cache",
        profileStore.getValueDescriptors().isEmpty());
    assertTrue("No commands should be in cache", profileStore.getCommands().isEmpty());
    assertTrue("No objects should be in cache", profileStore.getObjects().isEmpty());
  }

  @Test
  public void testAddDevice() {
    when(valueDescriptorClient.valueDescriptors()).thenReturn(valueDescriptors);
    profileStore.addDevice(device);
    assertEquals("Commands should be in cache", 1, profileStore.getCommands().size());
    assertEquals("Objects should be in cache", 1, profileStore.getObjects().size());
    assertEquals("Value descriptors should be in cache", 2,
        profileStore.getValueDescriptors().size());
  }

  @Test
  public void testRemoveDevice() {
    when(valueDescriptorClient.valueDescriptors()).thenReturn(valueDescriptors);
    profileStore.addDevice(device);
    assertEquals("Commands should be in cache", 1, profileStore.getCommands().size());
    assertEquals("Objects should be in cache", 1, profileStore.getObjects().size());
    profileStore.removeDevice(device);
    assertTrue("No commands should be in cache", profileStore.getCommands().isEmpty());
    assertTrue("No objects should be in cache", profileStore.getObjects().isEmpty());
  }

  @Test
  public void testUpdateDevice() {
    when(valueDescriptorClient.valueDescriptors()).thenReturn(valueDescriptors);
    profileStore.addDevice(device);
    assertEquals("Commands should be in cache", 1, profileStore.getCommands().size());
    assertEquals("Objects should be in cache", 1, profileStore.getObjects().size());
    assertEquals("Value descriptors should be in cache", 2,
        profileStore.getValueDescriptors().size());
    profileStore.updateDevice(device);
    assertEquals("Commands should be in cache", 1, profileStore.getCommands().size());
    assertEquals("Objects should be in cache", 1, profileStore.getObjects().size());
    assertEquals("Value descriptors should be in cache", 2,
        profileStore.getValueDescriptors().size());
  }

}
