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

import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;

public class SpanCreatedEvent extends ApplicationEvent
{
    private static final long serialVersionUID = 5206262614840209407L;
    
    private final AnnotatorState state;
    private final AnnotationFS annotation;
    
    public SpanCreatedEvent(Object aSource, AnnotatorState aState, AnnotationFS aAnnotation)
    {
        super(aSource);
        state = aState;
        annotation = aAnnotation;
    }
    
    public AnnotatorState getState()
    {
        return state;
    }
    
    public AnnotationFS getAnnotation()
    {
        return annotation;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SpanCreatedEvent [");
        if (state != null) {
            builder.append("docID=");
            builder.append(state.getDocument().getId());
            builder.append(", user=");
            builder.append(state.getUser().getUsername());
            builder.append(", ");
        }
        builder.append("span=[");
        builder.append(annotation.getBegin());
        builder.append("-");
        builder.append(annotation.getEnd());
        builder.append("](");
        builder.append(annotation.getCoveredText());
        builder.append(")]");
        return builder.toString();
    }
}
