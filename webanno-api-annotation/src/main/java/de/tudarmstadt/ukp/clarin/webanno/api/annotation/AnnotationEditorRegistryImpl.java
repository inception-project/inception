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

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Component;

@Component
public class AnnotationEditorRegistryImpl
    implements AnnotationEditorRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<AnnotationEditorFactory> factories;

    public AnnotationEditorRegistryImpl(@Autowired List<AnnotationEditorFactory> aFactories)
    {
        OrderComparator.sort(aFactories);
        
        for (AnnotationEditorFactory ext : aFactories) {
            log.info("Found annotation editor factory: {}",
                    ClassUtils.getAbbreviatedName(ext.getClass(), 20));
        }
        
        factories = Collections.unmodifiableList(aFactories);
    }

    @Override
    public List<AnnotationEditorFactory> getEditorFactories()
    {
        return factories;
    }
    
    @Override
    public AnnotationEditorFactory getEditorFactory(String aId)
    {
        if (aId == null) {
            return null;
        }
        else {
            return factories.stream().filter(f -> aId.equals(f.getBeanName())).findFirst()
                    .orElse(null);
        }
    }
    
    @Override
    public AnnotationEditorFactory getDefaultEditorFactory()
    {
        return getEditorFactories().get(0);
    }
}
