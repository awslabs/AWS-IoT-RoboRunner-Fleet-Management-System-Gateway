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

package com.amazon.iotroborunner.fmsg.constants;

/**
 * Constants to be used when working with CloudWatch or CloudWatch data.
 */
public class CloudWatchConstants {
    /**
     * The key used for publishing a metric.
     */
    public static final String METRIC_REQUEST_KEY = "request";

    /**
     * The key used for the namespace of a metric.
     */
    public static final String METRIC_NAMESPACE_KEY = "namespace";

    /**
     * The key used for the data of a metric.
     */
    public static final String METRIC_CONTENT_KEY = "metricData";

    /**
     * The key used for the name of a metric.
     */
    public static final String METRIC_NAME_KEY = "MetricName";

    /**
     * The key used for the dimensions of a metric.
     */
    public static final String METRIC_DIMENSIONS_KEY = "Dimensions";

    /**
     * The key used for the unit of a metric.
     */
    public static final String METRIC_UNIT_KEY = "Unit";

    /**
     * The key used for the value of a metric.
     */
    public static final String METRIC_VALUE_KEY = "Value";

    /**
     * The key used for the timestamp of a metric.
     */
    public static final String METRIC_TIMESTAMP_KEY = "Timestamp";

    /**
     * The key used for the name of the dimension in a metric.
     */
    public static final String METRIC_DIMENSION_NAME_KEY = "Name";

    /**
     * The key used for the value of the dimension in a metric.
     */
    public static final String METRIC_DIMENSION_VALUE_KEY = "Value";

    /**
     * Hidden constructor.
     */
    private CloudWatchConstants() {
        throw new UnsupportedOperationException("This class is for holding constants and should not be instantiated.");
    }
}
