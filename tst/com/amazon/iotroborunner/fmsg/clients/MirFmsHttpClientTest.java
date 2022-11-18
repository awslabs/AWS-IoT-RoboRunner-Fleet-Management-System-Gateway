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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;
import com.amazon.iotroborunner.fmsg.types.FmsHttpRequest;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the MiR FMS HTTP client module. */
@ExtendWith(MockitoExtension.class)
public class MirFmsHttpClientTest {
    private MirFmsHttpClient client;

    private FmsHttpRequest fmsRequest;

    @Mock
    private HttpClient mockHttpClient;

    /** Set up mocks required for in the tests. */
    @BeforeEach
    public void setup() {
        try (
             MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            httpClientStatic.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

            client = new MirFmsHttpClient(TestConstants.VENDOR_API_ENDPOINT, TestConstants.Secret.MIR_SECRET.name);
            fmsRequest = new FmsHttpRequest(TestConstants.HTTP_GET_REQUEST_METHOD,
                TestConstants.VENDOR_API_ROBOT_STATUS_REQUEST, TestConstants.EMPTY_HTTP_PAYLOAD);
        }
    }

    @Test
    public void given_validParameters_when_constructed_then_passes() {
        assertNotNull(client);
    }

    @Test
    public void given_nullEndpoint_when_constructed_then_throwsNullPointerException() {
        try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            httpClientStatic.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

            assertThrows(NullPointerException.class, () -> {
                new MirFmsHttpClient(null, TestConstants.Secret.MIR_SECRET.name);
            });
        }
    }

    @Test
    public void given_nullAuthSecret_when_constructed_then_throwsNullPointerException() {
        try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            httpClientStatic.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

            assertThrows(NullPointerException.class, () -> {
                new MirFmsHttpClient(TestConstants.VENDOR_API_ENDPOINT, null);
            });
        }
    }

    @Test
    public void given_nullFmsRequest_when_sendFmsRequest_then_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            this.client.sendFmsRequest(null);
        });
    }

    @Test
    public void given_nullFmsRequestMethod_when_sendFmsRequest_then_throwsNullPointerException() {
        final FmsHttpRequest request = new FmsHttpRequest(null, TestConstants.VENDOR_API_ROBOT_STATUS_REQUEST,
                TestConstants.EMPTY_HTTP_PAYLOAD);

        assertThrows(NullPointerException.class, () -> {
            this.client.sendFmsRequest(request);
        });
    }

    @Test
    public void given_nullFmsRequestEndpoint_when_sendFmsRequest_then_throwsNullPointerException() {
        final FmsHttpRequest request =
                new FmsHttpRequest(TestConstants.HTTP_GET_REQUEST_METHOD, null, TestConstants.EMPTY_HTTP_PAYLOAD);

        assertThrows(NullPointerException.class, () -> {
            this.client.sendFmsRequest(request);
        });
    }

    @Test
    public void given_nullFmsRequestPayload_when_sendFmsRequest_then_throwsNullPointerException() {
        final FmsHttpRequest request = new FmsHttpRequest(TestConstants.HTTP_GET_REQUEST_METHOD,
                TestConstants.VENDOR_API_ROBOT_STATUS_REQUEST, null);

        assertThrows(NullPointerException.class, () -> {
            this.client.sendFmsRequest(request);
        });
    }

    @Test
    public void given_httpRequestThrowsIllegalArgumentException_when_sendFmsRequest_then_returnsNull() {
        try (MockedStatic<HttpRequest> builder = mockStatic(HttpRequest.class)) {
            builder.when(HttpRequest::newBuilder).thenThrow(IllegalArgumentException.class);

            final String response = this.client.sendFmsRequest(this.fmsRequest);

            assertNull(response);
        }
    }

    @Test
    public void given_httpClientThrowsIoException_when_sendFmsRequestCalled_then_nullResponseReturned()
            throws Exception {
        when(mockHttpClient.send(any(), any())).thenThrow(IOException.class);

        final String response = this.client.sendFmsRequest(this.fmsRequest);

        assertNull(response);
    }

    @Test
    public void given_httpClientSendThrowsInterruptedException_when_sendFmsRequest_then_returnsNull()
            throws Exception {
        when(mockHttpClient.send(any(), any())).thenThrow(InterruptedException.class);

        final String response = this.client.sendFmsRequest(this.fmsRequest);

        assertNull(response);
    }

    @Test
    public void given_httpClientSendThrowsIllegalArgumentException_when_sendFmsRequest_then_returnsNull()
            throws Exception {
        when(mockHttpClient.send(any(), any())).thenThrow(IllegalArgumentException.class);

        final String response = this.client.sendFmsRequest(this.fmsRequest);

        assertNull(response);
    }

    @Test
    public void given_httpClientSendThrowsSecurityException_when_sendFmsRequest_then_returnsNull()
            throws Exception {
        when(mockHttpClient.send(any(), any())).thenThrow(SecurityException.class);

        final String response = this.client.sendFmsRequest(this.fmsRequest);

        assertNull(response);
    }

    @Test
    public void given_httpClientSendsErrorResponseCode_when_sendFmsRequestCalled_then_nullResponseReturned()
            throws Exception {
        final HttpResponse mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

        final String response = this.client.sendFmsRequest(this.fmsRequest);

        assertNull(response);
    }

    @Test
    public void given_validFmsHttpRequest_when_sendFmsRequestCalled_then_validResponseReturned() throws Exception {
        final HttpResponse mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("test");
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

        final String response = this.client.sendFmsRequest(this.fmsRequest);

        assertEquals("test", response);
    }
}
