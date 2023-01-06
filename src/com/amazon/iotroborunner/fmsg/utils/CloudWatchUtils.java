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

import static com.amazon.iotroborunner.fmsg.constants.CloudWatchConstants.METRIC_CONTENT_KEY;
import static com.amazon.iotroborunner.fmsg.constants.CloudWatchConstants.METRIC_DIMENSIONS_KEY;
import static com.amazon.iotroborunner.fmsg.constants.CloudWatchConstants.METRIC_DIMENSION_NAME_KEY;
import static com.amazon.iotroborunner.fmsg.constants.CloudWatchConstants.METRIC_DIMENSION_VALUE_KEY;
import static com.amazon.iotroborunner.fmsg.constants.CloudWatchConstants.METRIC_NAMESPACE_KEY;
import static com.amazon.iotroborunner.fmsg.constants.CloudWatchConstants.METRIC_NAME_KEY;
import static com.amazon.iotroborunner.fmsg.constants.CloudWatchConstants.METRIC_REQUEST_KEY;
import static com.amazon.iotroborunner.fmsg.constants.CloudWatchConstants.METRIC_TIMESTAMP_KEY;
import static com.amazon.iotroborunner.fmsg.constants.CloudWatchConstants.METRIC_UNIT_KEY;
import static com.amazon.iotroborunner.fmsg.constants.CloudWatchConstants.METRIC_VALUE_KEY;

import java.time.Instant;
import java.util.List;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.util.CollectionUtils;
import lombok.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utilities designed to be used in regard to CloudWatch and related operations.
 */
public class CloudWatchUtils {

    /**
     * Constructs a JSON string representing metric data.
     *
     * @param namespace the required namespace of the metric
     * @param metricName the required name of the metric
     * @param dimensions the dimensions of the metric
     * @param unit the required unit of the metric
     * @param value the required value of the metric
     * @return the string representing the metric json
     */
    public static String constructMetricJsonString(@NonNull final String namespace,
                                               @NonNull final String metricName,
                                               final List<Dimension> dimensions,
                                               @NonNull final StandardUnit unit,
                                               final double value) {
        final JSONObject metricData = new JSONObject() {{
                put(METRIC_NAME_KEY, metricName);
                put(METRIC_UNIT_KEY, unit);
                put(METRIC_VALUE_KEY, value);
                put(METRIC_TIMESTAMP_KEY, Instant.now());
            }};

        if (!CollectionUtils.isNullOrEmpty(dimensions)) {
            final JSONArray dimensionsArr = new JSONArray();

            // Add the dimensions one by one instead of providing the collection
            // as a param to avoid any changes to the property formatting.
            for (final Dimension dimension : dimensions) {
                final JSONObject dimensionObj = new JSONObject();
                dimensionObj.put(METRIC_DIMENSION_NAME_KEY, dimension.getName());
                dimensionObj.put(METRIC_DIMENSION_VALUE_KEY, dimension.getValue());
                dimensionsArr.put(dimensionObj);
            }
            metricData.put(METRIC_DIMENSIONS_KEY, dimensionsArr);
        }

        final JSONObject metricObj = new JSONObject() {{
                put(METRIC_NAMESPACE_KEY, namespace);
                put(METRIC_CONTENT_KEY, metricData);
            }};

        final JSONObject request = new JSONObject() {{
                put(METRIC_REQUEST_KEY, metricObj);
            }};

        return request.toString();
    }

    /**
     * Hidden constructor.
     */
    private CloudWatchUtils() {
        throw new UnsupportedOperationException("This class is for holding utilities and should not be instantiated.");
    }
}
