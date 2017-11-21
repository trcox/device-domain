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

import org.edgexfoundry.device.domain.configuration.ScheduleEventConfiguration;
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
@ContextConfiguration(classes = ScheduleEventConfigurationTest.Config.class)
@TestPropertySource(locations = "file:src/test/resources/schedule.properties")
@Category(RequiresSpring.class)
public class ScheduleEventConfigurationTest {

  public static final String[] TEST_NAMES = {"test-service-discovery", "test-service-cleanup"};
  public static final String[] TEST_PATHS = {"/api/v1/discovery", "/api/v1/cleanup"};
  private static final String[] TEST_SERVICES = {"test-service1", "test-service2"};
  private static final String[] TEST_SCHEDULES = {"test-interval1", "test-interval2"};
  private static final String[] TEST_SCHEDULERS = {"a", "b"};

  @Autowired
  private ScheduleEventConfiguration config;

  @Before
  public void setup() {
    assertNotNull("Configuration not loaded", config.getNames());
  }

  @Test
  public void testInitialization() {
    assertEquals("Size not correct", 2, config.getSize());
    assertArrayEquals("Names not read correctly", TEST_NAMES, config.getNames());
    assertArrayEquals("Frequency not read correctly", TEST_PATHS, config.getPaths());
    assertArrayEquals("Services not read correctly", TEST_SERVICES, config.getServices());
    assertArrayEquals("Schedules not read correctly", TEST_SCHEDULES, config.getSchedules());
    assertArrayEquals("Schedulers not read correctly", TEST_SCHEDULERS, config.getSchedulers());
  }

  @Test
  public void testNullChecks() {
    ScheduleEventConfiguration config = new ScheduleEventConfiguration();
    assertEquals("Names not empty", 0, config.getNames().length);
    assertEquals("Parameters not empty", 0, config.getParameters().length);
    assertEquals("Paths not empty", 0, config.getPaths().length);
    assertEquals("Schedulers not empty", 0, config.getSchedulers().length);
    assertEquals("Schedules not empty", 0, config.getSchedules().length);
    assertEquals("Services not empty", 0, config.getServices().length);
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
