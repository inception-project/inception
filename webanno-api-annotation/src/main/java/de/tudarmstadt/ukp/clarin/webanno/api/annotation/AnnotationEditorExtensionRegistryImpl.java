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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AnnotationEditorExtensionRegistryImpl
    implements AnnotationEditorExtensionRegistry, BeanPostProcessor
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, AnnotationEditorExtension> beans = new HashMap<>();
    private final List<AnnotationEditorExtension> sortedBeans = new ArrayList<>();
    private boolean sorted = false;

    @Override
    public Object postProcessAfterInitialization(Object aBean, String aBeanName)
        throws BeansException
    {
        // Collect annotation editor extensions
        if (aBean instanceof AnnotationEditorExtension) {
            beans.put(aBeanName, (AnnotationEditorExtension) aBean);
            log.debug("Found annotation editor extension: {}", aBeanName);
        }
        
        return aBean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object aBean, String aBeanName)
        throws BeansException
    {
        return aBean;
    }

    @Override
    public List<AnnotationEditorExtension> getExtensions()
    {
        if (!sorted) {
            sortedBeans.addAll(beans.values());
            OrderComparator.sort(sortedBeans);
            sorted = true;
        }
        return sortedBeans;
    }
    
    @Override
    public AnnotationEditorExtension getExtension(String aName)
    {
        return beans.get(aName);
    }
    
    @Override
    public void fireAction(AnnotationActionHandler aActionHandler, AnnotatorState aModelObject,
            AjaxRequestTarget aTarget, JCas aJCas, VID aParamId, int aBegin, int aEnd)
        throws IOException, AnnotationException
    {
        for (AnnotationEditorExtension ext : getExtensions()) {
            ext.handleAction(aActionHandler, aModelObject, aTarget, aJCas, aParamId, aBegin, aEnd);
        }
    }
    
    @Override
    public void fireRender(JCas aJCas, AnnotatorState aModelObject, VDocument aVdoc)
    {
        for (AnnotationEditorExtension ext: getExtensions()) {
            ext.render(aJCas, aModelObject, aVdoc);
        }
    }
}
