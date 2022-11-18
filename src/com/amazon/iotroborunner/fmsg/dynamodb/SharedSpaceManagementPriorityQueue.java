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

import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.LOCK_SORT_KEY;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.PARTITION_KEY_VALUE;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.PRIORITY_QUEUE_TABLE_NAME;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.SORT_KEY_VALUE;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.WORKER_KEY_VALUE;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.PriorityQueueUtils.createDynamoDbQuery;

import com.amazon.iotroborunner.fmsg.types.sharedspace.PriorityQueueRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.TransactionWriteRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * This is a dynamo backed priority queue that allows us to add and remove workers thus helping us determine:
 * 1. What workers are waiting for access to a shared space (requestedLock)?
 * 2. What worker currently has the right away in a shared space (acquiredLock)?
 * 3. Who is the next worker to receive access to the shared space?
 */
@Log4j2
public class SharedSpaceManagementPriorityQueue implements ISharedSpaceManagementPriorityQueue {
    private final String tableName;
    private final DynamoDBMapper dynamoDbMapper;

    /**
     * Default Constructor.
     */
    public SharedSpaceManagementPriorityQueue() {
        this.tableName = PRIORITY_QUEUE_TABLE_NAME;
        this.dynamoDbMapper = new DynamoDBMapper(AmazonDynamoDBClientBuilder.defaultClient());
    }

