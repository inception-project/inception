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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;

@Component
public class AnnotationEditorExtensionRegistryImpl
    implements AnnotationEditorExtensionRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<AnnotationEditorExtension> extensionsProxy;

    private List<AnnotationEditorExtension> extensions;
    
    public AnnotationEditorExtensionRegistryImpl(
            @Lazy @Autowired(required = false) List<AnnotationEditorExtension> aExtensions)
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
        List<AnnotationEditorExtension> exts = new ArrayList<>();

        if (extensionsProxy != null) {
            exts.addAll(extensionsProxy);
            AnnotationAwareOrderComparator.sort(exts);
        
            for (AnnotationEditorExtension fs : exts) {
                log.info("Found annotation editor extension: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }
        
        extensions = Collections.unmodifiableList(exts);
    }
    
    @Override
    public List<AnnotationEditorExtension> getExtensions()
    {
        return extensions;
    }
    
    @Override
    public AnnotationEditorExtension getExtension(String aId)
    {
        if (aId == null) {
            return null;
        }
        else {
            return extensions.stream().filter(ext -> aId.equals(ext.getBeanName())).findFirst()
                    .orElse(null);
        }
    }
    
    @Override
    public void fireAction(AnnotationActionHandler aActionHandler, AnnotatorState aModelObject,
            AjaxRequestTarget aTarget, JCas aJCas, VID aParamId, String aAction, int aBegin,
            int aEnd)
        throws IOException, AnnotationException
    {
        for (AnnotationEditorExtension ext : getExtensions()) {
            ext.handleAction(aActionHandler, aModelObject, aTarget, aJCas, aParamId, aAction,
                    aBegin, aEnd);
        }
    }
    
    @Override
    public void fireRender(JCas aJCas, AnnotatorState aModelObject, VDocument aVdoc,
                           int aWindowBeginOffset, int aWindowEndOffset)
    {
        for (AnnotationEditorExtension ext: getExtensions()) {
            ext.render(aJCas, aModelObject, aVdoc, aWindowBeginOffset, aWindowEndOffset);
        }
    }
}
