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

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Utilities designed to be used by FMS Connectors.
 */
@Log4j2
public final class FmsConnectorUtils {
    /**
     * Hidden Constructor.
     */
    private FmsConnectorUtils() {
        throw new UnsupportedOperationException("This class is for holding utilities and should not be instantiated.");
    }

    /**
     * Determines if the method has permission to be called. Permission is determined based on what FMSG Applications
     * have been enabled. By blocking calls we can ensure that all required resources are set up for each function
     * and that we don't through NPEs without clarity.
     *
     * @param methodName Name of the method that's being called.
     * @param isFmsgAppEnabled Boolean stating if the related FMSG app has been configured.
     * @param appName Name of the FMSG app.
     */
    public static void blockIfApplicationNotEnabled(@NonNull final String methodName,
                                             final boolean isFmsgAppEnabled,
                                             @NonNull final String appName) {
        if (!isFmsgAppEnabled) {
            final String failureMsg =
                String.format("Unable to call %s because %s hasn't been enabled.", methodName, appName);
            log.warn(failureMsg);
            throw new UnsupportedOperationException(failureMsg);
        }
    }
}
