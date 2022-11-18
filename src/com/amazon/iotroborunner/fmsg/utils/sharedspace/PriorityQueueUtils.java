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

package com.amazon.iotroborunner.fmsg.utils.sharedspace;

import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.CUSTOMER_MANAGED_CMK_ALIAS;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.PRIORITY_QUEUE_ATTRIBUTE_NAME_TO_TYPE;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.PRIORITY_QUEUE_KEY_SCHEMA_NAMES;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.PRIORITY_QUEUE_TABLE_NAME;

import com.amazon.iotroborunner.fmsg.types.sharedspace.PriorityQueueRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.SSESpecification;
import com.amazonaws.services.dynamodbv2.model.SSEType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveDescription;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveSpecification;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveStatus;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveRequest;
import com.amazonaws.waiters.WaiterParameters;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Utility functions that manage the Shared Space Management Priority Queue in DynamoDB.
 */
@Log4j2
public final class PriorityQueueUtils {

    /**
     * Number of required Attribute definitions.
     */
    private static final int NUMBER_OF_REQUIRED_ATTRIBUTES = PRIORITY_QUEUE_ATTRIBUTE_NAME_TO_TYPE.size();

    /**
     * Number of required Attribute definitions.
     */
    private static final int NUMBER_OF_REQUIRED_SCHEMAS = PRIORITY_QUEUE_KEY_SCHEMA_NAMES.size();

    /**
     * Hidden Constructor.
     */
    private PriorityQueueUtils() {
        throw new UnsupportedOperationException("This class is for holding utilities and should not be instantiated.");
    }

    /**
     * Creates a DynamoDB Query for Priority Queue Records.
     *
     * @param attributeValueMap Attribute Value Map which holds the partition key and sort key
     * @param keyCondition      Condition to evaluate
     * @return DynamoDB Query Expression
     */
    public static DynamoDBQueryExpression<PriorityQueueRecord> createDynamoDbQuery(
        @NonNull final Map<String, AttributeValue> attributeValueMap,
        @NonNull final String keyCondition) {

        return new DynamoDBQueryExpression<PriorityQueueRecord>()
            .withKeyConditionExpression(keyCondition)
            .withExpressionAttributeValues(attributeValueMap);
    }

    /**
     * Creates a DynamoDB Query for Priority Queue Records with filter expression.
     *
     * @param attributeValueMap attribute value map which holds the partition key and sort key
     * @param keyCondition      key condition to query results for
     * @param filterExpression  filter expression to filter results by
     * @return DynamoDB query Expression
     */
    public static DynamoDBQueryExpression<PriorityQueueRecord> createDynamoDbQuery(
        @NonNull final Map<String, AttributeValue> attributeValueMap,
        @NonNull final String keyCondition,
        @NonNull final String filterExpression) {

        return new DynamoDBQueryExpression<PriorityQueueRecord>()
            .withKeyConditionExpression(keyCondition)
            .withExpressionAttributeValues(attributeValueMap)
            .withFilterExpression(filterExpression);
    }

    /**
     * Create a Shared Space Management Priority Queue if the table doesn't already exist within the account.
     *
     * @param client Amazon DynamoDB client
     * @param mapper DynamoDB mapper
     */
    public static void createPriorityQueueIfMissing(@NonNull final AmazonDynamoDB client,
                                                    @NonNull final DynamoDBMapper mapper) {
        doesTableExist(client, PRIORITY_QUEUE_TABLE_NAME).ifPresentOrElse(
            tableDescription -> {
                log.info("Found {} table. Verifying the table is constructed correctly", PRIORITY_QUEUE_TABLE_NAME);
                if (hasCorrectTableSchema(tableDescription)
                    && tableDescription.getTableStatus().equals(TableStatus.ACTIVE.toString())) {
                    log.info("{} table passed validation", PRIORITY_QUEUE_TABLE_NAME);
                    return;
                }
                log.info("The {} table is incorrectly configured. Please delete and start again",
                    PRIORITY_QUEUE_TABLE_NAME);
            },
            () -> {
                log.info("Creating the {} table", PRIORITY_QUEUE_TABLE_NAME);
                createPriorityQueue(client, mapper);
            }
        );
    }

    /**
     * Determines if the provided table name exists in the account.
     *
     * @param client    Amazon DynamoDB Client
     * @param tableName the Amazon DynamoDB table to check for
     * @return optional TableDescription if the table exists
     */
    public static Optional<TableDescription> doesTableExist(@NonNull final AmazonDynamoDB client,
                                                            @NonNull final String tableName) {
        final DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
        try {
            final DescribeTableResult describeTableResult = client.describeTable(describeTableRequest);
            final TableDescription tableDescription = describeTableResult.getTable();
            return Optional.of(tableDescription);
        } catch (final ResourceNotFoundException e) {
            log.info("Table {} does not exist", tableName);
            return Optional.empty();
        }
    }

    /**
     * Determines if the provided attribute definitions meet the Priority Queue requirements.
     *
     * @param definitions attribute definitions to verify
     * @return true if definitions are valid else false
     */
    private static boolean hasValidAttributeDefinitions(@NonNull final List<AttributeDefinition> definitions) {
        boolean hasCorrectDefinitions = true;
        for (final AttributeDefinition definition : definitions) {
            final String attributeName = definition.getAttributeName();
            final String attributeType = definition.getAttributeType();

            if (!PRIORITY_QUEUE_ATTRIBUTE_NAME_TO_TYPE.containsKey(attributeName)
                || !PRIORITY_QUEUE_ATTRIBUTE_NAME_TO_TYPE.get(attributeName).equals(attributeType)) {
                log.info("Invalid attribute. Either the name or schema is incorrect: {}", definition.toString());
                hasCorrectDefinitions = false;
            }
        }
        return hasCorrectDefinitions;
    }

