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
package org.edgexfoundry.device.domain.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

/**
 * Represents a basic provision watcher configuration.
 *
 * @author Jim
 *
 */
// TODO - jpw - Tyler, what happens if the comma separated lists don't match? One is longer or
// shorter than the other?
public class BaseProvisionWatcherConfiguration {

  public static final String NAME_IDENTIFIER_KEY = "name";

  // comma separated list of provision watcher names
  @Value("${default.watcher.name:#{null}}")
  protected String[] names = {};
  // comma separated list of profile names
  @Value("${default.watcher.profile:#{null}}")
  protected String[] profiles;
  // comma separated list of service names
  @Value("${default.watcher.service:#{null}}")
  protected String[] services;
  // comma separated list of identifier regular expressions.
  @Value("${default.watcher.name_identifiers:#{null}}")
  protected String[] nameIdentifierExpressions;

  public int getSize() {
    return names.length;
  }

  public String[] getNames() {
    return names;
  }

  public void setNames(String[] names) {
    if (names != null)
      this.names = names;
  }

  public String[] getProfiles() {
    if (profiles == null)
      profiles = new String[getSize()];
    return profiles;
  }

  public void setProfiles(String[] profiles) {
    if (profiles != null)
      this.profiles = profiles;
  }

  public String[] getServices() {
    if (services == null)
      services = new String[getSize()];
    return services;
  }

  public void setServices(String[] services) {
    if (services != null)
      this.services = services;
  }

  public String[] getNameIdentifierExpressions() {
    if (nameIdentifierExpressions == null)
      nameIdentifierExpressions = new String[getSize()];
    return nameIdentifierExpressions;
  }

  public void setNameIdentifierExpressions(String[] nameIdentifierExpressions) {
    if (nameIdentifierExpressions != null)
      this.nameIdentifierExpressions = nameIdentifierExpressions;
  }

  public List<Map<String, String>> getIdentifiers() {
    List<Map<String, String>> identifiers = new ArrayList<>();
    for (String nameIdExpression : getNameIdentifierExpressions()) {
      Map<String, String> ident = new HashMap<>();
      ident.put(NAME_IDENTIFIER_KEY, nameIdExpression);
      identifiers.add(ident);
    }
    return identifiers;
  }

}