    /**
     * Constructor.
     *
     * @param tableName Desired table name.
     * @param mapper Dynamo DB Mapper.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP")
    public SharedSpaceManagementPriorityQueue(@NonNull final String tableName, @NonNull final DynamoDBMapper mapper) {
        this.tableName = tableName;
        this.dynamoDbMapper = mapper;
    }

    @Override
    public void addRecord(@NonNull final PriorityQueueRecord record) {
        if (record.getWorkerArn() == null || record.getSharedSpaceArn() == null || record.getPriority() == null) {
            log.error("Unable to add record to priority queue due to missing attributes. Record: {}", record);
            throw new IllegalArgumentException("All attributes must be added to priority queue record.");
        }

        log.info("Adding record to priority queue for worker: {}", record.getWorkerArn());
        dynamoDbMapper.save(record);
        log.info("Record added to the queue");
    }

    @Override
    public void addRecordRequestIfNotAlreadyPresent(@NonNull final PriorityQueueRecord record) {
        log.info("Checking if worker: {} already has been added to the queue", record.getWorkerArn());
        final Map<String, AttributeValue> expressionAttributesToValues = new HashMap<>();
        expressionAttributesToValues.put(PARTITION_KEY_VALUE, new AttributeValue().withS(record.getSharedSpaceArn()));
        expressionAttributesToValues.put(WORKER_KEY_VALUE, new AttributeValue().withS(record.getWorkerArn()));

        final String keyCondition = String.format("sharedSpaceArn = %s",
            PARTITION_KEY_VALUE);

        final String filterExpression = String.format("workerArn = %s",
            WORKER_KEY_VALUE);

        final List<PriorityQueueRecord> matchingRecords = dynamoDbMapper.query(
            PriorityQueueRecord.class,
            createDynamoDbQuery(expressionAttributesToValues, keyCondition, filterExpression));

        if (matchingRecords == null || matchingRecords.isEmpty()) {
            log.info("There are no current records found for this worker: {}", record.getWorkerArn());
            addRecord(record);
            return;
        }
        log.info("A record was already found for this worker. No addition will be made.");
    }

    @Override
    public void deleteRecord(@NonNull final PriorityQueueRecord record) {
        dynamoDbMapper.delete(record);

        log.info("Successfully deleted worker: {} request shared space: {} from the DynamoDB table: {}",
                record.getWorkerArn(),
                record.getSharedSpaceArn(),
                tableName);
    }

    @Override
    public Optional<PriorityQueueRecord> getNextWorkerInQueue(@NonNull final String sharedSpaceArn) {
        log.info("Fetching the next worker who should cross sharedSpaceArn: {}", sharedSpaceArn);

        final Map<String, AttributeValue> expressionAttributesToValues = new HashMap<>();
        expressionAttributesToValues.put(PARTITION_KEY_VALUE, new AttributeValue().withS(sharedSpaceArn));

        final String condition = String.format("sharedSpaceArn = %s", PARTITION_KEY_VALUE);

        final List<PriorityQueueRecord> queuedWorkers = dynamoDbMapper.query(
                PriorityQueueRecord.class,
                createDynamoDbQuery(expressionAttributesToValues, condition));

        if (queuedWorkers == null || queuedWorkers.isEmpty()) {
            log.info("There is no worker who should cross sharedSpaceArn: {} next", sharedSpaceArn);
            return Optional.empty();
        }

        final PriorityQueueRecord firstRecord = queuedWorkers.get(0);
        log.debug("The next worker for sharedSpaceArn: {} is workerArn: {}",
                sharedSpaceArn,
                firstRecord.getWorkerArn());
        return Optional.of(firstRecord);
    }

    @Override
    public Optional<PriorityQueueRecord> getCurrentLockHolder(@NonNull final String sharedSpaceArn) {
        log.info("Fetching current lock holder for sharedSpaceArn: {}", sharedSpaceArn);
        final Map<String, AttributeValue> expressionAttributesToValues = new HashMap<>();
        expressionAttributesToValues.put(PARTITION_KEY_VALUE, new AttributeValue().withS(sharedSpaceArn));
        expressionAttributesToValues.put(SORT_KEY_VALUE, new AttributeValue().withS(LOCK_SORT_KEY));

        final String condition = String.format("sharedSpaceArn = %s and priority = %s",
            PARTITION_KEY_VALUE,
                SORT_KEY_VALUE);

        final List<PriorityQueueRecord> lockHolder = dynamoDbMapper.query(
                PriorityQueueRecord.class,
                createDynamoDbQuery(expressionAttributesToValues, condition));

        if (lockHolder == null || lockHolder.isEmpty()) {
            log.info("No lock found for sharedSpaceArn: {}", sharedSpaceArn);
            return Optional.empty();
        }

        final PriorityQueueRecord firstRecord = lockHolder.get(0);
        log.info("Worker: {} is holding a lock for the sharedSpaceArn: {}",
                firstRecord.getWorkerArn(),
                firstRecord.getSharedSpaceArn());
        log.debug("Record:{}", firstRecord);

        return Optional.of(firstRecord);
    }

    @Override
    public List<PriorityQueueRecord> listLockHolders() {
        final Map<String, AttributeValue> expressionAttributesToValues = new HashMap<>();
        expressionAttributesToValues.put(SORT_KEY_VALUE, new AttributeValue().withS(LOCK_SORT_KEY));

        final String condition = String.format("priority = %s", SORT_KEY_VALUE);

        final List<PriorityQueueRecord> lockHolders = dynamoDbMapper.query(
                PriorityQueueRecord.class,
                createDynamoDbQuery(expressionAttributesToValues, condition));

        if (lockHolders == null || lockHolders.isEmpty()) {
            log.info("There are no workers holding a lock for any shared spaces.");
            return Collections.emptyList();
        }
        return new ArrayList<>(lockHolders);
    }

    @Override
    public void transactionWrite(@NonNull final PriorityQueueRecord lockHoldingRecord,
            @NonNull final PriorityQueueRecord workerRecord) {
        if (lockHoldingRecord.getWorkerArn() == null || lockHoldingRecord.getSharedSpaceArn() == null
                || lockHoldingRecord.getPriority() == null) {
            log.error(
                "Unable to complete record transaction to priority queue due to missing attributes."
                + "lockHoldingRecord: {}",
                lockHoldingRecord
            );
            throw new IllegalArgumentException("All attributes must be added to priority queue lockHoldingRecord.");
        }

        final TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addDelete(workerRecord);
        transactionWriteRequest.addPut(lockHoldingRecord);

        // Run the transaction and process the result.
        try {
            dynamoDbMapper.transactionWrite(transactionWriteRequest);
            log.info("Successfully updated worker: {} with lock.",
                lockHoldingRecord.getWorkerArn());

        } catch (final Exception ex) {
            log.error("One of the table involved in the transaction is not found" + ex.getMessage());
        }
    }    

}
