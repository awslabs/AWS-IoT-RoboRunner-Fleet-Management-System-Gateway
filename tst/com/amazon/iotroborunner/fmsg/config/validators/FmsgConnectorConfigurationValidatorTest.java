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

package com.amazon.iotroborunner.fmsg.config.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;
import com.amazon.iotroborunner.fmsg.types.RobotFleetType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for the FMSG configuration validator module. */
public class FmsgConnectorConfigurationValidatorTest {
    private FmsgConnectorConfigurationValidator validator;

    @BeforeEach
    public void setup() {
        validator = new FmsgConnectorConfigurationValidator();
    }

    @Test
    public void given_validParameters_when_constructed_then_passes() {
        final FmsgConnectorConfigurationValidator fmsConfigValidator = new FmsgConnectorConfigurationValidator();
        assertNotNull(fmsConfigValidator);
    }

    @Test
    public void given_nullConfiguration_when_validateConfiguration_then_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            validator.validateConfiguration(null);
        });
    }

    @Test
    public void given_unsupportedFleetType_when_validateConfiguration_then_returnsFalse() {
        final FmsgConnectorConfiguration config = FmsgConnectorConfiguration.builder()
            .fleetType("MUFFINS!")
            .workerFleetArn(TestConstants.WORKER_FLEET_ARN)
            .apiEndpoint(TestConstants.VENDOR_API_ENDPOINT)
            .apiSecretName(TestConstants.Secret.GENERAL_SECRET.name).build();
        assertFalse(validator.validateConfiguration(config));
    }

    @Test
    public void given_invalidWorkerFleetArn_when_validateConfiguration_then_returnsFalse() {
        final FmsgConnectorConfiguration config = FmsgConnectorConfiguration.builder()
            .fleetType(RobotFleetType.MIR.value)
            .workerFleetArn(TestConstants.INVALID_WORKER_FLEET_ARN)
            .apiEndpoint(TestConstants.VENDOR_API_ENDPOINT)
            .apiSecretName(TestConstants.Secret.GENERAL_SECRET.name).build();
        assertFalse(validator.validateConfiguration(config));
    }

    @Test
    public void given_invalidApiEndpoint_when_validateConfiguration_then_returnsFalse() {
        final FmsgConnectorConfiguration config = FmsgConnectorConfiguration.builder()
            .fleetType(RobotFleetType.MIR.value)
            .workerFleetArn(TestConstants.WORKER_FLEET_ARN)
            .apiEndpoint("MUFFINS!")
            .apiSecretName(TestConstants.Secret.GENERAL_SECRET.name).build();
        assertFalse(validator.validateConfiguration(config));
    }

    @Test
    public void given_validConfigurationWithIpAddressEndpoint_when_validateConfiguration_then_returnsTrue() {
        final FmsgConnectorConfiguration config = FmsgConnectorConfiguration.builder()
            .fleetType(RobotFleetType.MIR.value)
            .workerFleetArn(TestConstants.WORKER_FLEET_ARN)
            .apiEndpoint(TestConstants.VENDOR_API_IP_ENDPOINT)
            .apiSecretName(TestConstants.Secret.GENERAL_SECRET.name).build();
        assertTrue(validator.validateConfiguration(config));
    }

    @Test
    public void given_unreachableApiEndpoint_when_validateConfiguration_then_returnsFalse() {
        final FmsgConnectorConfiguration config = FmsgConnectorConfiguration.builder()
            .fleetType(RobotFleetType.MIR.value)
            .workerFleetArn(TestConstants.WORKER_FLEET_ARN)
            .apiEndpoint(TestConstants.UNREACHABLE_VENDOR_API_ENDPOINT)
            .apiSecretName(TestConstants.Secret.GENERAL_SECRET.name).build();
        assertFalse(validator.validateConfiguration(config));
    }

    @Test
    public void given_validConfigurationWithHttpEndpoint_when_validateConfiguration_then_returnsTrue() {
        final FmsgConnectorConfiguration config = FmsgConnectorConfiguration.builder()
            .fleetType(RobotFleetType.MIR.value)
            .workerFleetArn(TestConstants.WORKER_FLEET_ARN)
            .apiEndpoint(TestConstants.VENDOR_API_ENDPOINT)
            .apiSecretName(TestConstants.Secret.GENERAL_SECRET.name).build();
        assertTrue(validator.validateConfiguration(config));
    }
}
