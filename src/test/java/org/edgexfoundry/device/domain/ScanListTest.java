/*******************************************************************************
 * Copyright 2016-2017 Dell Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @microservice: device-domain
 * @author: Jim White, Dell
 * @version: 1.0.0
 *******************************************************************************/

package org.edgexfoundry.device.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.edgexfoundry.device.domain.ScanList;
import org.edgexfoundry.test.category.RequiresNone;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RequiresNone.class)
public class ScanListTest {

  private ScanList list;

  @Before
  public void setup() {
    list = new ScanList();
  }

  @Test
  public void testAdd() {
    Map<String, String> scanMap = new HashMap<>();
    scanMap.put("foo", "bar");
    assertTrue("Scan map did not get added correctly", list.add(scanMap));
    assertEquals("Scan map added not found", scanMap, list.getScanMaps().get(0));
  }

  @Test
  public void testRemove() {
    Map<String, String> scanMap = new HashMap<>();
    scanMap.put("foo", "bar");
    assertTrue("Scan map did not get added correctly", list.add(scanMap));
    assertEquals("Scan map added not found", scanMap, list.getScanMaps().get(0));
    assertTrue("Scan map did not get removed correctly", list.remove(scanMap));
    assertTrue("List should be empty after removal", list.getScanMaps().isEmpty());
  }
  
}
