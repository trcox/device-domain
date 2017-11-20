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

package org.edgexfoundry.device.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A list of key value pair maps containing the names, address spaces, and other details of a space to scan for new
 * devices. A scan "map" might include the scan name and a scan MAC address range to scan. A scan "map" might be
 * name and a URL to query for new devices. What goes into a scan map is based on device type and
 * device service arrangements.
 * 
 * @author Jim White
 *
 */
public class ScanList {
  private List<Map<String, String>> scanMaps = new ArrayList<>();

  public List<Map<String, String>> getScanMaps() {
    return scanMaps;
  }

  public void setScanMaps(List<Map<String, String>> scanMaps) {
    this.scanMaps = scanMaps;
  }

  public boolean add(Map<String, String> item) {
    return scanMaps.add(item);
  }

  public boolean remove(Map<String, String> item) {
    return scanMaps.remove(item);
  }
}
