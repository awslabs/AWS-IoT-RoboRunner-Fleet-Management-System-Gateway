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

package com.amazon.iotroborunner.fmsg.testhelpers;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

/**
 * TestUtils contains general utility static methods to help to keep test
 * files clean and organized when it comes to creating resources and more.
 */
public final class TestUtils {
    /**
     * Generates a uuid that can be used with ARNs and other resources.
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a new arn that is similar to given one but has a different id.
     */
    public static String generateSimilarArn(final String arn) {
        return String.format("%s/%s", StringUtils.substringBeforeLast(arn, "/"), UUID.randomUUID());
    }

    /*
     * Utility class, don't allow instantiation.
     */
    private TestUtils() {}
}
