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

import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.ClassUtils.getAbbreviatedName;

import java.lang.invoke.MethodHandles;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.AnnotationStateChangeMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.DocumentStateChangeMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.ProjectStateChangeMessage;
import de.tudarmstadt.ukp.inception.documents.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.inception.documents.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.ProjectStateChangedEvent;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#webhookService}.
 * </p>
 */
public class WebhookService
    implements InitializingBean
{
    final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Map<Class<? extends ApplicationEvent>, String> EVENT_TOPICS;

    public static final String DOCUMENT_STATE = "DOCUMENT_STATE";
    public static final String ANNOTATION_STATE = "ANNOTATION_STATE";
    public static final String PROJECT_STATE = "PROJECT_STATE";

    static {
        var names = new HashMap<Class<? extends ApplicationEvent>, String>();
        names.put(ProjectStateChangedEvent.class, PROJECT_STATE);
        names.put(DocumentStateChangedEvent.class, DOCUMENT_STATE);
        names.put(AnnotationStateChangeEvent.class, ANNOTATION_STATE);
        EVENT_TOPICS = unmodifiableMap(names);
    }

    private final WebhooksConfiguration configuration;
    private final List<WebhookDriver> extensionsListProxy;
    private List<WebhookDriver> extensionsList;

    public WebhookService(WebhooksConfiguration aConfiguration,
            @Lazy @Autowired(required = false) List<WebhookDriver> aExtensions)
        throws GeneralSecurityException

    {
        configuration = aConfiguration;
        extensionsListProxy = aExtensions;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        init();
    }

    public void init()
    {
        var extensions = new ArrayList<WebhookDriver>();

        if (extensionsListProxy != null) {
            extensions.addAll(extensionsListProxy);
            extensions.sort(AnnotationAwareOrderComparator.INSTANCE);

            for (var fs : extensions) {
                LOG.debug("Found {} extension: {}", getClass().getSimpleName(),
                        getAbbreviatedName(fs.getClass(), 20));
            }
        }

        BOOT_LOG.info("Found [{}] {} extensions", extensions.size(), getClass().getSimpleName());

        extensionsList = unmodifiableList(extensions);

        if (!configuration.getGlobalHooks().isEmpty()) {
            LOG.info("Global webhooks registered:");
            for (var hook : configuration.getGlobalHooks()) {
                LOG.info("- " + hook);
            }
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Async
    public void onApplicationEvent(ApplicationEvent aEvent)
    {
        var topic = EVENT_TOPICS.get(aEvent.getClass());
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

    public WebhookDriver getDriver(Webhook aHook)
    {
        return extensionsList.stream() //
                .filter(d -> d.getId().equals(aHook.getDriver())) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException(
                        "No such webhook driver: [" + aHook.getDriver() + "]"));
    }

    private void dispatch(String topic, Object message)
    {
        var relevantHooks = configuration.getGlobalHooks().stream() //
                .filter(Webhook::isEnabled) //
                .filter(h -> h.getTopics().contains(topic)) //
                .map(h -> Pair.of(h, new AtomicInteger(0))) //
                .collect(toCollection(ArrayList::new));

        while (!relevantHooks.isEmpty()) {
            var i = relevantHooks.iterator();

            while (i.hasNext()) {
                var hookAndCount = i.next();

                try {
                    var hook = hookAndCount.getKey();

                    var effectiveTopic = topic;
                    if (hook.getRoutes() != null
                            && hook.getRoutes().containsKey(topic)) {
                        effectiveTopic = hook.getRoutes().get(topic);
                    }

                    var driver = getDriver(hookAndCount.getKey());
                    driver.sendNotification(effectiveTopic, message, hook);
                    i.remove();
                }
                catch (Exception e) {
                    if (hookAndCount.getValue().get() < configuration.getRetryCount()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.error("Unable to send webhook [{}]: {} - retrying in a moment",
                                    hookAndCount.getKey().getUrl(), e.getMessage(), e);
                        }
                        else {
                            LOG.error("Unable to send webhook [{}]: {} - retrying in a moment",
                                    hookAndCount.getKey().getUrl(), e.getMessage());
                        }
                        hookAndCount.getValue().incrementAndGet();
                    }
                    else {
                        if (LOG.isDebugEnabled()) {
                            LOG.error("Unable to invoke webhook [{}]: {} - giving up",
                                    hookAndCount.getKey().getUrl(), e.getMessage(), e);
                        }
                        else {
                            LOG.error("Unable to invoke webhook [{}]: {} - giving up",
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
}
