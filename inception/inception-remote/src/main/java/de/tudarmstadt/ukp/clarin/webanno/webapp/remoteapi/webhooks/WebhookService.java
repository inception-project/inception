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

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.clarin.webanno.api.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.AnnotationStateChangeMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.DocumentStateChangeMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.ProjectStateChangeMessage;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#webhookService}.
 * </p>
 */
public class WebhookService
    implements InitializingBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String X_AERO_NOTIFICATION = "X-AERO-Notification";
    public static final String X_AERO_SIGNATURE = "X-AERO-Signature";

    private static final Map<Class<? extends ApplicationEvent>, String> EVENT_TOPICS;

    public static final String DOCUMENT_STATE = "DOCUMENT_STATE";
    public static final String ANNOTATION_STATE = "ANNOTATION_STATE";
    public static final String PROJECT_STATE = "PROJECT_STATE";

    static {
        Map<Class<? extends ApplicationEvent>, String> names = new HashMap<>();
        names.put(ProjectStateChangedEvent.class, PROJECT_STATE);
        names.put(DocumentStateChangedEvent.class, DOCUMENT_STATE);
        names.put(AnnotationStateChangeEvent.class, ANNOTATION_STATE);
        EVENT_TOPICS = Collections.unmodifiableMap(names);
    }

    private final WebhooksConfiguration configuration;
    private final RestTemplateBuilder restTemplateBuilder;

    private HttpComponentsClientHttpRequestFactory nonValidatingRequestFactory = null;

    public WebhookService(WebhooksConfiguration aConfiguration,
            RestTemplateBuilder aRestTemplateBuilder)
        throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException
    {
        configuration = aConfiguration;
        restTemplateBuilder = aRestTemplateBuilder;

        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy).build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();

        nonValidatingRequestFactory = new HttpComponentsClientHttpRequestFactory();
        nonValidatingRequestFactory.setHttpClient(httpClient);
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        init();
    }

    public void init()
    {
        if (!configuration.getGlobalHooks().isEmpty()) {
            log.info("Global webhooks registered:");
            for (Webhook hook : configuration.getGlobalHooks()) {
                log.info("- " + hook);
            }
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Async
    public void onApplicationEvent(ApplicationEvent aEvent)
    {
        String topic = EVENT_TOPICS.get(aEvent.getClass());
        if (topic == null) {
            return;
        }

        Object message;
        switch (topic) {
        case PROJECT_STATE:
            message = new ProjectStateChangeMessage((ProjectStateChangedEvent) aEvent);
            break;
        case DOCUMENT_STATE:
            message = new DocumentStateChangeMessage((DocumentStateChangedEvent) aEvent);
            break;
        case ANNOTATION_STATE:
            message = new AnnotationStateChangeMessage((AnnotationStateChangeEvent) aEvent);
            break;
        default:
            return;
        }

        dispatch(topic, message);
    }

    private void dispatch(String topic, Object message)
    {
        var relevantHooks = configuration.getGlobalHooks().stream() //
                .filter(Webhook::isEnabled) //
                .filter(h -> h.getTopics().contains(topic)) //
                .map(h -> Pair.of(h, new AtomicInteger(0))) //
                .collect(toList());

        while (!relevantHooks.isEmpty()) {
            var i = relevantHooks.iterator();

            while (i.hasNext()) {
                var hookAndCount = i.next();

                try {
                    sendNotification(topic, message, hookAndCount.getKey());
                    i.remove();
                }
                catch (Exception e) {
                    if (hookAndCount.getValue().get() < configuration.getRetryCount()) {
                        if (log.isDebugEnabled()) {
                            log.error("Unable to send webhook [{}]: {} - retrying in a moment",
                                    hookAndCount.getKey().getUrl(), e.getMessage(), e);
                        }
                        else {
                            log.error("Unable to send webhook [{}]: {} - retrying in a moment",
                                    hookAndCount.getKey().getUrl(), e.getMessage());
                        }
                        hookAndCount.getValue().incrementAndGet();
                    }
                    else {
                        if (log.isDebugEnabled()) {
                            log.error("Unable to invoke webhook [{}]: {} - giving up",
                                    hookAndCount.getKey().getUrl(), e.getMessage(), e);
                        }
                        else {
                            log.error("Unable to invoke webhook [{}]: {} - giving up",
                                    hookAndCount.getKey().getUrl(), e.getMessage());
                        }
                        i.remove();
                    }
                }
            }

            try {
                Thread.sleep(configuration.getRetryDelay());
            }
            catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    private void sendNotification(String topic, Object message, Webhook hook) throws IOException
    {
        log.trace("Sending webhook message on topic [{}] to [{}]", topic, hook.getUrl());

        // Configure rest template without SSL certification check if that is disabled.
        RestTemplate restTemplate;
        if (hook.isVerifyCertificates()) {
            restTemplate = restTemplateBuilder.build();
        }
        else {
            restTemplate = restTemplateBuilder.requestFactory(this::getNonValidatingRequestFactory)
                    .build();
        }

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.set(X_AERO_NOTIFICATION, topic);

        // If a secret is set, then add a digest header that allows the client to verify
        // the message integrity
        String json = JSONUtil.toJsonString(message);
        if (isNotBlank(hook.getSecret())) {
            String digest = DigestUtils.shaHex(hook.getSecret() + json);
            requestHeaders.set(X_AERO_SIGNATURE, digest);
        }

        if (isNotBlank(hook.getAuthHeader()) && isNotBlank(hook.getAuthHeaderValue())) {
            requestHeaders.set(hook.getAuthHeader(), hook.getAuthHeaderValue());
        }

        HttpEntity<?> httpEntity = new HttpEntity<Object>(json, requestHeaders);
        restTemplate.postForEntity(hook.getUrl(), httpEntity, Void.class);
    }

    private HttpComponentsClientHttpRequestFactory getNonValidatingRequestFactory()
    {
        return nonValidatingRequestFactory;
    }
}
