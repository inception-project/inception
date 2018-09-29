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
package de.tudarmstadt.ukp.clarin.webanno.api.event;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class AnnotationStateChangeEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 2369088588078065027L;
    
    private AnnotationDocument annotationDocument;
    private AnnotationDocumentState previousState;
    private AnnotationDocumentState newState;

    public AnnotationStateChangeEvent(Object aSource, AnnotationDocument aAnnotation,
            AnnotationDocumentState aPreviousState)
    {
        super(aSource);
        annotationDocument = aAnnotation;
        newState = aAnnotation.getState();
        previousState = aPreviousState;
    }

    public SourceDocument getDocument()
    {
        return annotationDocument.getDocument();
    }
    
    public AnnotationDocument getAnnotationDocument()
    {
        return annotationDocument;
    }

    public AnnotationDocumentState getPreviousState()
    {
        return previousState;
    }

    public AnnotationDocumentState getNewState()
    {
        return newState;
    }
}
