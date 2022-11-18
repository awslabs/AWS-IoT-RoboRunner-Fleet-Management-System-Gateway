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

package com.amazon.iotroborunner.fmsg.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;
import com.amazon.iotroborunner.fmsg.types.RobotFleetType;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Unit tests for the FMSG connector configuration reader module. */
public class FmsgConnectorConfigurationReaderTest {
    private static final String CONFIG_NAME = "fmsgConnectorConfiguration.json";
    private static final String ADDITIONAL_CONFIG_KEY = "additionalStuff";
    private final  FmsgConnectorConfigurationReader reader =
            new FmsgConnectorConfigurationReader(TestConstants.CONFIG_FILE_DIRECTORY);

    @Test
    void given_validParameters_when_getFmsgConnectorConfigurationFilePath_then_returnsCorrectPath() {
        final String path =
                reader.getFmsgConnectorConfigurationFilePath(TestConstants.CONFIG_FILE_DIRECTORY).toString();

        assertNotNull(path);
        assertEquals(String.format("%s/%s", TestConstants.CONFIG_FILE_DIRECTORY, CONFIG_NAME), path);
    }

    @Test
    void given_validConfigurationJson_when_getAllFleetManagerConfigs_then_returnsValidConfigurationObject()
            throws IOException {
        final List<FmsgConnectorConfiguration> configs = reader.getAllFleetManagerConfigs();
        assertNotNull(configs);
        assertEquals(1, configs.size());

        final FmsgConnectorConfiguration config = configs.get(0);
        assertEquals(RobotFleetType.MIR.value, config.getFleetType());
        assertEquals(TestConstants.VENDOR_API_ENDPOINT, config.getApiEndpoint());
        assertEquals(TestConstants.Secret.MIR_SECRET.name, config.getApiSecretName());
        assertEquals(TestConstants.WORKER_FLEET_ARN, config.getWorkerFleetArn());
        assertEquals(TestConstants.SITE_ARN, config.getSiteArn());
        assertEquals(TestConstants.Region.EU_CENTRAL_1.name, config.getAwsRegion());
        assertNotNull(config.getAdditionalConfiguration());
        assertEquals(1, config.getAdditionalConfiguration().size());
        assertEquals(TestConstants.Secret.GENERAL_SECRET.name,
                config.getAdditionalConfiguration().get(ADDITIONAL_CONFIG_KEY));
        assertFalse(config.isEnableConnector());
    }
}
