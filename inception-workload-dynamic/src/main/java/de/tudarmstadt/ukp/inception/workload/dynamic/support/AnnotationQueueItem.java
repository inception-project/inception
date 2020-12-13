/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.inception.workload.dynamic.support;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class AnnotationQueueItem
    implements Serializable
{
    private static final long serialVersionUID = -874572990382384661L;

    private final SourceDocument sourceDocument;
    private final List<AnnotationDocument> annotationDocuments;
    private final Set<String> annotators;
    private int inProgressCount;
    private int finishedCount;
    private Date lastUpdated;

    public AnnotationQueueItem(SourceDocument aSourceDocument,
            List<AnnotationDocument> aAnnotationDocuments)
    {
        super();
        sourceDocument = aSourceDocument;
        annotationDocuments = aAnnotationDocuments;

        annotators = new TreeSet<>();
        for (AnnotationDocument ad : annotationDocuments) {
            switch (ad.getState()) {
            case IN_PROGRESS:
                inProgressCount++;
                annotators.add(ad.getUser());
                updateLastUpdated(ad.getTimestamp());
                break;
            case FINISHED:
                finishedCount++;
                annotators.add(ad.getUser());
                updateLastUpdated(ad.getTimestamp());
                break;
            default:
                // Nothing to do
            }
        }
    }

    private void updateLastUpdated(Date aDate)
    {
        if (aDate == null) {
            return;
        }

        if (lastUpdated == null) {
            lastUpdated = aDate;
            return;
        }

        if (lastUpdated.compareTo(aDate) < 0) {
            lastUpdated = aDate;
        }
    }

    public SourceDocument getSourceDocument()
    {
        return sourceDocument;
    }

    public List<AnnotationDocument> getAnnotationDocuments()
    {
        return annotationDocuments;
    }

    public Set<String> getAnnotators()
    {
        return annotators;
    }

    public int getFinishedCount()
    {
        return finishedCount;
    }

    public int getInProgressCount()
    {
        return inProgressCount;
    }

    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof AnnotationQueueItem)) {
            return false;
        }
        AnnotationQueueItem castOther = (AnnotationQueueItem) other;
        return new EqualsBuilder().append(sourceDocument, castOther.sourceDocument).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(sourceDocument).toHashCode();
    }
}
