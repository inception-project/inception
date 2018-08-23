/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.search.scheduling.tasks;

import static org.apache.commons.lang3.Validate.notNull;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

/**
 * Abstract search task
 */

public abstract class Task
    implements Runnable
{
    private final Project project;
    private final User user;
    private SourceDocument sourceDocument;
    private AnnotationDocument annotationDocument;
    private JCas jCas;

    public Task(Project aProject, User aUser)
    {
        // notNull(aUser);
        notNull(aProject);

        project = aProject;
        user = aUser;
    }

    public Task(SourceDocument aSourceDocument, JCas aJCas)
    {
        notNull(aSourceDocument);

        project = aSourceDocument.getProject();
        sourceDocument = aSourceDocument;
        jCas = aJCas;
        user = null;
    }

    public Task(AnnotationDocument aAnnotationDocument, JCas aJCas)
    {
        notNull(aAnnotationDocument);
        notNull(aJCas);

        project = aAnnotationDocument.getProject();
        annotationDocument = aAnnotationDocument;
        jCas = aJCas;
        user = null;
    }

    public User getUser()
    {
        return user;
    }

    public Project getProject()
    {
        return project;
    }

    public SourceDocument getSourceDocument()
    {
        return sourceDocument;
    }

    public AnnotationDocument getAnnotationDocument()
    {
        return annotationDocument;
    }

    public JCas getJCas()
    {
        return jCas;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [project=");
        builder.append(project.getName());
        builder.append(", user=");
        builder.append((user == null) ? " " : user.getUsername());
        builder.append(", sourceDocument=");
        builder.append(sourceDocument.getName());
        builder.append(", annotationDocument=");
        builder.append(annotationDocument.getName());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((project == null) ? 0 : project.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result
                + ((sourceDocument == null) ? 0 : sourceDocument.hashCode());
        result = prime * result
                + ((annotationDocument == null) ? 0 : annotationDocument.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Task other = (Task) obj;
        if (project == null) {
            if (other.project != null) {
                return false;
            }
        }
        else if (!project.equals(other.project)) {
            return false;
        }
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        }
        else if (!user.equals(other.user)) {
            return false;
        }
        else if (sourceDocument == null) {
            if (other.sourceDocument != null) {
                return false;
            }
        }
        else if (!sourceDocument.equals(other.sourceDocument)) {
            return false;
        }
        else if (annotationDocument == null) {
            if (other.annotationDocument != null) {
                return false;
            }
        }
        else if (!annotationDocument.equals(other.annotationDocument)) {
            return false;
        }
        return true;
    }
}
