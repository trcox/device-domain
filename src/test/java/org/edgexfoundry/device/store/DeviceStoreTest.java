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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.edgexfoundry.controller.AddressableClient;
import org.edgexfoundry.controller.DeviceClient;
import org.edgexfoundry.domain.meta.Addressable;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.domain.meta.DeviceService;
import org.edgexfoundry.service.handler.ServiceHandler;
import org.edgexfoundry.test.category.RequiresNone;
import org.edgexfoundry.test.data.AddressableData;
import org.edgexfoundry.test.data.DeviceData;
import org.edgexfoundry.test.data.ProfileData;
import org.edgexfoundry.test.data.ServiceData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Category(RequiresNone.class)
public class DeviceStoreTest {

  private static final String TEST_DEVICE_ID = "1234";
  private static final String TEST_SERVICE_NAME = "TEST_SERVICE";

  @InjectMocks
  private DeviceStore deviceStore;

  @Mock
  private DeviceClient deviceClient;

  @Mock
  private AddressableClient addressableClient;

  @Mock
  private ProfileStore profileStore;

  @Mock
  private ServiceHandler handler;

  private Device device;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    device = DeviceData.newTestInstance();
    Addressable addressable = AddressableData.newTestInstance();
    device.setAddressable(addressable);
    device.setId(TEST_DEVICE_ID);
  }

  @Test
  public void testInitialize() throws IllegalAccessException {
    FieldUtils.writeField(deviceStore, "serviceName", TEST_SERVICE_NAME, true);
    // add device to cache first
    when(deviceClient.deviceForName(device.getName())).thenReturn(device);
    assertTrue("Device store add did not happen successfully", deviceStore.add(device, handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
    // prep for metadevices
    List<Device> devices = new ArrayList<>();
    devices.add(device);
    when(deviceClient.devicesForService(TEST_SERVICE_NAME)).thenReturn(devices);
    Map<String, Device> map = deviceStore.initialize(TEST_SERVICE_NAME, handler);
    assertEquals("Cache not initialized appropriately", 1, map.size());
    assertEquals("Cache not initialized appropriately", device, map.get(device.getName()));
  }

  @Test
  public void testAdd() {
    when(deviceClient.deviceForName(device.getName())).thenReturn(device);
    assertTrue("Device store add did not happen successfully", deviceStore.add(device, handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
  }

  @Test
  public void testAddWithId() {
    when(deviceClient.device(device.getId())).thenReturn(device);
    when(deviceClient.deviceForName(device.getName())).thenReturn(device);
    assertTrue("Device store add did not happen successfully",
        deviceStore.add(device.getId(), handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
  }

  @Test
  public void testAddWithExisting() {
    when(deviceClient.deviceForName(device.getName())).thenReturn(device);
    assertTrue("First device store add did not happen successfully",
        deviceStore.add(device, handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
    assertTrue("Second device store add did not happen successfully",
        deviceStore.add(device, handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
  }

  @Test
  public void testAddNoDeviceFound() {
    when(deviceClient.deviceForName(device.getName())).thenThrow(new NotFoundException());
    when(deviceClient.add(device)).thenThrow(new RuntimeException());
    assertFalse("Device store add should not happen successfully",
        deviceStore.add(device, handler));
    assertNull("Device store does not contain the device", deviceStore.getDevice(device.getName()));
  }

  @Test
  public void testAddDeviceAddException() {
    when(deviceClient.deviceForName(device.getName())).thenThrow(new NotFoundException());
    assertTrue("Device store add did not happen successfully", deviceStore.add(device, handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
  }

  @Test
  public void testAddNoAddressableFound() {
    when(deviceClient.deviceForName(device.getName())).thenReturn(device);
    when(addressableClient.addressableForName(device.getAddressable().getName()))
        .thenThrow(new NotFoundException());
    assertTrue("Device store add did not happen successfully", deviceStore.add(device, handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
  }

  @Test
  public void testRemoveFromEmptyCache() {
    assertTrue("Attempt to remove from empty cache did not happen successfully",
        deviceStore.remove(device.getId(), handler));
  }

  @Test
  public void testRemove() {
    // first add the device
    when(deviceClient.deviceForName(device.getName())).thenReturn(device);
    assertTrue("Device store add did not happen successfully", deviceStore.add(device, handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
    assertTrue("Device was not removed from device store successfully",
        deviceStore.remove(device.getId(), handler));
  }

  @Test
  public void testUpdateThatAdds() {
    when(deviceClient.device(device.getId())).thenReturn(device);
    when(deviceClient.deviceForName(device.getName())).thenReturn(device);
    assertTrue("Device store update did not happen successfully",
        deviceStore.update(device.getId(), handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
  }

  @Test
  public void testUpdate() {
    DeviceProfile profile = ProfileData.newTestInstance();
    DeviceService service = ServiceData.newTestInstance();
    device.setProfile(profile);
    device.setService(service);
    // first add
    when(deviceClient.device(device.getId())).thenReturn(device);
    when(deviceClient.deviceForName(device.getName())).thenReturn(device);
    assertTrue("Device store add did not happen successfully",
        deviceStore.add(device.getId(), handler));
    // then update
    assertTrue("Device store update did not happen successfully",
        deviceStore.update(device.getId(), handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
  }

  @Test
  public void testGetDevices() {
    assertTrue("Cache is not empty", deviceStore.getDevices().isEmpty());
  }

  @Test
  public void testGetDeviceCacheNull() throws IllegalAccessException {
    FieldUtils.writeField(deviceStore, "deviceCache", null, true);
    assertNull("Null cache should return null for any get",
        deviceStore.getDevice(device.getName()));
  }

  @Test
  public void testGetDeviceByIdCacheNull() throws IllegalAccessException {
    FieldUtils.writeField(deviceStore, "deviceCache", null, true);
    assertNull("Null cache should return null for any get",
        deviceStore.getDeviceById(TEST_DEVICE_ID));
  }

  @Test
  public void testGetMetaDevices() throws IllegalAccessException {
    FieldUtils.writeField(deviceStore, "serviceName", TEST_SERVICE_NAME, true);
    // add device to cache first
    when(deviceClient.deviceForName(device.getName())).thenReturn(device);
    assertTrue("Device store add did not happen successfully", deviceStore.add(device, handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
    // test metadevices
    List<Device> devices = new ArrayList<>();
    devices.add(device);
    when(deviceClient.devicesForServiceByName(TEST_SERVICE_NAME)).thenReturn(devices);
    assertEquals("Device from metadevice should equal device from cache.", device,
        deviceStore.getMetaDevice(device.getName()));
  }

  @Test
  public void testGetMetaDevicesById() throws IllegalAccessException {
    FieldUtils.writeField(deviceStore, "serviceName", TEST_SERVICE_NAME, true);
    // add device to cache first
    when(deviceClient.deviceForName(device.getName())).thenReturn(device);
    assertTrue("Device store add did not happen successfully", deviceStore.add(device, handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
    // test metadevices
    List<Device> devices = new ArrayList<>();
    devices.add(device);
    when(deviceClient.devicesForServiceByName(TEST_SERVICE_NAME)).thenReturn(devices);
    assertEquals("Device from metadevice should equal device from cache.", device,
        deviceStore.getMetaDeviceById(device.getId()));
  }

  @Test
  public void testIsDeviceLocked() {
    // first add device to cache
    when(deviceClient.deviceForName(device.getName())).thenReturn(device);
    assertTrue("Device store add did not happen successfully", deviceStore.add(device, handler));
    assertNotNull("Device store does not contain the device",
        deviceStore.getDevice(device.getName()));
    assertFalse("Device should not be locked", deviceStore.isDeviceLocked(device.getId()));
  }

  @Test
  public void testIsDeviceLockedNotInCache() throws IllegalAccessException {
    FieldUtils.writeField(deviceStore, "serviceName", TEST_SERVICE_NAME, true);
    List<Device> devices = new ArrayList<>();
    devices.add(device);
    when(deviceClient.devicesForServiceByName(TEST_SERVICE_NAME)).thenReturn(devices);
    assertFalse("Device should be locked", deviceStore.isDeviceLocked(device.getId()));
  }

  @Test(expected = org.edgexfoundry.exception.controller.NotFoundException.class)
  public void testIsDeviceLockedNotFound() throws IllegalAccessException {
    FieldUtils.writeField(deviceStore, "serviceName", TEST_SERVICE_NAME, true);
    deviceStore.isDeviceLocked(device.getId());
  }
}
