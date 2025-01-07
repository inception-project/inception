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
package de.tudarmstadt.ukp.inception.search.scheduling.tasks;

import java.util.Objects;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.scheduling.MatchableTask;
import de.tudarmstadt.ukp.inception.scheduling.Progress;
import de.tudarmstadt.ukp.inception.scheduling.Task;

/**
 * Abstract search task
 */
public abstract class IndexingTask_ImplBase
    extends Task
    implements MatchableTask
{
    private final SourceDocument sourceDocument;
    private final AnnotationDocument annotationDocument;

    protected IndexingTask_ImplBase(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder);

        sourceDocument = aBuilder.sourceDocument;
        annotationDocument = aBuilder.annotationDocument;
    }

    public SourceDocument getSourceDocument()
    {
        return sourceDocument;
    }

    public AnnotationDocument getAnnotationDocument()
    {
        return annotationDocument;
    }

    @Deprecated
    public abstract Progress getProgress();

    @Override
    public String toString()
    {
        var builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [project=");
        builder.append(getProject());
        if (getUser().isPresent()) {
            builder.append(", user=");
            builder.append(getUser().get());
        }
        if (sourceDocument != null) {
            builder.append(", sourceDocument=");
            builder.append(sourceDocument.getName());
        }
        if (annotationDocument != null) {
            builder.append(", annotationDocument=");
            builder.append(annotationDocument.getName());
        }
        builder.append(", trigger=");
        builder.append(getTrigger());
        builder.append("]");
        return builder.toString();
    }

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

    public static abstract class Builder<T extends Builder<?>>
        extends Task.Builder<T>
    {
        protected SourceDocument sourceDocument;
        protected AnnotationDocument annotationDocument;

        protected Builder()
        {
        }

        @SuppressWarnings("unchecked")
        public T withSourceDocument(SourceDocument aSourceDocument)
        {
            this.sourceDocument = aSourceDocument;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withAnnotationDocument(AnnotationDocument aAnnotationDocument)
        {
            this.annotationDocument = aAnnotationDocument;
            return (T) this;
        }
    }
}
