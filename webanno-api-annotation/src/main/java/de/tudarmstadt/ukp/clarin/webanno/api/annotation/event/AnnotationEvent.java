/*
 * Copyright 2019
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

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public abstract class AnnotationEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = -7460965556870957082L;
    
    private final Project project;
    private final SourceDocument document;
    private final String user;
    private final AnnotationLayer layer;

    public AnnotationEvent(Object aSource, Project aProject, String aUser,
            AnnotationLayer aLayer)
    {
        this(aSource, aProject, null, aUser, aLayer);
    }

    public AnnotationEvent(Object aSource, SourceDocument aDocument, String aUser,
            AnnotationLayer aLayer)
    {
        this(aSource, aDocument != null ? aDocument.getProject() : null, aDocument, aUser, aLayer);
    }

    private AnnotationEvent(Object aSource, Project aProject, SourceDocument aDocument,
            String aUser, AnnotationLayer aLayer)
    {
        super(aSource);
        project = aProject;
        document = aDocument;
        user = aUser;
        layer = aLayer;
    }
    
    public Project getProject()
    {
        return project;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public String getUser()
    {
        return user;
    }
    
    public AnnotationLayer getLayer()
    {
        return layer;
    }
}
