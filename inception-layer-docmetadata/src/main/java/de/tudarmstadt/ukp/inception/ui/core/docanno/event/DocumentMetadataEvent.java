/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.ui.core.docanno.event;

import org.apache.uima.cas.AnnotationBaseFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.AnnotationEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class DocumentMetadataEvent
    extends AnnotationEvent
{
    private static final long serialVersionUID = -3581426461056757004L;
    
    private final AnnotationBaseFS annotation;

    public DocumentMetadataEvent(Object aSource, SourceDocument aDocument, String aUser,
            AnnotationLayer aLayer, AnnotationBaseFS aAnnotation)
    {
        super(aSource, aDocument, aUser, aLayer);
        
        annotation = aAnnotation;
    }
    
    public AnnotationBaseFS getAnnotation()
    {
        return annotation;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [");
        if (getDocument() != null) {
            builder.append("docID=");
            builder.append(getDocument().getId());
            builder.append(", user=");
            builder.append(getUser());
        }
        builder.append("]");
        return builder.toString();
    }
}
