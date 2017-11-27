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

package org.edgexfoundry.device.domain.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Object representing the schedule event(s) configuration as determined by configuration settings
 * or default values.
 * 
 * The configuration allows or more than one schedule event by providing comma separated list for
 * each of the properties below.
 * 
 * @author Jim
 *
 */
// TODO - jpw - Tyler; should we check that all the configuration parameters have either no elements or
// are the same number of elements as name or else exit?
@Component
public class ScheduleEventConfiguration {

  @Value("${default.scheduleEvent.name:#{null}}")
  private String[] names = {};
  @Value("${default.scheduleEvent.schedule:#{null}}")
  private String[] schedules;
  @Value("${default.scheduleEvent.parameters:#{null}}")
  private String[] parameters;
  @Value("${default.scheduleEvent.service:#{null}}")
  private String[] services;
  @Value("${default.scheduleEvent.path:#{null}}")
  private String[] paths;
  @Value("${default.scheduleEvent.scheduler:#{null}}")
  private String[] schedulers;

  public int getSize() {
    return names.length;
  }

  public String[] getNames() {
    if (names == null)
      names = new String[getSize()];
    return names;
  }

  public void setNames(String[] names) {
    if (names != null)
      this.names = names;
  }

  public String[] getSchedules() {
    if (schedules == null)
      schedules = new String[getSize()];
    return schedules;
  }

  public void setSchedules(String[] schedules) {
    if (schedules != null)
      this.schedules = schedules;
  }

  public String[] getParameters() {
    if (parameters == null)
      parameters = new String[getSize()];
    return parameters;
  }

  public void setParameters(String[] parameters) {
    if (parameters != null)
      this.parameters = parameters;
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

  public String[] getPaths() {
    if (paths == null)
      paths = new String[getSize()];
    return paths;
  }

  public void setPaths(String[] paths) {
    if (paths != null)
      this.paths = paths;
  }

  public String[] getSchedulers() {
    if (schedulers == null)
      schedulers = new String[getSize()];
    return schedulers;
  }

  public void setSchedulers(String[] schedulers) {
    if (schedulers != null)
      this.schedulers = schedulers;
  }

}
