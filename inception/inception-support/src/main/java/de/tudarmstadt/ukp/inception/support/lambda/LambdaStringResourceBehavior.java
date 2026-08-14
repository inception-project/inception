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
package de.tudarmstadt.ukp.inception.support.lambda;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.wicket.util.lang.Args.notNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.danekja.java.util.function.serializable.SerializableSupplier;

/**
 * Mounts a callback URL on a component which serves a plain text resource - e.g. JSON to be fetched
 * by client-side code. Unlike {@link LambdaAjaxBehavior}, this does not use the Wicket Ajax
 * protocol and thus does not respond with a Wicket Ajax response envelope, but rather with the
 * response body produced by the supplier.
 */
public class LambdaStringResourceBehavior
    extends AbstractAjaxBehavior
{
    private static final long serialVersionUID = -5089570873534856741L;

    private final String contentType;
    private final String encoding;
    private final SerializableSupplier<String> supplier;

    /**
     * Serves the value provided by the given supplier as UTF-8 encoded JSON.
     *
     * @param aSupplier
     *            provides the response body.
     */
    public LambdaStringResourceBehavior(SerializableSupplier<String> aSupplier)
    {
        this(APPLICATION_JSON_VALUE, aSupplier);
    }

    /**
     * Serves the value provided by the given supplier as UTF-8 encoded text of the given content
     * type.
     *
     * @param aContentType
     *            the content type of the response.
     * @param aSupplier
     *            provides the response body.
     */
    public LambdaStringResourceBehavior(String aContentType, SerializableSupplier<String> aSupplier)
    {
        this(aContentType, UTF_8.name(), aSupplier);
    }

    /**
     * Serves the value provided by the given supplier as text of the given content type and
     * encoding.
     *
     * @param aContentType
     *            the content type of the response.
     * @param aEncoding
     *            the encoding of the response - may be {@code null}.
     * @param aSupplier
     *            provides the response body.
     */
    public LambdaStringResourceBehavior(String aContentType, String aEncoding,
            SerializableSupplier<String> aSupplier)
    {
        contentType = notNull(aContentType, "aContentType");
        encoding = aEncoding;
        supplier = notNull(aSupplier, "aSupplier");
    }

    @Override
    public void onRequest()
    {
        getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(
                new TextRequestHandler(contentType, encoding, supplier.get()));
    }
}
