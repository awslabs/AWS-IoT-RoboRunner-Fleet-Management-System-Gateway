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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.iotroborunner.fmsg.testhelpers.MockedAppender;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for FMS Connector Utils.
 */
public final class FmsConnectorUtilsTest {
    private static Logger logger;
    private static MockedAppender mockedAppender;

    /**
     * Set up logs required for each tests.
     */
    @BeforeAll
    public static void setUp() {
        mockedAppender = new MockedAppender();
        logger = (Logger) LogManager.getLogger(FmsConnectorUtils.class);
        logger.addAppender(mockedAppender);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    public void cleanUp() {
        mockedAppender.clear();
    }

    @Test
    public void given_disabledApp_when_blockIfApplicationNotEnabled_then_block() {
        final boolean isAppEnabled = false;
        final String expectedLog =
            String.format("Unable to call %s because %s hasn't been enabled.", "testMethod", "testApp");

        assertThrows(UnsupportedOperationException.class, () -> {
            FmsConnectorUtils.blockIfApplicationNotEnabled("testMethod", isAppEnabled, "testApp");
            mockedAppender.assertLogContainsMessage(expectedLog);
        });
    }

    @Test
    public void given_enabledApp_when_blockIfApplicationNotEnabled_then_dontBlock() {
        final boolean isAppEnabled = true;

        FmsConnectorUtils.blockIfApplicationNotEnabled("testMethod", isAppEnabled, "testApp");

        assertTrue(mockedAppender.message.isEmpty());
    }
}
