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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfiguration;
import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for the RoboRunner FMSG cofiguration validator module. */
public class FmsgCoreConfigurationValidatorTest {
    private FmsgCoreConfigurationValidator validator;

    @BeforeEach
    public void setup() {
        validator = new FmsgCoreConfigurationValidator();
    }

    @Test
    public void given_validParameters_when_constructed_then_passes() {
        assertNotNull(validator);
    }

    @Test
    public void given_nullConfiguration_when_validateConfiguration_then_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            validator.validateConfiguration(null);
        });
    }

    @Test
    public void given_invalidSiteArn_when_validateConfiguration_then_returnsListSizeOne() {
        final FmsgCoreConfiguration rrFmsgConfig = FmsgCoreConfiguration.builder()
            .siteArn(TestConstants.INVALID_SITE_ARN)
            .workerPropertyUpdatesEnabled(true)
            .spaceManagementEnabled(true)
            .build();

        assertEquals(validator.validateConfiguration(rrFmsgConfig).size(), 1);
    }

    @Test
    public void given_validConfigurationRequiredValues_when_validateConfiguration_then_returnsListSizeZero() {
        final FmsgCoreConfiguration rrFmsgConfig = FmsgCoreConfiguration.builder()
            .siteArn(TestConstants.SITE_ARN)
            .build();
        assertEquals(validator.validateConfiguration(rrFmsgConfig).size(), 0);
    }

    @Test
    public void given_validConfigurationAllValues_when_validateConfiguration_then_returnsListSizeZero() {
        final FmsgCoreConfiguration rrFmsgConfig = FmsgCoreConfiguration.builder()
            .siteArn(TestConstants.SITE_ARN)
            .workerPropertyUpdatesEnabled(true)
            .spaceManagementEnabled(true)
            .maximumSharedSpaceCrossingTime(10)
            .vendorSharedSpacePollingInterval(4)
            .build();
        assertEquals(validator.validateConfiguration(rrFmsgConfig).size(), 0);
    }

    @Test
    public void given_invalidMaxSharedSpaceCrossingTime_tooSmall_when_validateConfiguration_then_returnsListSizeOne() {
        final FmsgCoreConfiguration rrFmsgConfig = FmsgCoreConfiguration.builder()
            .siteArn(TestConstants.SITE_ARN)
            .workerPropertyUpdatesEnabled(true)
            .spaceManagementEnabled(true)
            .maximumSharedSpaceCrossingTime(0)
            .build();
        assertEquals(validator.validateConfiguration(rrFmsgConfig).size(), 1);
    }

    @Test
    public void given_invalidMaxSharedSpaceCrossingTime_tooLarge_when_validateConfiguration_then_returnsListSizeOne() {
        final FmsgCoreConfiguration rrFmsgConfig = FmsgCoreConfiguration.builder()
            .siteArn(TestConstants.SITE_ARN)
            .workerPropertyUpdatesEnabled(true)
            .spaceManagementEnabled(true)
            .maximumSharedSpaceCrossingTime(901)
            .build();

        assertEquals(validator.validateConfiguration(rrFmsgConfig).size(), 1);
    }

    @Test
    public void given_invalidPollingInterval_tooSmall_when_validateConfiguration_then_returnsListSizeOne() {
        final FmsgCoreConfiguration rrFmsgConfig = FmsgCoreConfiguration.builder()
            .siteArn(TestConstants.SITE_ARN)
            .workerPropertyUpdatesEnabled(true)
            .spaceManagementEnabled(true)
            .vendorSharedSpacePollingInterval(0)
            .build();

        assertEquals(validator.validateConfiguration(rrFmsgConfig).size(), 1);
    }

    @Test
    public void given_invalidPollingInterval_tooLarge_when_validateConfiguration_then_returnsListSizeOne() {
        final FmsgCoreConfiguration rrFmsgConfig = FmsgCoreConfiguration.builder()
            .siteArn(TestConstants.SITE_ARN)
            .workerPropertyUpdatesEnabled(true)
            .spaceManagementEnabled(true)
            .vendorSharedSpacePollingInterval(10)
            .build();

        assertEquals(validator.validateConfiguration(rrFmsgConfig).size(), 1);
    }

    @Test
    public void given_invalidPollingInterval_and_MaxSharedSpace_when_validateConfiguration_then_returnsListSizeTwo() {
        final FmsgCoreConfiguration rrFmsgConfig = FmsgCoreConfiguration.builder()
            .siteArn(TestConstants.SITE_ARN)
            .workerPropertyUpdatesEnabled(true)
            .spaceManagementEnabled(true)
            .vendorSharedSpacePollingInterval(10)
            .maximumSharedSpaceCrossingTime(901)
            .build();

        assertEquals(validator.validateConfiguration(rrFmsgConfig).size(), 2);
    }
}
