/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.support.test.http;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.net.ssl.SSLHandshakeException;

public class HttpTestUtils
{
    public static boolean checkURL(String urlString)
    {
        var httpClient = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder().uri(URI.create(urlString)).build();

        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            var statusCode = response.statusCode();
            return statusCode >= 200 && statusCode < 300;
        }
        catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static final Set<String> UNREACHABLE_ENDPOINTS = new LinkedHashSet<>();

    public static boolean isReachable(String aUrl)
    {
        if (UNREACHABLE_ENDPOINTS.contains(aUrl)) {
            return false;
        }

        try {
            var url = new URL(aUrl);
            var con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(7_500);
            con.setReadTimeout(7_500);
            con.setRequestProperty("Content-Type", "*/*-query");
            var status = con.getResponseCode();

            if (status == HTTP_MOVED_TEMP || status == HTTP_MOVED_PERM) {
                String location = con.getHeaderField("Location");
                return isReachable(location);
            }

            if (status >= 200 && status < 300) {
                return true;
            }

            UNREACHABLE_ENDPOINTS.add(aUrl);
            return false;
        }
        catch (SSLHandshakeException e) {
            // We ignore SSL handshake exceptions here because they usually indicate that the server
            // is there but that there is a certificate problem (e.g. self-signed certificate)
            return true;
        }
        catch (Exception e) {
            System.out.printf("[%s] Network-level check: %s%n", aUrl, e.getMessage());
            UNREACHABLE_ENDPOINTS.add(aUrl);
            return false;
        }
    }

    /**
     * Tries to connect to the given endpoint url and assumes that the connection is successful with
     * {@link org.junit.jupiter.api.Assumptions#assumeTrue(boolean, String)}
     * 
     * @param aEndpointURL
     *            the url to check
     */
    public static void assumeEndpointIsAvailable(String aEndpointURL)
    {
        assumeTrue(isReachable(aEndpointURL),
                "Remote repository at [" + aEndpointURL + "] is not reachable");
    }
}
