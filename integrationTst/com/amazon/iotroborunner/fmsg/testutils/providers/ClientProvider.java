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

package com.amazon.iotroborunner.fmsg.testutils.providers;

import com.amazon.iotroborunner.fmsg.clients.IotRoboRunnerJavaClientProvider;
import com.amazon.iotroborunner.fmsg.clients.MirFmsHttpClient;
import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfiguration;
import com.amazon.iotroborunner.fmsg.types.RobotFleetType;
import com.amazon.iotroborunner.fmsg.utils.SecretsManagerUtils;

import java.util.Optional;

import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;

/**
 * ClientProvider class provides static methods to acquire
 * various clients for vendor FMSes and IoT RoboRunner.
 */
public final class ClientProvider {
    /**
     * Creates and returns IoT RoboRunner Client based on
     * provided configurations for IoT RoboRunner.
     *
     * @return  the built IoT RoboRunner Client
     */
    public static synchronized AWSIoTRoboRunner getRoboRunnerClient() {
        final FmsgCoreConfiguration roboRunnerConfig = ConfigurationProvider.getIotRoboRunnerConfig();
        return new IotRoboRunnerJavaClientProvider()
                .getAwsIotRoboRunnerClient(roboRunnerConfig.getAwsRegion());
    }

    /**
     * Creates and returns Mir FMS Client based on
     * provided configuration for Mir FMS.
     *
     * @return the created Mir FMS Client
     */
    public static synchronized Optional<MirFmsHttpClient> getMirFmsHttpClient() {
        final FmsgConnectorConfiguration mirFmsConfig = ConfigurationProvider.getFmsgConfigs()
                .get(RobotFleetType.MIR.value);

        // In case Mir configuration was not provided as part of the config file,
        // will return empty and let the caller decide the further actions.
        if (mirFmsConfig == null) {
            return Optional.empty();
        }

        final String authSecretValue = SecretsManagerUtils.getSecret(
                mirFmsConfig.getApiSecretName(),
                mirFmsConfig.getAwsRegion());

        return Optional.of(new MirFmsHttpClient(
                mirFmsConfig.getApiEndpoint(), authSecretValue));
    }
}
