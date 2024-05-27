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
package de.tudarmstadt.ukp.inception.editor;

import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.editor.config.AnnotationEditorAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationEditorAutoConfiguration#annotationEditorRegistry}.
 * </p>
 */
public class AnnotationEditorRegistryImpl
    implements AnnotationEditorRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<AnnotationEditorFactory> extensionsProxy;

    private List<AnnotationEditorFactory> extensions;

    public AnnotationEditorRegistryImpl(
            @Lazy @Autowired(required = false) List<AnnotationEditorFactory> aExtensions)
    {
        extensionsProxy = aExtensions;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    /* package private */ void init()
    {
        List<AnnotationEditorFactory> exts = new ArrayList<>();

        if (extensionsProxy != null) {
            exts.addAll(extensionsProxy);
            AnnotationAwareOrderComparator.sort(exts);

            for (AnnotationEditorFactory fs : exts) {
                log.debug("Found annotation editor: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }

        BaseLoggers.BOOT_LOG.info("Found [{}] annotation editors", exts.size());

        extensions = unmodifiableList(exts);
    }

    @Override
    public List<AnnotationEditorFactory> getEditorFactories()
    {
        return extensions;
    }

    @Override
    public AnnotationEditorFactory getEditorFactory(String aId)
    {
        if (aId == null) {
            return null;
        }

        return extensions.stream() //
                .filter(f -> aId.equals(f.getBeanName())) //
                .findFirst() //
                .orElse(null);
    }

    @Override
    public AnnotationEditorFactory getDefaultEditorFactory()
    {
        return getEditorFactories().get(0);
    }

    @Override
    public AnnotationEditorFactory getPreferredEditorFactory(Project aProject, String aFormat)
    {
        return getEditorFactories().stream() //
                .max(comparing(factory -> factory.accepts(aProject, aFormat))) //
                .orElseGet(this::getDefaultEditorFactory);
    }
}
