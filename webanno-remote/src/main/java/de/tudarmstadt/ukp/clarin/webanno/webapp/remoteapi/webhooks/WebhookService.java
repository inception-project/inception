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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.clarin.webanno.api.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.AnnotationStateChangeMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.DocumentStateChangeMessage;

@Component
public class WebhookService
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String X_AERO_NOTIFICATION = "X-AERO-Notification";

    private static final Map<Class<? extends ApplicationEvent>, String> EVENT_TOPICS;
    
    public static final String DOCUMENT_STATE = "DOCUMENT_STATE";
    public static final String ANNOTATION_STATE = "ANNOTATION_STATE";
    
    static {
        Map<Class<? extends ApplicationEvent>, String> names = new HashMap<>();
        names.put(DocumentStateChangedEvent.class, DOCUMENT_STATE);
        names.put(AnnotationStateChangeEvent.class, ANNOTATION_STATE);
        EVENT_TOPICS = Collections.unmodifiableMap(names);
    }
    
    private @Resource WebhooksConfiguration configuration;
    private @Resource RestTemplateBuilder restTemplateBuilder;
    
    @PostConstruct
    public void init()
    {
        if (!configuration.getGlobalHooks().isEmpty()) {
            log.info("Global webhooks registered:");
            for (WebhookConfiguration hook : configuration.getGlobalHooks()) {
                log.info("- " + hook);
            }
        }
    }
    
    @EventListener
    @Async
    public void onApplicationEvent(ApplicationEvent aEvent)
    {
        String topic = EVENT_TOPICS.get(aEvent.getClass());
        if (topic == null) {
            return;
        }
        
        Object message;
        switch (topic) {
        case DOCUMENT_STATE:
            message = new DocumentStateChangeMessage((DocumentStateChangedEvent) aEvent);
            break;
        case ANNOTATION_STATE:
            message = new AnnotationStateChangeMessage((AnnotationStateChangeEvent) aEvent);
            break;
        default:
            return;
        }
        
        RestTemplate restTemplate = restTemplateBuilder.build();
        
        for (WebhookConfiguration hook : configuration.getGlobalHooks()) {
            if (!hook.isEnabled() || !hook.getTopics().contains(topic)) {
                continue;
            }
            
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
            requestHeaders.set(X_AERO_NOTIFICATION, topic);

            HttpEntity<?> httpEntity = new HttpEntity<Object>(message, requestHeaders);
            
            restTemplate.postForEntity(hook.getUrl(), httpEntity, Void.class);
        }
    }
}
