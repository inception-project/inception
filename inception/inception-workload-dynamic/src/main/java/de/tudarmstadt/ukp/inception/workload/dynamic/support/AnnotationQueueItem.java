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
package de.tudarmstadt.ukp.inception.workload.dynamic.support;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_DOCUMENT_STATES;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.workload.extension.CurationReadiness.isReadyForCurationEnoughRequired;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.Serializable;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.DateFormatUtils;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.workload.extension.CurationReadiness;

public class AnnotationQueueItem
    implements Serializable
{
    private static final long serialVersionUID = -874572990382384661L;

    private final SourceDocument sourceDocument;
    private final List<AnnotationDocument> annotationDocuments;
    private final Duration abandonationTimeout;
    private final Set<String> annotators;
    private SourceDocumentState state;
    private int inProgressCount;
    private int finishedCount;
    private Date lastUpdated;

    /**
     * Retained because the required-annotation count is a constructor parameter rather than a field
     * - see {@link #isReadyForCuration()}.
     */
    private final boolean readyForCuration;

    public AnnotationQueueItem(SourceDocument aSourceDocument,
            List<AnnotationDocument> aAnnotationDocuments, int aRequiredAnnotations,
            Duration aAbandonationTimeout)
    {
        super();
        sourceDocument = aSourceDocument;
        annotationDocuments = aAnnotationDocuments;
        abandonationTimeout = aAbandonationTimeout;

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

        readyForCuration = isReadyForCurationEnoughRequired(finishedCount, aRequiredAnnotations);

        state = sourceDocument.getState();
        if (!(CURATION_IN_PROGRESS == state || CURATION_FINISHED == state)) {
            if (readyForCuration) {
                state = ANNOTATION_FINISHED;
            }
            else if (finishedCount + inProgressCount == 0) {
                state = SourceDocumentState.NEW;
            }
            else {
                state = ANNOTATION_IN_PROGRESS;
            }
        }
    }

    public SourceDocumentState getState()
    {
        return state;
    }

    /**
     * Whether the workload manager would consider annotation on this document complete enough for
     * it to be curated. Computed in the constructor from the already-loaded annotation documents -
     * no query.
     * <p>
     * Mind that this is the same rule as
     * {@link de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtensionImpl#isReadyForCuration}
     * - both delegate to {@link CurationReadiness} so the two cannot drift apart.
     * <p>
     * Note that a document that is <b>not</b> ready may still legitimately be in curation:
     * readiness is an entry condition, not a continuous invariant. This flag exists to <b>mark</b>
     * such rows for the admin, not to correct them.
     *
     * @return whether the document is ready for curation.
     */
    public boolean isReadyForCuration()
    {
        return readyForCuration;
    }

    /**
     * @return whether curation has been started on this document.
     */
    public boolean isInCuration()
    {
        return CURATION_DOCUMENT_STATES.contains(sourceDocument.getState());
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

    public String getSourceDocumentName()
    {
        return sourceDocument.getName();
    }

    public List<AnnotationDocument> getAnnotationDocuments()
    {
        return annotationDocuments;
    }

    public String getLastUpdated()
    {
        if (lastUpdated == null) {
            return "";
        }
        // Return now "1 day ago" , "2 days" etc until 1 week, then simply put in the date
        long daysSinceLastUpdate = Math.abs(lastUpdated.getTime() - new Date().getTime());
        int diff = (int) DAYS.convert(daysSinceLastUpdate, MILLISECONDS);
        switch (diff) {
        case (0):
            return "Today";
        case (1):
            return "Yesterday";
        case (2):
            return "2 days ago";
        case (3):
            return "3 days ago";
        case (4):
            return "4 days ago";
        case (5):
            return "5 days ago";
        case (6):
            return "6 days ago";
        default:
            return DateFormatUtils.format(lastUpdated, "d MMM y");
        }
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

    public int getInProgressCount()
    {
        return inProgressCount;
    }

    public int getFinishedCount()
    {
        return finishedCount;
    }

    public Set<String> getAnnotators()
    {
        return annotators;
    }

    public Duration getAbandonationTimeout()
    {
        return abandonationTimeout;
    }
}
