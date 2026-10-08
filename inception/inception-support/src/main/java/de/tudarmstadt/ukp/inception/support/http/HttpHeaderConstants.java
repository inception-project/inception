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
package de.tudarmstadt.ukp.inception.support.http;

/**
 * Names and values of HTTP headers for which the Spring and Wicket constants do not exist.
 */
public final class HttpHeaderConstants
{
    public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    /**
     * Value for {@link #X_CONTENT_TYPE_OPTIONS}: the browser must trust the declared content type
     * and not guess one from the content.
     */
    public static final String NOSNIFF = "nosniff";

    private HttpHeaderConstants()
    {
        // No instances
    }
}
