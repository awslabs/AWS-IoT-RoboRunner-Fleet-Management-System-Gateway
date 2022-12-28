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
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.PRIORITY_QUEUE_TABLE_NAME;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.PriorityQueueUtils.createDynamoDbQuery;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.PriorityQueueUtils.doesTableExist;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.iotroborunner.fmsg.testhelpers.MockedAppender;
import com.amazon.iotroborunner.fmsg.types.sharedspace.PriorityQueueRecord;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.SSEDescription;
import com.amazonaws.services.dynamodbv2.model.SSEType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveDescription;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveStatus;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveRequest;
import com.amazonaws.services.dynamodbv2.waiters.AmazonDynamoDBWaiters;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.AliasListEntry;
import com.amazonaws.services.kms.model.ListAliasesResult;
import com.amazonaws.waiters.Waiter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for PriorityQueueUtils module.
 */
@Log4j2
@ExtendWith(MockitoExtension.class)
public class PriorityQueueUtilsTest {
    @Mock
    private AmazonDynamoDBWaiters waiters;
    @Mock
    private Waiter<DescribeTableRequest> waiter;
    @Mock
    private AmazonDynamoDB mockClient;
    @Mock
    private DynamoDBMapper mockMapper;
    @Mock
    private AWSKMS mockKmsClient;
    @Captor
    ArgumentCaptor<UpdateTimeToLiveRequest> updateRequestCaptor;

    private static Logger logger;
    private static MockedAppender mockedAppender;
    public static final List<KeySchemaElement> requiredKses = List.of(
        new KeySchemaElement("sharedSpaceArn", KeyType.HASH),
        new KeySchemaElement("priority", KeyType.RANGE));
    public static final List<AttributeDefinition> requiredAttributes = List.of(
        new AttributeDefinition("sharedSpaceArn", ScalarAttributeType.S),
        new AttributeDefinition("priority", ScalarAttributeType.S));

    /**
     * Set up the mocks needed for each subsequent test.
     */
    @BeforeAll
    public static void setUp() {
        mockedAppender = new MockedAppender();
        logger = (Logger) LogManager.getLogger(PriorityQueueUtils.class);
        logger.addAppender(mockedAppender);
        logger.setLevel(Level.DEBUG);
    }

    /**
     * Clean Up the used test resources.
     */
    @AfterAll
    public static void cleanUp() {
        if (mockedAppender != null) {
            logger.removeAppender(mockedAppender);
        }
    }

    @AfterEach
    public void clearLogHistory() {
        mockedAppender.clear();
    }

    @Test
    public void given_validMapAndCondition_when_createDynamoDbQuery_then_returnDynamoDbQuery() {
        final String condition = "priority = :val";
        final Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withS("testKey"));

        final DynamoDBQueryExpression<PriorityQueueRecord> result = createDynamoDbQuery(eav, condition);

