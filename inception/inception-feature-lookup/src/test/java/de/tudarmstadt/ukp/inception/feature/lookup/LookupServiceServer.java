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
package de.tudarmstadt.ukp.inception.feature.lookup;

import static de.tudarmstadt.ukp.inception.feature.lookup.LookupServiceImpl.PARAM_ID;
import static de.tudarmstadt.ukp.inception.feature.lookup.LookupServiceImpl.PARAM_LIMIT;
import static de.tudarmstadt.ukp.inception.feature.lookup.LookupServiceImpl.PARAM_QUERY;
import static de.tudarmstadt.ukp.inception.feature.lookup.LookupServiceImpl.PARAM_QUERY_CONTEXT;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.ExceptionListener;
import org.apache.hc.core5.http.HttpConnection;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.impl.bootstrap.ServerBootstrap;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.TimeValue;

public class LookupServiceServer
{
    private static final int PORT = 8888;

    public static void main(String[] args) throws Exception
    {
        var server = ServerBootstrap.bootstrap() //
                .setListenerPort(PORT) //
                .setExceptionListener(makeExceptionListener()) //
                .register("*", makeRequestHandler()) //
                .create();

        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.close(CloseMode.GRACEFUL)));
        System.out.println("Listening on port " + PORT);

        server.awaitTermination(TimeValue.MAX_VALUE);
    }

    private static LookupEntry handleLookup(String aId)
    {
        try {
            int id = Integer.valueOf(aId);
            return new LookupEntry(aId, "Item :" + id, "Description " + id);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<LookupEntry> handleQuery(String aQuery, String aQueryContext, int aLimit)
    {
        System.out.printf("Query:[%s] context:[%s] limit:[%d]%n", aQuery, aQueryContext, aLimit);

        if ("error".equals(aQuery)) {
            System.out.println("Generating error as requested");
            throw new IllegalStateException("Error requested");
        }

        List<LookupEntry> result = new ArrayList<>();
        for (int i = 0; i < aLimit; i++) {
            result.add(new LookupEntry(String.valueOf(i), //
                    "Item " + i + ": " + aQuery + " (" + aQueryContext + ")", //
                    "Description " + i + ": " + aQuery));
        }

        return result;
    }

    private static HttpRequestHandler makeRequestHandler()
    {
        return new HttpRequestHandler()
        {
            @Override
            public void handle(ClassicHttpRequest aRequest, ClassicHttpResponse aResponse,
                    HttpContext aContext)
                throws HttpException, IOException
            {
                if (!"GET".equals(aRequest.getMethod())) {
                    throw new HttpException("Unsupported method: " + aRequest.getMethod());
                }

                System.out.printf("Headers: %s%n", asList(aRequest.getHeaders()));

                try {
                    URIBuilder uri = new URIBuilder(aRequest.getRequestUri());
                    var params = uri.getQueryParams().stream()
                            .collect(toMap(NameValuePair::getName, NameValuePair::getValue));

                    Object response = null;
                    if (params.containsKey(PARAM_ID)) {
                        response = handleLookup(params.get("id"));
                    }

                    if (params.containsKey(PARAM_QUERY)) {
                        response = handleQuery(params.get(PARAM_QUERY),
                                params.get(PARAM_QUERY_CONTEXT),
                                Integer.valueOf(params.get(PARAM_LIMIT)));
                    }

                    if (response == null) {
                        aResponse.setCode(404);
                        return;
                    }

                    aResponse.setCode(200);
                    aResponse.setHeader(HttpHeaders.CONTENT_TYPE, response);
                    aResponse.setEntity(new StringEntity(toJsonString(response), UTF_8));
                }
                catch (URISyntaxException e) {
                    throw new IOException(e);
                }
            }
        };
    }

    private static ExceptionListener makeExceptionListener()
    {
        return new ExceptionListener()
        {

            @Override
            public void onError(final Exception ex)
            {
                ex.printStackTrace();
            }

            @Override
            public void onError(final HttpConnection conn, final Exception ex)
            {
                if (ex instanceof SocketTimeoutException) {
                    System.err.println("Connection timed out");
                }
                else if (ex instanceof ConnectionClosedException) {
                    System.err.println(ex.getMessage());
                }
                else {
                    ex.printStackTrace();
                }
            }
        };
    }
}
