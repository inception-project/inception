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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * <p>
 * This class is loaded via {@link RemoteApiAutoConfiguration}.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = WebhooksConfiguration.PROPERTY_PREFIX)
public class WebhooksConfiguration
{
    public static final String PROPERTY_PREFIX = "webhooks";

    /**
     * List of globally configured webhooks. Each webhook specifies an URL and a set of topics about
     * which the remote service listening at the given URL is notified.
     * <p>
     * Supported topics:
     * <ul>
     * <li>{@code DOCUMENT_STATE} - events related to the change of a document state such as when
     * any user starts annotating or curating the document.</li>
     * <li>{@code ANNOTATION_STATE} - events related to the change of an annotation state such as
     * when a user starts or completes the annotation of a document.</li>
     * <li>{@code PROJECT_STATE} - events related to the change of an entire project such as when
     * all documents have been curated.</li>
     * </ul>
     */
    private List<Webhook> globalHooks = new ArrayList<>();

    /**
     * Delay in milliseconds between retry attempts when delivering a webhook notification fails.
     * Allowed range is {@code 10} to {@code 5000}.
     */
    @Min(value = 10)
    @Max(value = 5000)
    private int retryDelay = 1000;

    /**
     * Number of additional delivery attempts to make when a webhook notification cannot be
     * delivered. By default, only one delivery attempt is made ({@code 0} retries). Up to {@code 3}
     * additional attempts can be configured.
     */
    @Min(value = 0)
    @Max(value = 3)
    private int retryCount = 0;

    public List<Webhook> getGlobalHooks()
    {
        return globalHooks;
    }

    public void setGlobalHooks(List<Webhook> aWebhooks)
    {
        globalHooks = aWebhooks;
    }

    public int getRetryCount()
    {
        return retryCount;
    }

    public void setRetryCount(int aRetryCount)
    {
        retryCount = aRetryCount;
    }

    public int getRetryDelay()
    {
        return retryDelay;
    }

    public void setRetryDelay(int aRetryDelay)
    {
        retryDelay = aRetryDelay;
    }
}
