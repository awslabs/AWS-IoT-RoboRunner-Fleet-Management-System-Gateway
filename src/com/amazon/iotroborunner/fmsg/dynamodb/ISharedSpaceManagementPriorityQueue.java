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

import com.amazon.iotroborunner.fmsg.types.sharedspace.PriorityQueueRecord;

import java.util.List;
import java.util.Optional;

/**
 * A generic interface for the "storage" of priority queue entries which maintains
 * what workers have requested access to Shared Space and what workers were granted
 * locks to enter said Shared Space.
 */
public interface ISharedSpaceManagementPriorityQueue {
    /**
     * Adds a worker to the priority queue for a given Shared Space.
     *
     * @param record record which includes worker arn, shared space arn, etc
     */
    void addRecord(PriorityQueueRecord record);

    /**
     * Add a worker's request for a shared space if it isn't already there.
     *
     * @param record record which includes worker arn, shared space arn, etc
     */
    void addRecordRequestIfNotAlreadyPresent(PriorityQueueRecord record);

    /**
     * Removes a worker from the priority queue for the given Shared Space.
     *
     * @param record record which includes worker arn, Shared Space arn, etc
     */
    void deleteRecord(PriorityQueueRecord record);

    /**
     * Finds the next worker in the priority queue that should be granted access to the Shared Space.
     *
     * @param sharedSpaceArn ARN of the Shared Space that we're looking for the next worker grant access to
     * @return a priority queue entry that includes details about the worker that should be granted access
     */
    Optional<PriorityQueueRecord> getNextWorkerInQueue(String sharedSpaceArn);

    /**
     * Finds the worker who currently has a lock for the provided Shared Space.
     *
     * @param sharedSpaceArn ARN of the Shared Space we're looking for a lock holder of
     * @return a priority queue entry that includes details about the worker with the lock
     */
    Optional<PriorityQueueRecord> getCurrentLockHolder(String sharedSpaceArn);

    /**
     * List of all workers currently holding locks for a Shared Space.
     *
     * @return list of priority queue entries that include details about the workers with locks
     */
    List<PriorityQueueRecord> listLockHolders();

    /**
     * Performs transactional write on a worker from the priority queue for the given Shared Space.
     *
     * @param lockHoldingRecord updated lock record for worker to be given lock
     * @param workerRecord original record for worker to be given lock
     */
    void transactionWrite(PriorityQueueRecord lockHoldingRecord, PriorityQueueRecord workerRecord);
}
