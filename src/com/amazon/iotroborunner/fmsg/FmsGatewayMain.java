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

package com.amazon.iotroborunner.fmsg;

import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfigurationReader;
import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfiguration;
import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfigurationReader;
import com.amazon.iotroborunner.fmsg.connectors.FmsConnector;
import com.amazon.iotroborunner.fmsg.sharedspacemgmt.FmsgSharedSpaceMgmt;
import com.amazon.iotroborunner.fmsg.workerpropertyupdates.FmsgWorkerPropertyUpdates;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Entry point for FMS Gateway application.
 */

@Log4j2
public final class FmsGatewayMain {
    private FmsGatewayMain() {
    }

    /**
     * The entry point of the application.
     *
     * @param args the input arguments
     * @throws IOException if the config files can't be read
     */
    public static void main(final String[] args) throws IOException {
        // Set log level to INFO for all classes.
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);
        log.info("Starting FMS Gateway application");

        final String configDir = System.getenv("FMSG_CONFIGURATION_DIRECTORY");
        final List<FmsgConnectorConfiguration> fmsConnectorConfigs = getFmsgConnectorConfigurations(configDir);
        final FmsgCoreConfiguration fmsgConfig = getFmsgConfiguration(configDir);

        final Map<String, FmsConnector> connectorsByWorkerFleet =
            getConnectorsByWorkerFleetArn(fmsgConfig, fmsConnectorConfigs);
        if (connectorsByWorkerFleet.isEmpty()) {
            log.error("No FMSG connectors configured, exiting");
            return;
        }

        final boolean workerPropertyUpdatesEnabled = fmsgConfig.isWorkerPropertyUpdatesEnabled();
        final boolean spaceManagementEnabled = fmsgConfig.isSpaceManagementEnabled();

        if (workerPropertyUpdatesEnabled) {
            FmsgWorkerPropertyUpdates.startWorkerPropertyUpdates(connectorsByWorkerFleet);
        }

        if (spaceManagementEnabled) {
            final FmsgSharedSpaceMgmt sharedSpaceMgmt = new FmsgSharedSpaceMgmt(fmsgConfig);
            sharedSpaceMgmt.startSharedSpaceMgmt(connectorsByWorkerFleet);
        }

        if (!workerPropertyUpdatesEnabled && !spaceManagementEnabled) {
            log.error("No FMSG applications enabled, exiting");
            return;
        }
    }

    /**
     * Creates fmsg based on config files.
     *
     * @param configDir the directory containing config files
     * @return fmsg configuration
     */
    private static FmsgCoreConfiguration getFmsgConfiguration(final String configDir) throws IOException {
        final FmsgCoreConfigurationReader fmsgConfigurationReader =
            new FmsgCoreConfigurationReader(configDir);
        return fmsgConfigurationReader.getFmsgCoreConfiguration();
    }

    /**
     * Creates list of FMSG connector configurations based on config files.
     *
     * @param configDir the directory containing config files
     * @return list of FMSG connector configurations
     */
    private static List<FmsgConnectorConfiguration> getFmsgConnectorConfigurations(final String configDir)
            throws IOException {
        final FmsgConnectorConfigurationReader fmsgConnectorConfigReader =
            new FmsgConnectorConfigurationReader(configDir);
        return fmsgConnectorConfigReader.getAllFleetManagerConfigs();
    }

    /**
     * Builds of map of FMS Connectors by their respective worker fleets. At this time there is a 1 to 1 relationship
     * between connectors and worker fleets. Having such a mapping makes it easy for FMSG Applications to contact the
     * correct vendor FMS just based on the fleet the worker is assigned to.
     *
     * @param fmsgCoreConfigs      FMSG Core Configurations.
     * @param fmsgConnectorConfigs FMSG Connector Configurations.
     * @return Map of fully enabled connectors to
     */
    private static Map<String, FmsConnector> getConnectorsByWorkerFleetArn(
        @NonNull final FmsgCoreConfiguration fmsgCoreConfigs,
        @NonNull final List<FmsgConnectorConfiguration> fmsgConnectorConfigs) {

        final Map<String, FmsConnector> connectorsByArn = new HashMap<>();
        for (final FmsgConnectorConfiguration connectorConfig : fmsgConnectorConfigs) {
            final String fleetType = connectorConfig.getFleetType();
            if (connectorConfig.isEnableConnector()) {
                log.info("Building a FMS Connector for {}", fleetType);
                final FmsConnector fmsConnector = FmsConnectorFactory.getFmsConnectorForFleetType(
                    fleetType, connectorConfig);

                if (fmsgCoreConfigs.isSpaceManagementEnabled()) {
                    log.info("Setting up Shared Space Management for FMS Connector: {}", fleetType);
                    fmsConnector.setupSharedSpaceManagement();
                }

                if (fmsgCoreConfigs.isWorkerPropertyUpdatesEnabled()) {
                    log.info("Setting up Worker Property Updates for FMS Connector: {}", fleetType);
                    fmsConnector.setupWorkerPropertyUpdates();
                }
                connectorsByArn.put(connectorConfig.getWorkerFleetArn(), fmsConnector);
            }
        }
        return connectorsByArn;
    }

}
