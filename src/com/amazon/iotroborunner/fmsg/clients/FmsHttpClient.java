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

package com.amazon.iotroborunner.fmsg.clients;

import com.amazon.iotroborunner.fmsg.types.FmsHttpRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Abstract base class for implementing robot fleet specific FMS clients.
 */
@Log4j2
public abstract class FmsHttpClient {
    /**
     * The httpClient used to contact the FMS.
     */
    protected final HttpClient httpClient;

    /**
     * The FMS HTTP endpoint.
     */
    protected final String endpoint;

    /**
     * The authorization secret used for authentication/authorization for FMS requests.
     */
    protected final String apiAuthSecret;


    FmsHttpClient(final HttpClient httpClient, final String endpoint, final String apiAuthSecret) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.apiAuthSecret = apiAuthSecret;
    }

    /**
     * Build the HttpRequest for the given method, command, and payload.
     *
     * @param method  The HTTP method
     * @param command The command to send to the FMS API
     * @param payload The payload for the API request
     * @return        The HttpRequest object used to send to the FMS
     */
    protected HttpRequest buildRequest(@NonNull final String method, @NonNull final String command,
                                       @NonNull final String payload) {
        final String uri = this.endpoint + command;
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(uri).normalize())
                                    .method(method, HttpRequest.BodyPublishers.ofString(payload))
                                    .header("Content-Type", "application/json")
                                    .header("Accepted-Language", "en_US")
                                    .header("authorization", this.apiAuthSecret)
                                    .build();
            return request;
        } catch (IllegalArgumentException ex) {
            log.error("Illegal argument when sending request: " + method, uri);
            return null;
        }
    }

    /**
     * Method to send an API request to an FMS.
     *
     * @param request The HttpRequest object to send to the FMS
     * @return        The string body of the FMS response
     */
    protected String sendRequest(final HttpRequest request) {
        HttpResponse<String> response = null;
        String responseBody = null;

        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException | IllegalArgumentException | SecurityException e) {
            log.error(String.format("Exception received when sending request to %s FMS: %s",
                                    request.uri().toString(), e.getMessage()));
        }

        if (response != null && response.statusCode() < 300 && response.statusCode() >= 200) {
            responseBody = response.body();
        }

        return responseBody;
    }

    /**
     * Generic method to send a request to the FMS.
     *
     * @param fmsRequest The FmsHttpRequest containing values for the HTTP request to the FMS
     * @return           The string body of the FMS response
     */
    public String sendFmsRequest(@NonNull final FmsHttpRequest fmsRequest) {
        final HttpRequest request = buildRequest(fmsRequest.getMethod(), fmsRequest.getEndpoint(),
            fmsRequest.getPayload());

        if (request != null) {
            return sendRequest(request);
        }

        return null;
    }
}
