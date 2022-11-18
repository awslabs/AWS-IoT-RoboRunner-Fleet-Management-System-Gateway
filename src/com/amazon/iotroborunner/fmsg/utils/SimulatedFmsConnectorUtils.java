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

import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.SHARED_SPACE_ARN;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.WORKER_LOCATION_STATUS;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerLocationStatus.OUT_OF_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerLocationStatus.WAITING_FOR_SHARED_SPACE;

import com.amazon.iotroborunner.fmsg.types.roborunner.WorkerAdditionalTransientProperties;

import java.util.Map;

import com.amazonaws.services.iotroborunner.model.Worker;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Utilities designed to be used by Simulated FMS Connectors.
 */
@Log4j2
public final class SimulatedFmsConnectorUtils {
    /**
     * Checks if worker is waiting for a shared space.
     *
     * @param  workerLocationStatus the status of worker with respect to any shared space
     *                              (waiting for, in, or out of shared space)
     * @return true if the worker is waiting for a shared space, false otherwise
     */
    public static boolean isWorkerWaitingForSharedSpace(@NonNull final String workerLocationStatus) {
        return workerLocationStatus.equals(WAITING_FOR_SHARED_SPACE.value);
    }

    /**
     * Checks if worker is out of a shared space.
     *
     * @param  workerLocationStatus the status of worker with respect to any shared space
     *                              (waiting for, in, or out of shared space)
     * @return true if the worker is out of a shared space, false otherwise
     */
    public static boolean isWorkerOutOfSharedSpace(@NonNull final String workerLocationStatus) {
        return workerLocationStatus.equals(OUT_OF_SHARED_SPACE.value);
    }

    /**
     * Returns the information about the worker's whereabouts related to any shared
     * space it might be interacting with.
     *
     * @param worker the worker from which to extract the information
     * @return a pair of location status and related shared space arn if the worker
     *         has needed custom properties, and null otherwise
     */
    public static Pair<String, String> getWorkerLocationStatusToSharedSpaceArnMapping(@NonNull final Worker worker) {
        try {
            if (!StringUtils.isEmpty(worker.getAdditionalTransientProperties())) {
                final WorkerAdditionalTransientProperties workerAdditionalTransientProperties =
                        WorkerAdditionalTransientProperties.readWorkerAdditionalTransientProperties(
                                worker.getAdditionalTransientProperties());


                final Map<String, String> customTransientProperties = workerAdditionalTransientProperties
                        .getCustomTransientProperties();

                if (!MapUtils.isEmpty(customTransientProperties)
                        && !StringUtils.isEmpty(customTransientProperties.get(WORKER_LOCATION_STATUS.value))
                        && !StringUtils.isEmpty(customTransientProperties.get(SHARED_SPACE_ARN.value))) {

                    return ImmutablePair.of(customTransientProperties.get(WORKER_LOCATION_STATUS.value),
                            customTransientProperties.get(SHARED_SPACE_ARN.value));
                }
            }
        } catch (final JsonProcessingException e) {
            log.debug("Can't acquire required by the connector properties for worker {}. Skipping",
                    worker.getArn());
        }
        return null;
    }

    /**
     * Hidden Constructor.
     */
    private SimulatedFmsConnectorUtils() {
        throw new UnsupportedOperationException("This class is for holding utilities and should not be instantiated");
    }
}
