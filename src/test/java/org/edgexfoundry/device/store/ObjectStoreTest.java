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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.edgexfoundry.device.store.impl.ObjectStoreImpl;
import org.edgexfoundry.device.store.impl.ProfileStoreImpl;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.domain.meta.ResourceOperation;
import org.edgexfoundry.test.category.RequiresNone;
import org.edgexfoundry.test.data.DeviceData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.yaml.snakeyaml.Yaml;

// TODO - add more testing when ObjectStore is cleaned up - jpw
@Category(RequiresNone.class)
public class ObjectStoreTest {

  private static final String TEST_VALUE = "test value";

  @InjectMocks
  private ObjectStoreImpl objectStore;

  @Mock
  private ProfileStoreImpl profileStore;

  private Device device;

  private ResourceOperation operation;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    device = DeviceData.newTestInstance();
    operation = new ResourceOperation();
    DeviceProfile profile = loadProfile();
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
  public void testPut() {
    objectStore.put(device, operation, TEST_VALUE);
  }

  @Test
  public void testPutNull() {
    objectStore.put(device, operation, null);
  }

  @Test
  public void testGet() {
    objectStore.get(device, operation);
  }

  @Test
  public void testGetResposnes() {
    objectStore.getResponses(device, operation);
  }
}
