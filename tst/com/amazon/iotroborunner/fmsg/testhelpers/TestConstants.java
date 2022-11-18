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

/**
 * TestConstants contains general constants that can be used in multiple test files.
 */
public final class TestConstants {
    // IoT RoboRunner resource constant(s).
    public static final String SITE_ARN =
            "arn:aws:iotroborunner:eu-central-1:accountId:site/7ba753d4-d764-4f5d-b762-98f924ba5ce9";
    public static final String WORKER_FLEET_ARN =
            "arn:aws:iotroborunner:eu-central-1:accountId:site/7ba753d4-d764-4f5d-b762-98f924ba5ce9/worker-fleet/"
                    + "f6749edd-54c8-4510-8a14-868d73bff031";
    public static final String INVALID_SITE_ARN = "INVALID_ARN";
    public static final String INVALID_WORKER_FLEET_ARN = "INVALID_ARN";
    public static final String WORKER_ARN =
            "arn:aws:iotroborunner:eu-central-1:accountId:site/7ba753d4-d764-4f5d-b762-98f924ba5ce9/worker-fleet/"
                    + "f6749edd-54c8-4510-8a14-868d73bff031/worker/3490d42e-d764-0000-tfl0-98f924ba5111";
    public static final String DESTINATION_ARN =
            "arn:aws:iotroborunner:eu-central-1:accountId:destination/11111111-dddd-f3f3-3234-09s824ba0055";

    /** Enum defining AWS regions. */
    public enum Region {
        EU_CENTRAL_1("eu-central-1"),
        US_EAST_1("us-east-1");

        public final String name;

        Region(final String name) {
            this.name = name;
        }
    }

    /** Enum defining SecretsManager secret names. */
    public enum Secret {
        GENERAL_SECRET("SecretString"),
        MIR_SECRET("MirSecretString");

        public final String name;

        Secret(final String name) {
            this.name = name;
        }
    }

    // HTTP constant(s).
    public static final String EMPTY_HTTP_PAYLOAD = "";

    public static final String HTTP_GET_REQUEST_METHOD = "GET";
    // Vendor constant(s).
    public static final String MIR_ROBOT_ID = "7";
    public static final String VENDOR_API_ENDPOINT = "http://example.com/api/v2.0.0";
    public static final String UNREACHABLE_VENDOR_API_ENDPOINT = "http://20.215.184.210/api/v2.0.0";

    public static final String VENDOR_API_ROBOT_STATUS_REQUEST = "/RobotStatus/";
    public static final String VENDOR_API_IP_ENDPOINT = "123.45.67.89";

    // Config related constant(s).
    public static final String CONFIG_FILE_DIRECTORY = "tst/com/amazon/iotroborunner/fmsg/config";

    // ARCL specific constants
    public static final String ROBOT_IP = "192.168.1.1";

    // Shared Space constants
    public static final String POLYGON_JSON_EMPTY = "{\"polygon\":[]}";

    public static final String FAILURE_MESSAGE = "Failure message.";

    // RoboRunner FMSG Configuration constants.
    public static final int MAX_SHARED_SPACE_CROSSING_TIME = 200;
    public static final int VENDOR_SHARED_SPACE_POLLING_INTERVAL = 5;

    /**
     * Utility/Constant class, don't allow instantiation.
     */
    private TestConstants() {
        throw new UnsupportedOperationException("This class is for holding constants and should not be instantiated");
    }
}
