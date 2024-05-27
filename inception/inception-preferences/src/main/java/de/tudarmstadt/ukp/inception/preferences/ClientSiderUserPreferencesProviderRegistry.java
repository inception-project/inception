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
package de.tudarmstadt.ukp.inception.preferences;

import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.ClassUtils.getAbbreviatedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

public class ClientSiderUserPreferencesProviderRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<ClientSideUserPreferencesProvider> extensionsListProxy;

    private List<ClientSideUserPreferencesProvider> extensionsList;

    public ClientSiderUserPreferencesProviderRegistry(
            List<ClientSideUserPreferencesProvider> aExtensions)
    {
        extensionsListProxy = aExtensions;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    public void init()
    {
        var extensions = new ArrayList<ClientSideUserPreferencesProvider>();

        if (extensionsListProxy != null) {
            extensions.addAll(extensionsListProxy);
            AnnotationAwareOrderComparator.sort(extensions);

            for (var fs : extensions) {
                log.debug("Found {} extension: {}", getClass().getSimpleName(),
                        getAbbreviatedName(fs.getClass(), 20));
            }
        }

        BOOT_LOG.info("Found [{}] {} extensions", extensions.size(), getClass().getSimpleName());

        extensionsList = unmodifiableList(extensions);
    }

    public List<ClientSideUserPreferencesProvider> getExtensions()
    {
        if (extensionsList == null) {
            log.error(
                    "List of extensions was accessed on this extension point before the extension "
                            + "point was initialized!",
                    new IllegalStateException());
            return Collections.emptyList();
        }

        return extensionsList;
    }

    public Optional<ClientSideUserPreferencesProvider> getProviderForClientSideKey(String aKey)
    {
        return getExtensions().stream() //
                .filter(e -> e.getUserPreferencesKey().isPresent())
                .filter(e -> aKey.equals(e.getUserPreferencesKey().get().getClientSideKey())) //
                .findFirst();
    }
}
