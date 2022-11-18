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

package com.amazon.iotroborunner.fmsg.types.sharedspace;

import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.MAX_CROSSING_TIME_ATTRIBUTE_NAME;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.PRIORITY_ATTRIBUTE_NAME;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.SHARED_SPACE_ATTRIBUTE_NAME;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.TIME_TO_LIVE_ATTRIBUTE_NAME;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.WORKER_ATTRIBUTE_NAME;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.WORKER_FLEET_ATTRIBUTE_NAME;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * The POJO maps to the priority queue record which is used to add and remove
 * workers from the Shared Space Management Priority Queue. These records are
 * required to determine who should have access to a shared space at a given time.
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "SharedSpaceManagementPriorityQueue")
public class PriorityQueueRecord {
    /**
     * ARN of the shared space that's being requested/released.
     * This is the partition key.
     */
    @DynamoDBHashKey(attributeName = SHARED_SPACE_ATTRIBUTE_NAME)
    private String sharedSpaceArn;

    /**
     * ARN of the worker requesting/releasing a lock.
     */
    @DynamoDBAttribute(attributeName = WORKER_ATTRIBUTE_NAME)
    private String workerArn;

    /**
     * The fleet the worker is assigned to.
     */
    @DynamoDBAttribute(attributeName = WORKER_FLEET_ATTRIBUTE_NAME)
    private String workerFleet;

    /**
     * Priority of the worker's entry. (e.g. worker's arrival time.)
     * This is the sort key.
     */
    @DynamoDBRangeKey(attributeName = PRIORITY_ATTRIBUTE_NAME)
    private String priority;

    /**
     * The time in which the worker should have crossed the shared space
     * without triggering a timeout.
     */
    @DynamoDBAttribute(attributeName = MAX_CROSSING_TIME_ATTRIBUTE_NAME)
    private Long maxCrossingTime;

    /**
     * The time in which the record so be evicted from the table.
     */
    @DynamoDBAttribute(attributeName = TIME_TO_LIVE_ATTRIBUTE_NAME)
    private Long ttl;
}
