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
package de.tudarmstadt.ukp.inception.support.extensionpoint;

import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ClassUtils.getAbbreviatedName;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

public abstract class ExtensionPoint_ImplBase<C, E extends Extension<C>>
    implements ExtensionPoint<C, E>
{
    private static final Logger LOG = getLogger(lookup().lookupClass());

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
        var extensions = new ArrayList<E>();

        if (extensionsListProxy != null) {
            extensions.addAll(extensionsListProxy);
            extensions.sort(makeComparator());

            for (E fs : extensions) {
                LOG.debug("Found {} extension: {}", getClass().getSimpleName(),
                        getAbbreviatedName(fs.getClass(), 20));
            }
        }

        BOOT_LOG.info("Found [{}] {} extensions", extensions.size(), getClass().getSimpleName());

        extensionsList = unmodifiableList(extensions);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Comparator<E> makeComparator()
    {
        return (Comparator) AnnotationAwareOrderComparator.INSTANCE;
    }

    @Override
    public List<E> getExtensions()
    {
        if (extensionsList == null) {
            LOG.error(
                    "List of extensions was accessed on this extension point before the extension "
                            + "point was initialized!",
                    new IllegalStateException());
            return emptyList();
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
