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

import java.util.Objects;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.scheduling.Task;

/**
 * Abstract search task
 */
public abstract class IndexingTask_ImplBase
    extends Task
{
    private final SourceDocument sourceDocument;
    private final AnnotationDocument annotationDocument;

    private byte[] binaryCas;

    public IndexingTask_ImplBase(Project aProject, String aUser, String aTrigger)
    {
        super(new User(aUser), aProject, aTrigger);

        sourceDocument = null;
        annotationDocument = null;
        binaryCas = null;
    }

    public IndexingTask_ImplBase(SourceDocument aSourceDocument, String aTrigger, byte[] aBinaryCas)
    {
        super(aSourceDocument.getProject(), aTrigger);

        notNull(aBinaryCas);

        sourceDocument = aSourceDocument;
        annotationDocument = null;
        binaryCas = aBinaryCas;
    }

    public IndexingTask_ImplBase(AnnotationDocument aAnnotationDocument, String aTrigger,
            byte[] aBinaryCas)
    {
        super(new User(aAnnotationDocument.getUser()), aAnnotationDocument.getProject(), aTrigger);

        notNull(aBinaryCas);

        sourceDocument = null;
        annotationDocument = aAnnotationDocument;
        binaryCas = aBinaryCas;
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
        builder.append(getProject().getName());
        builder.append(", user=");
        builder.append((getUser() == null) ? " " : getUser());
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
    public abstract boolean matches(IndexingTask_ImplBase aTask);

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof IndexingTask_ImplBase)) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }
        IndexingTask_ImplBase castOther = (IndexingTask_ImplBase) other;
        return Objects.equals(sourceDocument, castOther.sourceDocument)
                && Objects.equals(annotationDocument, castOther.annotationDocument);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), sourceDocument, annotationDocument);
    }
}
