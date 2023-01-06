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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the CloudWatch utility module.
 */
@ExtendWith(MockitoExtension.class)
public class CloudWatchUtilsTest {
    @Test
    void given_allValidParams_when_constructMetricJsonString_then_returnsValidMetricJsonString() throws JsonProcessingException {
        final String namespace = "FleetManagementSystemGateway";
        final String metricName = "SecretRetrievalFailed";
        final String dimensionName = "secretName";
        final String dimensionValue = "testSecret";
        final List<Dimension> dimensions = new ArrayList<>() {{
                add(new Dimension()
                    .withName(dimensionName)
                    .withValue(dimensionValue));
            }};
        final String metricRequestString = CloudWatchUtils.constructMetricJsonString(
                namespace, metricName, dimensions, StandardUnit.Count, 1);

        final JSONObject metricRequestJsonObj = new JSONObject(metricRequestString).getJSONObject(METRIC_REQUEST_KEY);
        final JSONObject metricDataJsonObj = metricRequestJsonObj.getJSONObject(METRIC_CONTENT_KEY);
        final JSONArray metricDimensionsJsonArr = metricDataJsonObj.getJSONArray(METRIC_DIMENSIONS_KEY);
        assertEquals(namespace, metricRequestJsonObj.getString(METRIC_NAMESPACE_KEY));
        assertEquals(metricName, metricDataJsonObj.getString(METRIC_NAME_KEY));
        assertEquals(1, metricDimensionsJsonArr.length());
        final JSONObject dimensionJsonObj = metricDimensionsJsonArr.getJSONObject(0);
        assertEquals(dimensionName, dimensionJsonObj.getString(METRIC_DIMENSION_NAME_KEY));
        assertEquals(dimensionValue, dimensionJsonObj.getString(METRIC_DIMENSION_VALUE_KEY));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @Disabled
    void given_allValidParamsWithNoDimensions_when_constructMetricJsonString_then_jsonStringDoesNotContainDimensions(
            final List<Dimension> dimensions) {
        final String metricRequestString = CloudWatchUtils.constructMetricJsonString(
                "FleetManagementSystemGateway", "SecretRetrievalFailed", dimensions, StandardUnit.Count, 1);

        final JSONObject metricRequestJsonObj = new JSONObject(metricRequestString).getJSONObject(METRIC_REQUEST_KEY);
        final JSONObject metricDataJsonObj = metricRequestJsonObj.getJSONObject(METRIC_CONTENT_KEY);
        assertFalse(metricDataJsonObj.has(METRIC_DIMENSIONS_KEY));
    }
}
