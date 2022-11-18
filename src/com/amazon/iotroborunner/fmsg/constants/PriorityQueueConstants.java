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

package com.amazon.iotroborunner.fmsg.constants;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

/**
 * Constants to be used for the Priority Queue.
 */
public final class PriorityQueueConstants {
    /**
     * Name of the DynamoDB table used to hold the Shared Space Priority Queue.
     */
    public static final String PRIORITY_QUEUE_TABLE_NAME = "SharedSpaceManagementPriorityQueue";

    /**
     * LOCK Sort key.
     */
    public static final String LOCK_SORT_KEY = "LOCK";

    /**
     * LOCK Priority.
     */
    public static final String LOCK_PRIORITY = LOCK_SORT_KEY;

    /**
     * Sort Key Attribute.
     */
    public static final String SORT_KEY_VALUE = ":sortKey";

    /**
     * Partition Key Attribute.
     */
    public static final String PARTITION_KEY_VALUE = ":partitionKey";

    /**
     * Worker Attribute.
     */
    public static final String WORKER_KEY_VALUE = ":workerArn";

    /**
     * Attribute Name for a Shared Space.
     */
    public static final String SHARED_SPACE_ATTRIBUTE_NAME = "sharedSpaceArn";

    /**
     * Attribute Name for a Worker.
     */
    public static final String WORKER_ATTRIBUTE_NAME = "workerArn";

    /**
     * Attribute Name for a Worker Fleet.
     */
    public static final String WORKER_FLEET_ATTRIBUTE_NAME = "workerFleetArn";

    /**
     * Attribute Name for the Priority.
     */
    public static final String PRIORITY_ATTRIBUTE_NAME = "priority";

    /**
     * Attribute Name for the Maximum Crossing Time.
     */
    public static final String MAX_CROSSING_TIME_ATTRIBUTE_NAME = "maxCrossingTime";

    /**
     * Attribute Name for the Time to Live.
     */
    public static final String TIME_TO_LIVE_ATTRIBUTE_NAME = "timeToLive";

    /**
     * Name of the Priority Queue Schema Keys.
     */
    public static final List<String> PRIORITY_QUEUE_KEY_SCHEMA_NAMES = List.of(SHARED_SPACE_ATTRIBUTE_NAME,
        PRIORITY_ATTRIBUTE_NAME);

    /**
     * Priority Queue attribute name to attribute type map.
     */
    public static final Map<String, String> PRIORITY_QUEUE_ATTRIBUTE_NAME_TO_TYPE = Map.of(
        SHARED_SPACE_ATTRIBUTE_NAME, ScalarAttributeType.S.toString(),
        PRIORITY_ATTRIBUTE_NAME, ScalarAttributeType.S.toString());

    /**
     * Alias for the SM Customer Managed CMK.
     */
    public static final String CUSTOMER_MANAGED_CMK_ALIAS = "alias/sm-priority-queue-cmk";

    /**
     * Hidden Constructor.
     */
    private PriorityQueueConstants() {
        throw new UnsupportedOperationException("This class is for holding constants and should not be instantiated.");
    }
}
