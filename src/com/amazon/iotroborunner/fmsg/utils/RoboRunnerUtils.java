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

import com.amazon.iotroborunner.fmsg.types.WorkerStatus;
import com.amazon.iotroborunner.fmsg.types.roborunner.WorkerAdditionalTransientProperties;
import com.amazon.iotroborunner.fmsg.types.roborunner.WorkerFleetAdditionalFixedProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.amazonaws.services.iotroborunner.model.GetWorkerFleetRequest;
import com.amazonaws.services.iotroborunner.model.GetWorkerFleetResult;
import com.amazonaws.services.iotroborunner.model.ListWorkersRequest;
import com.amazonaws.services.iotroborunner.model.ListWorkersResult;
import com.amazonaws.services.iotroborunner.model.UpdateWorkerRequest;
import com.amazonaws.services.iotroborunner.model.UpdateWorkerResult;
import com.amazonaws.services.iotroborunner.model.Worker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

/**
 * Class with helper methods to interact with IoT RoboRunner service.
 */
@AllArgsConstructor
@Log4j2
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class RoboRunnerUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @NonNull
    private AWSIoTRoboRunner rrClient;

    /**
     * Updates IoT RoboRunner Worker Status with new information.
     *
     * @param workerArn the unique identifier of the worker that needs the update
     * @param status the new status of the worker
     */
    public void updateRoboRunnerWorkerStatus(@NonNull final String workerArn, @NonNull final WorkerStatus status) {
        final UpdateWorkerRequest request = new UpdateWorkerRequest()
                .withId(workerArn)
                .withPosition(status.getPosition())
                .withOrientation(status.getOrientation())
                .withVendorProperties(status.getVendorProperties())
                .withAdditionalTransientProperties(status.getWorkerAdditionalTransientProperties());

        try {
            rrClient.updateWorker(request);
            log.debug(String.format("Updated RoboRunner Worker Status for ARN: %s", workerArn));
        } catch (SdkBaseException ex) {
            log.error("Exception received while updating RoboRunner worker: " + workerArn);
            log.error(ex.getMessage());
            throw ex;
        }
    }

    /**
     * Updates IoT RoboRunner worker with new additional transient properties.
     *
     * @param workerArn the unique identifier of the worker that needs the update
     * @param workerAdditionalTransientProperties the new additional transient properties the worker needs to have
     * @return the result of the update request to IoT RoboRunner
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public UpdateWorkerResult updateRoboRunnerWorkerAdditionalTransientProperties(
            @NonNull final String workerArn,
            @NonNull final WorkerAdditionalTransientProperties workerAdditionalTransientProperties)
            throws JsonProcessingException {
        return rrClient.updateWorker(new UpdateWorkerRequest()
                .withId(workerArn)
                .withAdditionalTransientProperties(
                        OBJECT_MAPPER.writeValueAsString(workerAdditionalTransientProperties)));
    }

    /**
     * Gets additional fixed properties of a worker fleet.
     *
     * @param workerFleetArn the unique identifier of the worker fleet
     * @return the additional fixed properties JSON object
     */
    public Optional<WorkerFleetAdditionalFixedProperties> getWorkerFleetAdditionalFixedProperties(
            @NonNull final String workerFleetArn) {
        final GetWorkerFleetRequest request = new GetWorkerFleetRequest().withId(workerFleetArn);
        final GetWorkerFleetResult result = rrClient.getWorkerFleet(request);

        final String additionalProperties = result.getAdditionalFixedProperties();

        if (additionalProperties != null) {
            try {
                return Optional.of(OBJECT_MAPPER.readValue(
                    additionalProperties, WorkerFleetAdditionalFixedProperties.class));
            } catch (final JsonProcessingException ex) {
                log.warn(String.format("Failed to read WorkerFleet additionalFixedProperties: %s", ex.getMessage()));
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Creates a map representing the vendor worker id to IoT RoboRunner worker arn mapping.
     *
     * @param siteArn the unique identifier of the site
     * @param fleetArn the unique identifier of the worker fleet
     * @return the map containing vendor worker id to worker arn mappings
     */
    public Map<String, String> createRobotIdToWorkerArnMap(@NonNull final String siteArn,
            @NonNull final String fleetArn) {
        if (StringUtils.isBlank(siteArn)) {
            throw new IllegalArgumentException("The siteArn cannot be null or empty when listing workers");
        }
        if (StringUtils.isBlank(fleetArn)) {
            throw new IllegalArgumentException("The fleetArn cannot be null or empty when listing workers");
        }

        final Map<String, String> robotIdToWorkerArnMap = new ConcurrentHashMap<>();
        final List<Worker> workers = this.getWorkersInWorkerFleet(siteArn, fleetArn);

        for (final Worker worker : workers) {
            robotIdToWorkerArnMap.put(worker.getVendorProperties().getVendorWorkerId(), worker.getArn());
        }
        return robotIdToWorkerArnMap;
    }

    /**
     * Creates a map representing the IoT RoboRunner worker arn to vendor worker arn mapping.
     *
     * @param siteArn the unique identifier of the site
     * @param fleetArn the unique identifier of the worker fleet
     * @return the map containing worker arn to vendor worker id mappings
     */
    public Map<String, String> createWorkerArnToRobotIpAddressMap(@NonNull final String siteArn,
            @NonNull final String fleetArn) {
        if (StringUtils.isBlank(siteArn)) {
            throw new IllegalArgumentException("The siteArn cannot be null or empty when listing workers");
        }
        if (StringUtils.isBlank(fleetArn)) {
            throw new IllegalArgumentException("The fleetArn cannot be null or empty when listing workers");
        }

        final Map<String, String> workerArnToRobotIpAddressMap = new ConcurrentHashMap<>();
        final List<Worker> workers = this.getWorkersInWorkerFleet(siteArn, fleetArn);

        for (final Worker worker : workers) {
            workerArnToRobotIpAddressMap.put(worker.getArn(), worker.getVendorProperties().getVendorWorkerIpAddress());
        }
        return workerArnToRobotIpAddressMap;
    }

    /**
     * Lists workers for the given site and worker arn.
     *
     * @param siteArn the unique identifier of the site
     * @param fleetArn the unique identifier of the worker fleet
     * @return the list of workers belonging to a given site and worker fleet
     */
    public List<Worker> getWorkersInWorkerFleet(@NonNull final String siteArn, @NonNull final String fleetArn) {
        final ListWorkersRequest request = new ListWorkersRequest().withSite(siteArn).withFleet(fleetArn);

        ListWorkersResult listWorkerResult = rrClient.listWorkers(request);
        final List<Worker> workers = listWorkerResult.getWorkers();

        String nextToken = listWorkerResult.getNextToken();
        while (nextToken != null) {
            listWorkerResult = rrClient.listWorkers(request.withNextToken(nextToken));
            workers.addAll(listWorkerResult.getWorkers());
            nextToken = listWorkerResult.getNextToken();
        }

        return workers;
    }
}
