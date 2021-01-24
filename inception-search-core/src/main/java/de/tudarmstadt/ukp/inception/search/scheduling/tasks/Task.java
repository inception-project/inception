/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.search.scheduling.tasks;

import static org.apache.commons.lang3.Validate.notNull;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * Abstract search task
 */
public abstract class Task
    implements Runnable
{
    private final Project project;
    private final String user;
    private final SourceDocument sourceDocument;
    private final AnnotationDocument annotationDocument;

    private byte[] binaryCas;

    public Task(Project aProject, String aUser)
    {
        // notNull(aUser);
        notNull(aProject);

        user = aUser;
        project = aProject;
        sourceDocument = null;
        annotationDocument = null;
        binaryCas = null;
    }

    public Task(SourceDocument aSourceDocument, byte[] aBinaryCas)
    {
        notNull(aSourceDocument);

        user = null;
        project = aSourceDocument.getProject();
        sourceDocument = aSourceDocument;
        annotationDocument = null;
        binaryCas = aBinaryCas;
    }

    public Task(AnnotationDocument aAnnotationDocument, byte[] aBinaryCas)
    {
        notNull(aAnnotationDocument);
        notNull(aBinaryCas);

        user = aAnnotationDocument.getUser();
        project = aAnnotationDocument.getProject();
        sourceDocument = null;
        annotationDocument = aAnnotationDocument;
        binaryCas = aBinaryCas;
    }

    public String getUser()
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

    public byte[] getBinaryCas()
    {
        return binaryCas;
    }

    public void setBinaryCas(byte[] aBinaryCas)
    {
        binaryCas = aBinaryCas;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [project=");
        builder.append(project.getName());
        builder.append(", user=");
        builder.append((user == null) ? " " : user);
        builder.append(", sourceDocument=");
        builder.append(sourceDocument == null ? "null" : sourceDocument.getName());
        builder.append(", annotationDocument=");
        builder.append(annotationDocument == null ? "null" : annotationDocument.getName());
        builder.append("]");
        return builder.toString();
    }

    /**
     * Used to avoid scheduling duplicate tasks. Returns true if the current task is a duplicate of
     * the given task.
     * 
     * @param aTask
     *            the given scheduling task
     * @return whether the given task matches this one
     */
    public abstract boolean matches(Task aTask);

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((project == null) ? 0 : project.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((sourceDocument == null) ? 0 : sourceDocument.hashCode());
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
