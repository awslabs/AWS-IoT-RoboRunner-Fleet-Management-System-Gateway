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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Class that reads integration test specific configuration from a JSON file and
 * deserializes it into configuration objects.
 */
@RequiredArgsConstructor
public class IntegrationTestPropertyConfigurationReader {
    private static final String INTEGRATION_TEST_PROPERTY_CONFIG_FILE_NAME =
            "integrationTestPropertyConfiguration.json";
    private static final String INTEGRATION_TEST_PROPERTY_CONFIGURATION = "integrationTestPropertyConfiguration";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @NonNull
    private String configDir;

    /** Method to read in the JSON configuration and validate the fields.
     *
     * @return The map of fleet type string to FmsgConnectorConfiguration for each FMS in the config file.
     * @throws IOException If there is an invalid field in the config file.
     */
    public List<IntegrationTestPropertyConfiguration> getAllIntegrationTestPropertyConfigs() throws IOException {
        final Path filePath = this.getIntegrationTestPropertyConfigurationFilePath(this.configDir);

        final JsonFactory parserFactory = OBJECT_MAPPER.getFactory();
        final JsonParser parser = parserFactory.createParser(Files.readAllBytes(filePath));
        final JsonNode node = parser.getCodec().readTree(parser);

        final List<IntegrationTestPropertyConfiguration> configs = OBJECT_MAPPER.readValue(
                String.valueOf(node.get(INTEGRATION_TEST_PROPERTY_CONFIGURATION)),
                new TypeReference<List<IntegrationTestPropertyConfiguration>>(){}
        );

        return configs;
    }

    Path getIntegrationTestPropertyConfigurationFilePath(@NonNull final String configDir) {
        final Path configDirPath = Paths.get(configDir);
        final Path configFilePath = Paths.get(INTEGRATION_TEST_PROPERTY_CONFIG_FILE_NAME);
        return configDirPath.resolve(configFilePath);
    }
}
