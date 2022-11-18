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

import com.amazon.iotroborunner.fmsg.clients.SecretsManagerClientProvider;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

/**
 * Class with utility functions for interacting with SecretsManager.
 */
public final class SecretsManagerUtils {
    /**
     * Utility class, don't allow instantiation.
     */
    private SecretsManagerUtils() {}

    /**
     * Retrieves secret value given the secret name and secrets manager client.
     *
     * @param secretsManagerClient the client used to acquire the secret value
     * @param secretName the name of the stored secret.
     * @return the value of the stored secret
     * @throws RuntimeException if the retrieved secret value is empty
     */
    public static String getSecret(
            @NonNull final AWSSecretsManager secretsManagerClient,
            @NonNull final String secretName) throws RuntimeException {
        final GetSecretValueRequest request = new GetSecretValueRequest().withSecretId(secretName);
        final GetSecretValueResult result;

        try {
            result = secretsManagerClient.getSecretValue(request);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Exception when getting secret value: %s",
                                                    ex.getMessage()), ex);
        }

        final String secretValue = result.getSecretString();

        if (!StringUtils.isEmpty(secretValue)) {
            return secretValue;
        } else {
            throw new RuntimeException(String.format("Secret %s is empty!", secretName));
        }
    }

    /**
     * Retrieves secret value given the secret name and region.
     *
     * @param secretName the name of the stored secret
     * @param secretRegion the region of the stored secret
     * @return the value of the stored secret
     * @throws RuntimeException if the retrieved secret value is empty
     */
    public static String getSecret(@NonNull final String secretName, @NonNull final String secretRegion) {
        final AWSSecretsManager secretsManagerClient = getSecretsManager(secretRegion);

        return SecretsManagerUtils.getSecret(secretsManagerClient, secretName);
    }

    /**
     * Creates and returns the AWSSecretsManager object
     * to be used for secrets retrieval.
     *
     * @param secretRegion the region of the stored secret
     * @return the ready to use AWS SecretsManager object
     */
    public static AWSSecretsManager getSecretsManager(@NonNull final String secretRegion) {
        return new SecretsManagerClientProvider()
                .getAwsSecretsManagerClient(secretRegion);
    }
}
