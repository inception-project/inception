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
package de.tudarmstadt.ukp.inception.documents.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class AnnotationStateChangeEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 2369088588078065027L;

    private String user;
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

        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            user = context.getAuthentication().getName();
        }
        else {
            user = "<SYSTEM>";
        }
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

    public String getUser()
    {
        return user;
    }
}