        assertEquals(condition, result.getKeyConditionExpression());
        assertEquals(eav, result.getExpressionAttributeValues());
    }

    @Test
    public void given_validMapAndConditionAndFilter_when_createDynamoDbQuery_then_returnDynamoDbQuery() {
        final String condition = "priority = :val";
        final String filter = "workerArn = :val";
        final Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withS("testKey"));

        final DynamoDBQueryExpression<PriorityQueueRecord> result = createDynamoDbQuery(eav, condition, filter);

        assertEquals(condition, result.getKeyConditionExpression());
        assertEquals(eav, result.getExpressionAttributeValues());
        assertEquals(filter, result.getFilterExpression());
    }


    @Test
    public void given_accountWithNoTable_when_doesTableExist_then_returnNoTableDescription() {
        when(mockClient.describeTable(any(DescribeTableRequest.class)))
            .thenThrow(new ResourceNotFoundException("missing table"));

        final Optional<TableDescription> result = doesTableExist(mockClient, PRIORITY_QUEUE_TABLE_NAME);

        assertTrue(result.isEmpty());
    }

    @Test
    public void given_accountWithTable_when_doesTableExist_then_returnTableDescription() {
        final DescribeTableResult tableResult = new DescribeTableResult().withTable(new TableDescription());
        when(mockClient.describeTable(any(DescribeTableRequest.class))).thenReturn(tableResult);

        final Optional<TableDescription> result = doesTableExist(mockClient, PRIORITY_QUEUE_TABLE_NAME);

        assertTrue(result.isPresent());
    }

    @Test
    public void given_existingActiveTable_when_createPriorityQueueIfMissing_then_logSuccess() {
        final TableDescription tableDescription = new TableDescription()
            .withKeySchema(requiredKses)
            .withAttributeDefinitions(requiredAttributes)
            .withTableStatus(TableStatus.ACTIVE)
            .withSSEDescription(new SSEDescription().withSSEType(SSEType.KMS)
                .withKMSMasterKeyArn(CUSTOMER_MANAGED_CMK_ALIAS));
        final DescribeTableResult tableResult = new DescribeTableResult().withTable(tableDescription);
        when(mockClient.describeTable(any(DescribeTableRequest.class))).thenReturn(tableResult);

        PriorityQueueUtils.createPriorityQueueIfMissing(mockClient, mockMapper, mockKmsClient);

        mockedAppender.assertLogContainsMessage(String.format("%s table passed validation", PRIORITY_QUEUE_TABLE_NAME));
    }

    @Test
    public void given_existingActiveTableWithIncorrectAttribute_when_createPriorityQueueIfMissing_then_logError() {
        final TableDescription tableDescription = new TableDescription()
            .withKeySchema(requiredKses)
            .withAttributeDefinitions(List.of(
                new AttributeDefinition("sharedSpace", ScalarAttributeType.S),
                new AttributeDefinition("priority", ScalarAttributeType.S)))
            .withTableStatus(TableStatus.ACTIVE)
            .withSSEDescription(new SSEDescription().withSSEType(SSEType.KMS)
                .withKMSMasterKeyArn(CUSTOMER_MANAGED_CMK_ALIAS));
        final DescribeTableResult tableResult = new DescribeTableResult().withTable(tableDescription);
        when(mockClient.describeTable(any(DescribeTableRequest.class))).thenReturn(tableResult);

        PriorityQueueUtils.createPriorityQueueIfMissing(mockClient, mockMapper, mockKmsClient);

        mockedAppender.assertLogContainsMessage(String.format("The %s table is incorrectly configured. Please "
            + "delete and start again", PRIORITY_QUEUE_TABLE_NAME));
    }

    @Test
    public void given_existingTableMissingAttributes_when_createPriorityQueueIfMissing_then_logError() {
        final List<AttributeDefinition> missingAttribute = new ArrayList<>(requiredAttributes);
        missingAttribute.remove(0);
        final TableDescription tableDescription = new TableDescription()
            .withKeySchema(requiredKses)
            .withAttributeDefinitions(missingAttribute)
            .withTableStatus(TableStatus.ACTIVE)
            .withSSEDescription(new SSEDescription().withSSEType(SSEType.KMS)
                .withKMSMasterKeyArn(CUSTOMER_MANAGED_CMK_ALIAS));
        final DescribeTableResult tableResult = new DescribeTableResult().withTable(tableDescription);
        when(mockClient.describeTable(any(DescribeTableRequest.class))).thenReturn(tableResult);

        PriorityQueueUtils.createPriorityQueueIfMissing(mockClient, mockMapper, mockKmsClient);

        mockedAppender.assertLogStartsWith("The number of attribute definitions does not match the Shared Space "
            + "Management requirement.");
    }

    @Test
    public void given_existingTableWithoutSortKey_when_createPriorityQueueIfMissing_then_logError() {
        final List<KeySchemaElement> missingKse = new ArrayList<>(requiredKses);
        missingKse.remove(0);
        final TableDescription tableDescription = new TableDescription()
            .withKeySchema(missingKse)
            .withAttributeDefinitions(requiredAttributes)
            .withTableStatus(TableStatus.ACTIVE)
            .withSSEDescription(new SSEDescription().withSSEType(SSEType.KMS)
                .withKMSMasterKeyArn(CUSTOMER_MANAGED_CMK_ALIAS));
        final DescribeTableResult tableResult = new DescribeTableResult().withTable(tableDescription);
        when(mockClient.describeTable(any(DescribeTableRequest.class))).thenReturn(tableResult);

        PriorityQueueUtils.createPriorityQueueIfMissing(mockClient, mockMapper, mockKmsClient);

        mockedAppender.assertLogStartsWith("The number of key schema elements does not match the existing table.");
    }

    @Test
    public void given_existingTableWithIncorrectSseKey_when_createPriorityQueueIfMissing_then_logWarning() {
        final TableDescription tableDescription = new TableDescription()
            .withKeySchema(requiredKses)
            .withAttributeDefinitions(requiredAttributes)
            .withTableStatus(TableStatus.ACTIVE)
            .withSSEDescription(new SSEDescription().withSSEType(SSEType.AES256)
                .withKMSMasterKeyArn(CUSTOMER_MANAGED_CMK_ALIAS));
        final DescribeTableResult tableResult = new DescribeTableResult().withTable(tableDescription);
        when(mockClient.describeTable(any(DescribeTableRequest.class))).thenReturn(tableResult);

        PriorityQueueUtils.createPriorityQueueIfMissing(mockClient, mockMapper, mockKmsClient);

        mockedAppender.assertLogStartsWith("The encryption key does not match Shared Space Management guidelines.");
    }

    @Test
    public void given_existingTableWithoutSseKey_when_createPriorityQueueIfMissing_then_logWarning() {
        final TableDescription tableDescription = new TableDescription()
            .withKeySchema(requiredKses)
            .withAttributeDefinitions(requiredAttributes)
            .withTableStatus(TableStatus.ACTIVE);
        final DescribeTableResult tableResult = new DescribeTableResult().withTable(tableDescription);
        when(mockClient.describeTable(any(DescribeTableRequest.class))).thenReturn(tableResult);

        PriorityQueueUtils.createPriorityQueueIfMissing(mockClient, mockMapper, mockKmsClient);

        mockedAppender.assertLogStartsWith("The encryption key does not match Shared Space Management guidelines.");
    }

    @Test
    public void given_existingTableWithoutKmsAlias_when_createPriorityQueueIfMissing_then_logError() {
        final TableDescription tableDescription = new TableDescription()
            .withKeySchema(requiredKses)
            .withAttributeDefinitions(requiredAttributes)
            .withTableStatus(TableStatus.ACTIVE)
            .withSSEDescription(new SSEDescription().withSSEType(SSEType.KMS));
        final DescribeTableResult tableResult = new DescribeTableResult().withTable(tableDescription);
        when(mockClient.describeTable(any(DescribeTableRequest.class))).thenReturn(tableResult);

        PriorityQueueUtils.createPriorityQueueIfMissing(mockClient, mockMapper, mockKmsClient);

        mockedAppender.assertLogStartsWith("The encryption key does not match Shared Space Management guidelines.");
    }

    @Test
    public void given_accountWithNoTable_when_createPriorityQueueIfMissing_then_createNewTable() {
        final CreateTableRequest createTableRequest = new CreateTableRequest()
            .withKeySchema(requiredKses)
            .withAttributeDefinitions(requiredAttributes)
            .withBillingMode(BillingMode.PAY_PER_REQUEST);
        final DescribeTimeToLiveResult disabledDescribeResult = new DescribeTimeToLiveResult()
            .withTimeToLiveDescription(new TimeToLiveDescription().withTimeToLiveStatus(TimeToLiveStatus.DISABLED));
        final AliasListEntry aliasEntry = new AliasListEntry().withAliasName(CUSTOMER_MANAGED_CMK_ALIAS);
        final List<AliasListEntry> entries = List.of(aliasEntry);
        final ListAliasesResult mockRes = mock(ListAliasesResult.class);
        when(mockClient.describeTimeToLive(any(DescribeTimeToLiveRequest.class))).thenReturn(disabledDescribeResult);
        when(mockMapper.generateCreateTableRequest(PriorityQueueRecord.class)).thenReturn(createTableRequest);
        when(mockClient.waiters()).thenReturn(waiters);
        when(waiters.tableExists()).thenReturn(waiter);
        when(mockClient.describeTable(any(DescribeTableRequest.class)))
            .thenThrow(new ResourceNotFoundException("missing table"));
        when(mockKmsClient.listAliases()).thenReturn(mockRes);
        when(mockRes.getAliases()).thenReturn(entries);

        PriorityQueueUtils.createPriorityQueueIfMissing(mockClient, mockMapper, mockKmsClient);

        mockedAppender.assertLogContainsMessage(String.format("Created %s table", PRIORITY_QUEUE_TABLE_NAME));
        verify(mockClient).createTable(any(CreateTableRequest.class));
        verify(mockClient).updateTimeToLive(updateRequestCaptor.capture());
    }

    @Test
    public void given_noTableAndCmkMissing_when_createPriorityQueueIfMissing_then_throwException() {
        final AliasListEntry aliasEntry = new AliasListEntry().withAliasName("aliasNoHereTryAgain");
        final List<AliasListEntry> entries = List.of(aliasEntry);
        final ListAliasesResult mockRes = mock(ListAliasesResult.class);
        when(mockClient.describeTable(any(DescribeTableRequest.class)))
            .thenThrow(new ResourceNotFoundException("missing table"));
        when(mockKmsClient.listAliases()).thenReturn(mockRes);
        when(mockRes.getAliases()).thenReturn(entries);
            
        assertThrows(IllegalStateException.class, () -> {
            PriorityQueueUtils.createPriorityQueueIfMissing(mockClient, mockMapper, mockKmsClient);
        });
    }
}
