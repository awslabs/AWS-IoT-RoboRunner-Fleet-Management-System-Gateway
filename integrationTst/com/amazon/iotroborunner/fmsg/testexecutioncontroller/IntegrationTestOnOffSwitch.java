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

package com.amazon.iotroborunner.fmsg.testexecutioncontroller;

import static com.amazon.iotroborunner.fmsg.testexecutioncontroller.TestExecutionControllerConstants.SHARED_SPACE_MGMT_INTEGRATION_TEST_GROUP;
import static com.amazon.iotroborunner.fmsg.testexecutioncontroller.TestExecutionControllerConstants.WORKER_PROPERTY_UPDATES_INTEGRATION_TEST_GROUP;

import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfiguration;
import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfigurationReader;

import java.util.Set;

import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * IntegrationTestOnOffSwitch is used to evaluate FMSG integration test configuration to determine
 * which applications (None, SM, WPU, or both) are enabled or disabled and therefore which integration
 * test groups should be executed.
 * This class is used together with @RunTestIfApplicationEnabled annotation
 * to toggle test groups on or off assuming that all the integration test classes are
 * correctly annotated to belong to appropriate test groups (SmIntegrationTest, WpuIntegrationTest).
 */
public class IntegrationTestOnOffSwitch implements ExecutionCondition {
    private static final String FMSG_CONFIG_DIR = "configuration/";

    @SneakyThrows
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext context) {
        final FmsgCoreConfigurationReader fmsgCoreConfiguration = new FmsgCoreConfigurationReader(FMSG_CONFIG_DIR);

        ConditionEvaluationResult result = ConditionEvaluationResult
                .disabled("Application is not enabled in provided config");

        final FmsgCoreConfiguration fmsgConfig = fmsgCoreConfiguration.getFmsgCoreConfiguration();

        // Getting the test class tags that is currently being executed and determining which
        // integration test group it belongs to.
        final Set<String> testClassTags = context.getTags();
        if (testClassTags.contains(SHARED_SPACE_MGMT_INTEGRATION_TEST_GROUP)) {
            if (fmsgConfig.isSpaceManagementEnabled()) {
                result = ConditionEvaluationResult.enabled("Shared Space Management application is enabled");
            }
        } else if (testClassTags.contains(WORKER_PROPERTY_UPDATES_INTEGRATION_TEST_GROUP)) {
            if (fmsgConfig.isWorkerPropertyUpdatesEnabled()) {
                result = ConditionEvaluationResult.enabled("Worker Property Updates application is enabled");
            }
        } else {
            throw new UnsupportedOperationException("Unknown type of integration test group");
        }

        return result;
    }
}
