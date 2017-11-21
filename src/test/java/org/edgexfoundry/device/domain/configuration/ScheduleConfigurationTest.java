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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.edgexfoundry.test.category.RequiresSpring;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ScheduleConfigurationTest.Config.class)
@TestPropertySource(locations = "file:src/test/resources/schedule.properties")
@Category(RequiresSpring.class)
public class ScheduleConfigurationTest {

  public static final String[] TEST_NAMES = {"test-interval1", "test-interval2"};
  public static final String[] TEST_FREQ = {"PT15S", "PT1M"};
  public static final String[] TEST_START = {"20170101T000000", "20170101T000000"};
  public static final String[] TEST_END = {"20171212T000000", "20171212T000000"};
  public static final String[] TEST_CRON = {"0 0 12 * * ?", "0 0 12 * * ?"};
  public static final String[] TEST_RUNONCE = {"true", "true"};

  @Autowired
  private ScheduleConfiguration config;

  @Before
  public void setup() {
    assertNotNull("Configuration not loaded", config.getNames());
  }

  @Test
  public void testInitialization() {
    assertEquals("Size not correct", 2, config.getSize());
    assertArrayEquals("Names not read correctly", TEST_NAMES, config.getNames());
    assertArrayEquals("Frequency not read correctly", TEST_FREQ, config.getFrequencys());
    assertArrayEquals("Names not read correctly", TEST_START, config.getStarts());
    assertArrayEquals("Names not read correctly", TEST_END, config.getEnds());
    assertArrayEquals("Names not read correctly", TEST_CRON, config.getCrons());
    assertArrayEquals("Names not read correctly", TEST_RUNONCE, config.getRunOnces());
  }

  @Test
  public void testNullChecks() {
    ScheduleConfiguration config = new ScheduleConfiguration();
    assertEquals("Crons not empty", 0, config.getCrons().length);
    assertEquals("Ends not empty", 0, config.getEnds().length);
    assertEquals("Frequencys not empty", 0, config.getFrequencys().length);
    assertEquals("Names not empty", 0, config.getNames().length);
    assertEquals("RunOnces not empty", 0, config.getRunOnces().length);
    assertEquals("Starts not empty", 0, config.getStarts().length);
  }

  @Configuration
  @ComponentScan("org.edgexfoundry.device.domain")
  static class Config {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
      return new PropertySourcesPlaceholderConfigurer();
    }

  }

}
