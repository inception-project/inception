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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.event;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;

import org.apache.uima.cas.FeatureStructure;
import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class FeatureValueUpdatedEvent extends ApplicationEvent
{
    private static final long serialVersionUID = -6246331778850797138L;
    
    private final SourceDocument document;
    private final String user;
    private final FeatureStructure fs;
    private final AnnotationFeature feature;
    private final Object oldValue;
    private final Object newValue;
    
    public FeatureValueUpdatedEvent(Object aSource, SourceDocument aDocument, String aUser,
            FeatureStructure aFS, AnnotationFeature aFeature, Object aNewValue, Object aOldValue)
    {
        super(aSource);
        
        document = aDocument;
        user = aUser;
        fs = aFS;
        feature = aFeature;
        oldValue = aOldValue;
        newValue = aNewValue;
    }

    public SourceDocument getDocument()
    {
        return document;
    }
    
    public String getUser()
    {
        return user;
    }
    
    public FeatureStructure getFS()
    {
        return fs;
    }

    public AnnotationFeature getFeature()
    {
        return feature;
    }

    public Object getOldValue()
    {
        return oldValue;
    }

    public Object getNewValue()
    {
        return newValue;
    }
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureValueUpdatedEvent [");
        if (document != null) {
            builder.append("docID=");
            builder.append(document.getId());
            builder.append(", user=");
            builder.append(user);
            builder.append(", ");
        }
        builder.append("addr=");
        builder.append(getAddr(fs));
        builder.append(", feature=");
        builder.append(feature.getName());
        builder.append(", old=");
        builder.append(oldValue);
        builder.append(", new=");
        builder.append(newValue);
        builder.append("]");
        return builder.toString();
    }
}
