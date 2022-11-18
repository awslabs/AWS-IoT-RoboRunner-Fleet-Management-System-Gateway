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

package com.amazon.iotroborunner.fmsg.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the SecretsManager utilities module. */
@ExtendWith(MockitoExtension.class)
public class SecretsManagerUtilsTest {
    @Mock
    private AWSSecretsManager awsSecretsManagerMock;

    @Test
    void given_secretsManagerClientAndSecretName_when_getSecret_then_returnsSecretString() {
        final String expectedSecretString = "testSecretString";
        final GetSecretValueResult result = new GetSecretValueResult().withSecretString(expectedSecretString);
        when(awsSecretsManagerMock.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(result);

        final String resultSecretString = SecretsManagerUtils.getSecret(awsSecretsManagerMock, "secretName");

        assertEquals(expectedSecretString, resultSecretString);
    }

    @Test
    void given_secretsManagerClientAndSecretName_when_getSecret_then_throwsRuntimeExceptionDueToNoAbilityToRetrieveSecret() {
        final String exceptionMsg = "Secret was not found";
        // Throwing any exception that can be caught in the code
        when(awsSecretsManagerMock.getSecretValue(any(GetSecretValueRequest.class)))
                .thenThrow(new ResourceNotFoundException(exceptionMsg));

        final RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            SecretsManagerUtils.getSecret(awsSecretsManagerMock, exceptionMsg);
        });

        assertTrue(ex.getMessage().contains(String.format("Exception when getting secret value: %s", exceptionMsg)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void given_secretsManagerClientAndSecretName_when_getSecret_then_throwsRuntimeExceptionDueToEmptySecret(final String secretString) {
        final String secretName = "secretName";
        final GetSecretValueResult result = new GetSecretValueResult().withSecretString(secretString);
        when(awsSecretsManagerMock.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(result);

        final RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            SecretsManagerUtils.getSecret(awsSecretsManagerMock, secretName);
        });

        assertEquals(String.format("Secret %s is empty!", secretName), ex.getMessage());
    }

    @Test
    void given_region_when_getSecretsManager_then_returnsSecretsManager() {
        final AWSSecretsManager manager = SecretsManagerUtils.getSecretsManager("us-east-1");

        assertNotNull(manager);
    }

    @Test
    void given_secretNameAndRegion_when_getSecret_then_returnsSecretString() {
        final String secretName = "secretName";
        final String expectedSecretString = "testSecretString";

        try (MockedStatic<SecretsManagerUtils> mockedStatic = mockStatic(
                SecretsManagerUtils.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> SecretsManagerUtils.getSecretsManager(anyString()))
                    .thenReturn(awsSecretsManagerMock);
            mockedStatic.when(() -> SecretsManagerUtils.getSecret(awsSecretsManagerMock,  secretName))
                    .thenReturn(expectedSecretString);

            // Testing
            final String actualSecretValue = SecretsManagerUtils.getSecret(secretName, "us-east-1");

            // Verification
            assertEquals(expectedSecretString, actualSecretValue);
        }
    }
}