    /**
     * Determines if the provided KeySchemaElements are correct.
     *
     * @param keySchemaElements elements to verify
     * @return true if elements are correct else false
     */
    private static boolean hasCorrectKeySchemas(@NonNull final List<KeySchemaElement> keySchemaElements) {
        boolean hasCorrectKeySchemas = true;
        for (final KeySchemaElement keySchemaElement : keySchemaElements) {
            if (!PRIORITY_QUEUE_KEY_SCHEMA_NAMES.contains(keySchemaElement.getAttributeName())) {
                log.info("Invalid key schema element. KeySchemaElement: {}", keySchemaElement);
                hasCorrectKeySchemas = false;
            }
        }
        return hasCorrectKeySchemas;
    }

    /**
     * Builds a Shared Space Management Priority Queue matching required specifications.
     *
     * @param client Amazon DynamoDB client
     * @param mapper DynamoDB mapper
     */
    private static void createPriorityQueue(@NonNull final AmazonDynamoDB client,
                                            @NonNull final DynamoDBMapper mapper) {
        final CreateTableRequest createTableRequest = mapper.generateCreateTableRequest(PriorityQueueRecord.class);
        createTableRequest.setBillingMode(BillingMode.PAY_PER_REQUEST.toString());
        createTableRequest.withSSESpecification(
            new SSESpecification()
                .withEnabled(true)
                .withSSEType(SSEType.KMS)
                .withKMSMasterKeyId(CUSTOMER_MANAGED_CMK_ALIAS));
        try {
            log.debug("CreateTableRequest: {}", createTableRequest);
            client.createTable(createTableRequest);
        } catch (final ResourceInUseException e) {
            throw new IllegalStateException("There may already be a in progress table creation", e);
        }
        log.info("Created {} table", PRIORITY_QUEUE_TABLE_NAME);
        log.info("Waiting for {} to turn ACTIVE", PRIORITY_QUEUE_TABLE_NAME);
        client.waiters().tableExists()
            .run(new WaiterParameters<>(new DescribeTableRequest(createTableRequest.getTableName())));
        log.info("{} table is now ACTIVE", PRIORITY_QUEUE_TABLE_NAME);

        enableTimeToLive(client, PRIORITY_QUEUE_TABLE_NAME, "timeToLive");
    }

    /**
     * Determines if the provided table has the correct schema.
     *
     * @param tableDescription description of the table to verify
     * @return true if the schema is correct else false
     */
    private static boolean hasCorrectTableSchema(@NonNull final TableDescription tableDescription) {
        if (tableDescription.getAttributeDefinitions().size() < NUMBER_OF_REQUIRED_ATTRIBUTES) {
            log.error("The number of attribute definitions does not match the Shared Space Management requirement."
                    + " The following attributes must be present: {}. Found attributes: {}",
                PRIORITY_QUEUE_ATTRIBUTE_NAME_TO_TYPE.keySet(),
                tableDescription.getAttributeDefinitions());
            return false;
        }

        if (!hasValidAttributeDefinitions(tableDescription.getAttributeDefinitions())) {
            return false;
        }

        final List<KeySchemaElement> keySchemaElements = tableDescription.getKeySchema();
        if (keySchemaElements.size() != NUMBER_OF_REQUIRED_SCHEMAS) {
            log.error("The number of key schema elements does not match the existing table. Expected: {}, Actual: {}",
                NUMBER_OF_REQUIRED_SCHEMAS, keySchemaElements.size());
            return false;
        }

        if (!hasCorrectKeySchemas(keySchemaElements)) {
            return false;
        }

        if (tableDescription.getSSEDescription() == null
            || !SSEType.KMS.toString().equals(tableDescription.getSSEDescription().getSSEType())
            || tableDescription.getSSEDescription().getKMSMasterKeyArn() == null) {
            log.warn("The encryption key does not match Shared Space Management guidelines. SSE Encryption: {}",
                tableDescription.getSSEDescription());
        }
        return true;
    }

    /**
     * Enables Time to Live for the requested table and attribute.
     *
     * @param dynamoDbClient Amazon DynamoDB client
     * @param tableName      table to add the Time to Live to
     * @param ttlAttribute   the attribute that tracks Time to live
     */
    private static void enableTimeToLive(@NonNull final AmazonDynamoDB dynamoDbClient,
                                         @NonNull final String tableName,
                                         @NonNull final String ttlAttribute) {
        final TimeToLiveDescription ttlDescription = dynamoDbClient
            .describeTimeToLive(new DescribeTimeToLiveRequest().withTableName(tableName))
            .getTimeToLiveDescription();
        switch (TimeToLiveStatus.fromValue(ttlDescription.getTimeToLiveStatus())) {
            case DISABLED:
            case DISABLING:
                dynamoDbClient.updateTimeToLive(new UpdateTimeToLiveRequest().withTableName(tableName)
                    .withTimeToLiveSpecification(new TimeToLiveSpecification()
                        .withAttributeName(ttlAttribute)
                        .withEnabled(Boolean.TRUE)));
                break;
            case ENABLED:
            case ENABLING:
            default:
                break;
        }
    }
}
