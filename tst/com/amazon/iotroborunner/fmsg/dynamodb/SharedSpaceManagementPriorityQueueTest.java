/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.amazon.iotroborunner.fmsg.dynamodb;

import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.PriorityQueueTestConstants.PRIORITY_QUEUE_RECORD_MISSING_ATTRIBUTE;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.PriorityQueueTestConstants.PRIORITY_QUEUE_RECORD_WITHOUT_LOCK;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.PriorityQueueTestConstants.PRIORITY_QUEUE_RECORD_WITH_LOCK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.iotroborunner.fmsg.types.sharedspace.PriorityQueueRecord;

import java.util.List;
import java.util.Optional;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/** Unit tests for SharedSpaceManagementPriorityQueue module. */
public class SharedSpaceManagementPriorityQueueTest {
    private DynamoDBMapper mapper;
    private SharedSpaceManagementPriorityQueue classUnderTest;

    /** Set up the mocks needed for each subsequent test. */
    @BeforeEach
    public void setup() {
        this.mapper = mock(DynamoDBMapper.class);
        this.classUnderTest = new SharedSpaceManagementPriorityQueue("test", this.mapper);
    }

    @Test
    public void given_validRecord_when_requestLock_then_successfulSave() {
        final PriorityQueueRecord record = PRIORITY_QUEUE_RECORD_WITHOUT_LOCK;

        classUnderTest.addRecord(record);

        verify(mapper, times(1)).save(eq(record));
    }

    @Test
    public void given_recordMissingRequiredAttribute_when_requestLock_then_throwIllegalArgumentException() {
        final String expectedExceptionMessage = "All attributes must be added to priority queue record.";

        final Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            classUnderTest.addRecord(PRIORITY_QUEUE_RECORD_MISSING_ATTRIBUTE);
        });

        assertTrue(exception.getMessage().contains(expectedExceptionMessage));
    }

    @Test
    public void given_validRecord_when_releaseLock_then_successfulDelete() {
        final PriorityQueueRecord record = PRIORITY_QUEUE_RECORD_WITHOUT_LOCK;

        classUnderTest.deleteRecord(record);

        verify(mapper).delete(record);
    }

    @Test
    public void given_sharedSpaceArnWithWorkersInQueue_when_getNextWorkerInQueue_then_returnTheFirstWorker() {
        final PriorityQueueRecord record1 = PriorityQueueRecord.builder()
                .sharedSpaceArn("fakeSharedSpaceArn")
                .workerArn("fakeWorkerArn")
                .priority("1405079844")
                .build();
        final PriorityQueueRecord record2 = PriorityQueueRecord.builder()
                .sharedSpaceArn("fakeSharedSpaceArn")
                .workerArn("fakeWorkerArn")
                .priority("1405079894")
                .build();
        final PaginatedQueryList<PriorityQueueRecord> paginatedQueryList = mock(PaginatedQueryList.class);
        paginatedQueryList.add(record1);
        paginatedQueryList.add(record2);
        when(paginatedQueryList.get(0)).thenReturn(record1);
        when(mapper.query(eq(PriorityQueueRecord.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(paginatedQueryList);

        final Optional<PriorityQueueRecord> result = classUnderTest.getNextWorkerInQueue(record1.getSharedSpaceArn());

        assertTrue(result.isPresent());
        assertEquals(record1, result.get());
    }

    @Test
    public void given_sharedSpaceArnWithoutWorkersInQueue_when_getNextWorkerInQueue_then_emptyOptional() {
        when(mapper.query(eq(PriorityQueueRecord.class), any(DynamoDBQueryExpression.class))).thenReturn(null);

        final Optional<PriorityQueueRecord> result = classUnderTest.getNextWorkerInQueue("mySharedSpaceArn");

        assertTrue(result.isEmpty());
    }

    @Test
    public void given_sharedSpaceArn_when_getCurrentLockHolder_then_returnLockHolder() {
        final PriorityQueueRecord record = PRIORITY_QUEUE_RECORD_WITH_LOCK;
        final PaginatedQueryList<PriorityQueueRecord> paginatedQueryList = mock(PaginatedQueryList.class);
        when(paginatedQueryList.get(0)).thenReturn(record);
        when(mapper.query(eq(PriorityQueueRecord.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(paginatedQueryList);


        final Optional<PriorityQueueRecord> result = classUnderTest.getCurrentLockHolder(record.getSharedSpaceArn());

        assertTrue(result.isPresent());
        assertEquals(record, result.get());
    }

    @Test
    public void given_tableWithoutLocks_when_listLockHolders_then_returnEmptyList() {
        when(mapper.query(eq(PriorityQueueRecord.class), any(DynamoDBQueryExpression.class))).thenReturn(null);

        final List<PriorityQueueRecord> result = classUnderTest.listLockHolders();

        assertTrue(result.isEmpty());
    }

    @Test
    public void given_tableWithLock_when_listHolders_then_returnLockHolders() {
        final PriorityQueueRecord record = PRIORITY_QUEUE_RECORD_WITH_LOCK;
        final PaginatedQueryList<PriorityQueueRecord> paginatedQueryList = mock(PaginatedQueryList.class);
        paginatedQueryList.add(record);

        when(paginatedQueryList.toArray()).thenReturn(new Object[]{record});
        when(mapper.query(eq(PriorityQueueRecord.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(paginatedQueryList);

        final List<PriorityQueueRecord> result = classUnderTest.listLockHolders();

        assertEquals(1, result.size());
        assertEquals(record, result.get(0));
    }

}
