/*
 * Copyright 2019 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.bigtable.grpc.async;

import com.google.bigtable.v2.MutateRowResponse;
import com.google.bigtable.v2.MutateRowsRequest;
import com.google.cloud.bigtable.core.IBulkMutation;
import com.google.cloud.bigtable.data.v2.internal.RequestContext;
import com.google.cloud.bigtable.data.v2.models.ReadModifyWriteRow;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class) public class TestBulkMutationWrapper {

  private BulkMutation mockDelegate;
  private RequestContext requestContext =
      RequestContext.create("ProjectId", "instanceId", "appProfileId");
  private IBulkMutation bulkWrapper;

  @Before
  public void setUp() {
    mockDelegate = Mockito.mock(BulkMutation.class);
    bulkWrapper = new BulkMutationWrapper(mockDelegate, requestContext);
  }

  @Test
  public void testFlush() throws InterruptedException {
    doNothing().when(mockDelegate).flush();
    bulkWrapper.flush();
    verify(mockDelegate).flush();
  }

  @Test
  public void testSendUnsend() {
    doNothing().when(mockDelegate).sendUnsent();
    bulkWrapper.sendUnsent();
    verify(mockDelegate).sendUnsent();
  }

  @Test
  public void testIsFlushed() {
    when(mockDelegate.isFlushed()).thenReturn(true);
    assertTrue(bulkWrapper.isFlushed());
    verify(mockDelegate).isFlushed();
  }

  @Test
  public void testAddMutate() {
    RowMutation rowMutation = RowMutation.create("tableId", "key");
    MutateRowsRequest.Entry requestProto = rowMutation.toBulkProto(requestContext).getEntries(0);
    when(mockDelegate.add(requestProto))
        .thenReturn(Futures.immediateFuture(MutateRowResponse.getDefaultInstance()));
    Future<Void> response = bulkWrapper.add(rowMutation);
    try {
      response.get();
    } catch (Exception ex) {
      throw new AssertionError("Assertion failed for BulkMutationWrapper#add(RowMutation)");
    }
    verify(mockDelegate).add(requestProto);
  }

  @Test
  public void testReadModifyRow() throws InterruptedException, ExecutionException {
    ReadModifyWriteRow readModifyRow = ReadModifyWriteRow.create("tableId", "test-key");
    ListenableFuture<Row> expectedResponse = Futures.immediateFuture(
        Row.create(ByteString.copyFromUtf8("test-key"), Collections.<RowCell>emptyList()));
    when(mockDelegate.readModifyWrite(readModifyRow)).thenReturn(expectedResponse);
    Future<Row> actualResponse = bulkWrapper.readModifyWrite(readModifyRow);
    assertEquals(expectedResponse.get(), actualResponse.get());
    verify(mockDelegate).readModifyWrite(readModifyRow);
  }
}