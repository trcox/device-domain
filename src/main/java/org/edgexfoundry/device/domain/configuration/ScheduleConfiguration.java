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
 * Object representing the schedule(s) configuration as determined by configuration settings or
 * default values.
 *
 * The configuration allows or more than one schedule by providing comma separated list for each of
 * the properties below.
 *
 * @author Jim
 *
 */
// TODO - jpw - Tyler; should we check that all the configuration parameters have either no elements or
// are the same number of elements as name or else exit?
@Component
public class ScheduleConfiguration {

  @Value("${default.schedule.name:#{null}}")
  private String[] names = {};
  @Value("${default.schedule.start:#{null}}")
  private String[] starts;
  @Value("${default.schedule.end:#{null}}")
  private String[] ends;
  @Value("${default.schedule.frequency:#{null}}")
  private String[] frequencys;
  @Value("${default.schedule.cron:#{null}}")
  private String[] crons;
  @Value("${default.schedule.runOnce:#{null}}")
  private String[] runOnces;

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

  public String[] getStarts() {
    if (starts == null)
      starts = new String[getSize()];
    return starts;
  }

  public void setStarts(String[] starts) {
    if (starts != null)
      this.starts = starts;
  }

  public String[] getEnds() {
    if (ends == null)
      ends = new String[getSize()];
    return ends;
  }

  public void setEnds(String[] ends) {
    if (ends != null)
      this.ends = ends;
  }

  public String[] getFrequencys() {
    if (frequencys == null)
      frequencys = new String[getSize()];
    return frequencys;
  }

  public void setFrequencys(String[] frequencys) {
    if (frequencys != null)
      this.frequencys = frequencys;
  }

  public String[] getCrons() {
    if (crons == null)
      crons = new String[getSize()];
    return crons;
  }

  public void setCrons(String[] crons) {
    if (crons != null)
      this.crons = crons;
  }

  public String[] getRunOnces() {
    if (runOnces == null)
      runOnces = new String[getSize()];
    return runOnces;
  }

  public void setRunOnces(String[] runOnces) {
    if (runOnces != null)
      this.runOnces = runOnces;
  }

}
