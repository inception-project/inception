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
package de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint;

import static de.tudarmstadt.ukp.clarin.webanno.support.logging.BaseLoggers.BOOT_LOG;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
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

public abstract class ExtensionPoint_ImplBase<C, E extends Extension<C>>
    implements ExtensionPoint<C, E>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<E> extensionsListProxy;

    private List<E> extensionsList;

    public ExtensionPoint_ImplBase(List<E> aExtensions)
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
        List<E> extensions = new ArrayList<>();

        if (extensionsListProxy != null) {
            extensions.addAll(extensionsListProxy);
            AnnotationAwareOrderComparator.sort(extensions);

            for (E fs : extensions) {
                log.debug("Found {} extension: {}", getClass().getSimpleName(),
                        getAbbreviatedName(fs.getClass(), 20));
            }
        }

        BOOT_LOG.info("Found [{}] {} extensions", extensions.size(), getClass().getSimpleName());

        extensionsList = unmodifiableList(extensions);
    }

    @Override
    public List<E> getExtensions()
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

    @Override
    public List<E> getExtensions(C aContext)
    {
        return getExtensions().stream() //
                .filter(e -> e.accepts(aContext)) //
                .collect(toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X extends E> Optional<X> getExtension(String aId)
    {
        return (Optional<X>) getExtensions().stream() //
                .filter(fs -> fs.getId().equals(aId)) //
                .findFirst();
    }
}
