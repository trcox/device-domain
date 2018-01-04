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
package org.edgexfoundry.device.domain;

import org.edgexfoundry.domain.meta.DeviceObject;


/**
 * Device object should someday be a service or service object in core. Both device and service
 * share the common properties for now. This helps obfuscate the 'device' nature associated with
 * DeviceObject and just renames it ServiceObject.
 *
 * @author Jim
 *
 */
@SuppressWarnings("serial")
public class ServiceObject extends DeviceObject {

  protected ServiceObject(DeviceObject object) {
    this.setName(object.getName());
    this.setTag(object.getTag());
    this.setDescription(object.getDescription());
    this.setProperties(object.getProperties());
    this.setAttributes(object.getAttributes());
  }

}
