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

package com.amazon.iotroborunner.fmsg.config;

import com.amazon.iotroborunner.fmsg.config.validators.FmsgCoreConfigurationValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Class that reads RoboRunner FMSG configuration from a JSON file and
 * deserializes it into a configuration object.
 */
@Log4j2
@RequiredArgsConstructor
public class FmsgCoreConfigurationReader {
    private static final String CONFIG_FILE_NAME = "fmsgCoreConfiguration.json";
    private static final String CONFIG_ROOT_OBJECT_NAME = "fmsgCoreConfiguration";
    private static final String SITE_ARN = "siteArn";
    private static final String WORKER_PROPERTY_UPDATES_CONFIG_NAME = "enableWorkerPropertyUpdates";
    private static final String SPACE_MANAGEMENT_CONFIG_NAME = "enableSpaceManagement";
    private static final String MAX_SHARED_SPACE_CROSSING_TIME_CONFIG_NAME = "maximumSharedSpaceCrossingTime";
    private static final String VENDOR_SHARED_SPACE_POLLING_INTERVAL_CONFIG_NAME = "vendorSharedSpacePollingInterval";

    @NonNull
    private String configDir;

    /**
     * Read and parse the FMSG core configuration file.
     *
     * @return                  The FmsgCoreConfiguration object representing configuration values
     * @throws IOException      If there is a problem with reading the config file
     * @throws RuntimeException If there is a problem validating the entered configuration values
     */
    public FmsgCoreConfiguration getFmsgCoreConfiguration() throws IOException, RuntimeException {
        final Path filePath = this.getFmsgCoreConfigurationFilePath(this.configDir);
        log.info(String.format("Reading RoboRunner FMSG configuration from %s", filePath.toString()));

        final ObjectMapper mapper = new ObjectMapper();
        final JsonFactory parserFactory = mapper.getFactory();
        final JsonParser parser = parserFactory.createParser(Files.readAllBytes(filePath));
        final JsonNode rootNode = parser.getCodec().readTree(parser);
        final JsonNode node = rootNode.get(CONFIG_ROOT_OBJECT_NAME);

        final String siteArn = node.get(SITE_ARN).asText();
        boolean workerPropertyUpdates = false;
        boolean spaceManagement = false;
        int maximumSharedSpaceCrossingTime = 300;
        int vendorSharedSpacePollingInterval = 3;

        if (node.findValue(WORKER_PROPERTY_UPDATES_CONFIG_NAME) != null) {
            workerPropertyUpdates = node.get(WORKER_PROPERTY_UPDATES_CONFIG_NAME).asBoolean();
        }
        if (node.findValue(SPACE_MANAGEMENT_CONFIG_NAME) != null) {
            spaceManagement = node.get(SPACE_MANAGEMENT_CONFIG_NAME).asBoolean();
        }
        if (node.findValue(MAX_SHARED_SPACE_CROSSING_TIME_CONFIG_NAME) != null) {
            maximumSharedSpaceCrossingTime = node.get(MAX_SHARED_SPACE_CROSSING_TIME_CONFIG_NAME).asInt();
        }
        if (node.findValue(VENDOR_SHARED_SPACE_POLLING_INTERVAL_CONFIG_NAME) != null) {
            vendorSharedSpacePollingInterval = node.get(VENDOR_SHARED_SPACE_POLLING_INTERVAL_CONFIG_NAME).asInt();
        }

        final FmsgCoreConfiguration rrFmsgConfig = FmsgCoreConfiguration.builder()
                .siteArn(siteArn)
                .workerPropertyUpdatesEnabled(workerPropertyUpdates)
                .spaceManagementEnabled(spaceManagement)
                .maximumSharedSpaceCrossingTime(maximumSharedSpaceCrossingTime)
                .vendorSharedSpacePollingInterval(vendorSharedSpacePollingInterval)
                .build();

        final FmsgCoreConfigurationValidator validator = new FmsgCoreConfigurationValidator();
        if (validator.validateConfiguration(rrFmsgConfig).size() > 0) {
            throw new RuntimeException(
                "Invalid configuration field(s) in "
                + CONFIG_FILE_NAME
                + ": "
                + validator.validateConfiguration(rrFmsgConfig)
            );
        }

        return rrFmsgConfig;
    }

    Path getFmsgCoreConfigurationFilePath(@NonNull final String configDir) {
        final Path configDirPath = Paths.get(configDir);
        final Path configFilePath = Paths.get(CONFIG_FILE_NAME);
        return configDirPath.resolve(configFilePath);
    }
}
