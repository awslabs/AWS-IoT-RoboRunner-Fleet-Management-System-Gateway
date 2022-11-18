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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/** Unit tests for the FMSG core configuration reader module. */
public class FmsgCoreConfigurationReaderTest {
    private static final String CONFIG_NAME = "fmsgCoreConfiguration.json";

    @Test
    void given_validParameters_when_getFleetManagerConfigurationFilePath_then_returnsCorrectPath() {
        final FmsgCoreConfigurationReader reader =
                new FmsgCoreConfigurationReader(TestConstants.CONFIG_FILE_DIRECTORY);

        final String path =
                reader.getFmsgCoreConfigurationFilePath(TestConstants.CONFIG_FILE_DIRECTORY).toString();

        assertNotNull(path);
        assertEquals(path, String.format("%s/%s", TestConstants.CONFIG_FILE_DIRECTORY, CONFIG_NAME));
    }

    @Test
    void given_validConfigurationJson_when_getAllFleetManagerConfigs_then_returnsValidConfigurationObject()
            throws IOException {
        final FmsgCoreConfigurationReader reader =
                new FmsgCoreConfigurationReader(TestConstants.CONFIG_FILE_DIRECTORY);

        final FmsgCoreConfiguration rrFmsgConfig = reader.getFmsgCoreConfiguration();

        assertNotNull(rrFmsgConfig);
        assertEquals(TestConstants.SITE_ARN, rrFmsgConfig.getSiteArn());
        assertTrue(rrFmsgConfig.isWorkerPropertyUpdatesEnabled());
        assertFalse(rrFmsgConfig.isSpaceManagementEnabled());
        assertEquals(TestConstants.MAX_SHARED_SPACE_CROSSING_TIME, rrFmsgConfig.getMaximumSharedSpaceCrossingTime());
        assertEquals(
                TestConstants.VENDOR_SHARED_SPACE_POLLING_INTERVAL,
                rrFmsgConfig.getVendorSharedSpacePollingInterval()
        );
    }
}
