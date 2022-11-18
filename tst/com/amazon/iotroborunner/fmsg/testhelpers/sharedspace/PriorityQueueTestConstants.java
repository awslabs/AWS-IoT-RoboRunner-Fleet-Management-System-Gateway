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

package com.amazon.iotroborunner.fmsg.testhelpers.sharedspace;

import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.WORKER_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.WORKER_FLEET_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.DestinationTestConstants.SHARED_SPACE_DESTINATION_ARN;

import com.amazon.iotroborunner.fmsg.types.sharedspace.PriorityQueueRecord;

import java.time.Duration;
import java.time.Instant;

/**
 * Shared Space Test Constants.
 */
public final class PriorityQueueTestConstants {
    /**
     * Hidden Constructor.
     */
    private PriorityQueueTestConstants() {
        throw new UnsupportedOperationException("This class is for holding constants and should not be instantiated");
    }

    public static final PriorityQueueRecord PRIORITY_QUEUE_RECORD_WITH_LOCK = PriorityQueueRecord.builder()
        .workerArn(WORKER_ARN)
        .workerFleet(WORKER_FLEET_ARN)
        .sharedSpaceArn(SHARED_SPACE_DESTINATION_ARN)
        .priority("LOCK")
        .maxCrossingTime(Instant.now().toEpochMilli() + Duration.ofDays(1).toMillis())
        .build();

    public static final PriorityQueueRecord TIMED_OUT_PRIORITY_QUEUE_WITH_LOCK = PriorityQueueRecord.builder()
        .workerArn(WORKER_ARN)
        .workerFleet(WORKER_FLEET_ARN)
        .sharedSpaceArn(SHARED_SPACE_DESTINATION_ARN)
        .priority("LOCK")
        .maxCrossingTime(Instant.now().toEpochMilli() - Duration.ofDays(1).toMillis())
        .build();

    public static final PriorityQueueRecord PRIORITY_QUEUE_RECORD_WITHOUT_LOCK = PriorityQueueRecord.builder()
        .workerArn(WORKER_ARN)
        .workerFleet(WORKER_FLEET_ARN)
        .sharedSpaceArn(SHARED_SPACE_DESTINATION_ARN)
        .priority("123456")
        .build();

    public static final PriorityQueueRecord PRIORITY_QUEUE_RECORD_MISSING_ATTRIBUTE = PriorityQueueRecord.builder()
        .sharedSpaceArn(SHARED_SPACE_DESTINATION_ARN)
        .priority("1665079844")
        .build();
}
