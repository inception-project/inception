/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.clarin.webanno.api.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.AnnotationStateChangeMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.DocumentStateChangeMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.ProjectStateChangeMessage;

@Component
public class WebhookService implements InitializingBean
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
    
    private @Autowired WebhooksConfiguration configuration;
    private @Autowired RestTemplateBuilder restTemplateBuilder;

    private HttpComponentsClientHttpRequestFactory nonValidatingRequestFactory = null;
    
    public WebhookService()
        throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException
    {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain,
                String authType) -> true;

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
        
        for (Webhook hook : configuration.getGlobalHooks()) {
            if (!hook.isEnabled() || !hook.getTopics().contains(topic)) {
                continue;
            }

            try {
                // Configure rest template without SSL certification check if that is disabled.
                RestTemplate restTemplate;
                if (hook.isVerifyCertificates()) {
                    restTemplate = restTemplateBuilder.build();
                }
                else {
                    restTemplate = restTemplateBuilder
                            .requestFactory(this::getNonValidatingRequestFactory).build();
                }
                
                HttpHeaders requestHeaders = new HttpHeaders();
                requestHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
                requestHeaders.set(X_AERO_NOTIFICATION, topic);
                
                // If a secret is set, then add a digest header that allows the client to verify
                // the message integrity
                String json = JSONUtil.toJsonString(message);
                if (isNotBlank(hook.getSecret())) {
                    String digest = DigestUtils.shaHex(hook.getSecret() + json);
                    requestHeaders.set(X_AERO_SIGNATURE, digest);
                }
    
                HttpEntity<?> httpEntity = new HttpEntity<Object>(json, requestHeaders);
                restTemplate.postForEntity(hook.getUrl(), httpEntity, Void.class);
            }
            catch (Exception e) {
                log.error("Unable to invoke webhook [{}]", hook, e);
            }
        }
    }
    
    private HttpComponentsClientHttpRequestFactory getNonValidatingRequestFactory()
    {
        return nonValidatingRequestFactory;
    }
}
