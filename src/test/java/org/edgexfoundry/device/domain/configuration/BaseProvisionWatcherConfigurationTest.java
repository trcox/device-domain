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

import org.edgexfoundry.device.domain.configuration.BaseProvisionWatcherConfiguration;
import org.edgexfoundry.test.category.RequiresSpring;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = BaseProvisionWatcherConfigurationTest.Config.class)
@TestPropertySource(locations = "file:src/test/resources/watcher.properties")
@Category(RequiresSpring.class)
public class BaseProvisionWatcherConfigurationTest {

  public static final String[] TEST_NAMES = {"testWatcher1", "testWatcher2"};
  public static final String[] TEST_SERVICES = {"test-service1", "test-service2"};
  public static final String[] TEST_PROFILES = {"test-profile1", "test-profile2"};
  public static final String[] TEST_IDENTIFIERS = {"test-identifier1*.*", "test-identifier2*.*"};

  @Autowired
  private BaseProvisionWatcherConfiguration config;

  @Before
  public void setup() {
    assertNotNull("Configuration not loaded", config.getNames());
  }

  @Test
  public void testInitialization() {
    assertEquals("Size not correct", TEST_NAMES.length, config.getSize());
    assertArrayEquals("Names not read correctly", TEST_NAMES, config.getNames());
    assertArrayEquals("Services not read correctly", TEST_SERVICES, config.getServices());
    assertArrayEquals("Profiles not read correctly", TEST_PROFILES, config.getProfiles());
    assertArrayEquals("Identifers not read correctly", TEST_IDENTIFIERS,
        config.getNameIdentifierExpressions());
  }

  @Test
  public void testGetIdentifiers() {
    assertEquals("Identifier list size not correct", TEST_IDENTIFIERS.length,
        config.getIdentifiers().size());
    int i = 0;
    for (String id : TEST_IDENTIFIERS) {
      assertEquals("Names not read correctly", id, config.getIdentifiers().get(i)
          .get(BaseProvisionWatcherConfiguration.NAME_IDENTIFIER_KEY));
      i++;
    }
  }
  
  @Test
  public void testNullChecks() {
    BaseProvisionWatcherConfiguration config = new BaseProvisionWatcherConfiguration();
    assertEquals("Name identifiers not empty", 0, config.getNameIdentifierExpressions().length);
    assertEquals("Names not empty", 0, config.getNames().length);
    assertEquals("Profiles not empty", 0, config.getProfiles().length);
    assertEquals("Services not empty", 0, config.getServices().length);
  }

  @Configuration
  static class Config {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
      return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    static BaseProvisionWatcherConfiguration testConfig() {
      return new BaseProvisionWatcherConfiguration();
    }
  }

}
