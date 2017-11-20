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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.edgexfoundry.domain.core.Reading;
import org.edgexfoundry.test.category.RequiresNone;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RequiresNone.class)
public class TransactionTest {

  private Transaction transaction;

  @Before
  public void setup() {
    transaction = new Transaction();
    assertFalse("Transaction id is null", transaction.getTransactionId().isEmpty());
    assertTrue("Transaction readings are not null", transaction.getReadings().isEmpty());
  }

  @Test
  public void testNewOpId() throws IllegalAccessException {
    String opId = transaction.newOpId();
    assertNotNull("Id did not change", opId);
    @SuppressWarnings("unchecked")
    Map<String, Boolean> opIds =
        (Map<String, Boolean>) FieldUtils.readDeclaredField(transaction, "opIds", true);
    assertTrue("OpId not found in transaction", opIds.containsKey(opId));
    assertFalse("OpId should not be finished", opIds.get(opId));
    assertFalse("Transaction should not be finished", transaction.isFinished());
  }

  @Test
  public void testFinishOp() throws IllegalAccessException {
    String opId = transaction.newOpId();
    Reading reading = new Reading("key", "value");
    List<Reading> readings = new ArrayList<>();
    readings.add(reading);
    transaction.finishOp(opId, readings);
    @SuppressWarnings("unchecked")
    Map<String, Boolean> opIds =
        (Map<String, Boolean>) FieldUtils.readDeclaredField(transaction, "opIds", true);
    assertTrue("OpId not found in transaction", opIds.containsKey(opId));
    assertTrue("OpId should be finished", opIds.get(opId));
    assertTrue("Transaction should be finished", transaction.isFinished());
  }
  
  @Test
  public void testFinishOpWithNullReadings() throws IllegalAccessException {
    String opId = transaction.newOpId();
    transaction.finishOp(opId, null);
    @SuppressWarnings("unchecked")
    Map<String, Boolean> opIds =
        (Map<String, Boolean>) FieldUtils.readDeclaredField(transaction, "opIds", true);
    assertTrue("OpId not found in transaction", opIds.containsKey(opId));
    assertTrue("OpId should be finished", opIds.get(opId));
    assertTrue("Transaction should be finished", transaction.isFinished());
  }

  @Test
  public void testAddReadingToEmptyReadings() throws IllegalAccessException {
    FieldUtils.writeDeclaredField(transaction, "readings", null, true);
    Reading reading = new Reading("key", "value");
    List<Reading> readings = new ArrayList<>();
    readings.add(reading);
    String opId = transaction.newOpId();
    transaction.finishOp(opId, readings);
    assertNotNull("Transaction readings should not be null", transaction.getReadings());
  }
}
