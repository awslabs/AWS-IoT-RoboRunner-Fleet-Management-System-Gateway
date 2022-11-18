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

import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfigurationReader;
import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfiguration;
import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfigurationReader;
import com.amazon.iotroborunner.fmsg.config.IntegrationTestPropertyConfiguration;
import com.amazon.iotroborunner.fmsg.config.IntegrationTestPropertyConfigurationReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConfigurationProvider class provides a way to read the integration test configuration files
 * once and provide them to other modules whenever needed without reading the files
 * multiple times.
 */
public final class ConfigurationProvider {
    /**
     * The location of FMSG Core and Connectors configuration files.
     */
    private static final String FMSG_CONFIG_DIR = "configuration/";
    /**
     * The location of integration test configuration files.
     */
    private static final String INTEGRATION_TEST_CONFIG_DIR = "integrationtstconfiguration/";

    /**
     * An object that contains the configuration details for IoT RoboRunner setup.
     */
    private static FmsgCoreConfiguration iotRoboRunnerConfig;

    /**
     * A map that contains fleetType to FMS configuration details mappings.
     */
    private static Map<String, FmsgConnectorConfiguration> fmsgConfigsByFleetType;

    /**
     * A map that contains fleetType to integration test property details mappings.
     */
    private static Map<String, IntegrationTestPropertyConfiguration> integTestPropertyConfigsByFleetType;

    /**
     * Gets the configuration details for IoT RoboRunner setup for integration testing by reading
     * the "fmsgCoreConfiguration.json" configuration file under FMSG_CONFIG_DIR directory.
     *
     * @return the configuration details of IoT RoboRunner service
     * @throws RuntimeException if the config file cannot be read
     */
    public static synchronized FmsgCoreConfiguration getIotRoboRunnerConfig() {
        if (iotRoboRunnerConfig == null) {
            final FmsgCoreConfigurationReader iotRoboRunnerConfigurationReader =
                    new FmsgCoreConfigurationReader(FMSG_CONFIG_DIR);

            try {
                iotRoboRunnerConfig = iotRoboRunnerConfigurationReader.getFmsgCoreConfiguration();
            } catch (final IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        return iotRoboRunnerConfig;
    }

    /**
     * Gets the configuration details provided in "fmsgConnectorConfiguration.json" file
     * for integration testing that is located under FMSG_CONFIG_DIR directory and
     * returns a map that represents fleetType to FMS configuration details mapping.
     *
     * @return the configuration details of vendor FMSs by fleet type
     * @throws RuntimeException if the config files cannot be read
     */
    public static synchronized Map<String, FmsgConnectorConfiguration> getFmsgConfigs() {
        if (fmsgConfigsByFleetType == null) {
            fmsgConfigsByFleetType = new ConcurrentHashMap<>();

            final FmsgConnectorConfigurationReader fleetManagerConfigReader = new FmsgConnectorConfigurationReader(
                    FMSG_CONFIG_DIR);

            final List<FmsgConnectorConfiguration> fleetManagerConfigs;
            try {
                fleetManagerConfigs = fleetManagerConfigReader.getAllFleetManagerConfigs();
            } catch (final IOException e) {
                throw new RuntimeException(e.getMessage());
            }

            for (final FmsgConnectorConfiguration fleetManagerConfig : fleetManagerConfigs) {
                fmsgConfigsByFleetType.put(fleetManagerConfig.getFleetType(), fleetManagerConfig);
            }
        }

        return Map.copyOf(fmsgConfigsByFleetType);
    }

    /**
     * Gets the configuration details provided in "integrationTestPropertyConfiguration.json" file
     * for integration testing that is located under INTEGRATION_TEST_CONFIG_DIR directory and
     * returns a map that represents fleetType to integration test configuration details mapping.
     *
     * @return the configuration specific details for integration testing by fleet type
     * @throws RuntimeException if the config files cannot be read
     */
    public static synchronized Map<String, IntegrationTestPropertyConfiguration> getIntegrationTestPropertyConfigs() {
        if (integTestPropertyConfigsByFleetType == null) {
            integTestPropertyConfigsByFleetType = new ConcurrentHashMap<>();

            final IntegrationTestPropertyConfigurationReader integrationTestPropertyConfigReader =
                    new IntegrationTestPropertyConfigurationReader(INTEGRATION_TEST_CONFIG_DIR);

            final List<IntegrationTestPropertyConfiguration> integrationTestPropertyConfigs;
            try {
                integrationTestPropertyConfigs = integrationTestPropertyConfigReader
                        .getAllIntegrationTestPropertyConfigs();
            } catch (final IOException e) {
                throw new RuntimeException(e.getMessage());
            }

            for (final IntegrationTestPropertyConfiguration integTestPropertyConfig : integrationTestPropertyConfigs) {
                integTestPropertyConfigsByFleetType.put(integTestPropertyConfig.getFleetType(),
                        integTestPropertyConfig);
            }
        }

        return Map.copyOf(integTestPropertyConfigsByFleetType);
    }
}
