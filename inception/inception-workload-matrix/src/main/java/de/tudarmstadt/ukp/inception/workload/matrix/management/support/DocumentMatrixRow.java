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
package de.tudarmstadt.ukp.inception.workload.matrix.management.support;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixCurationReadiness;

public class DocumentMatrixRow
    implements Serializable
{
    private static final long serialVersionUID = 7351346533262118753L;

    private final SourceDocument sourceDocument;
    private final Set<AnnotationSet> annotators;
    private final Map<AnnotationSet, AnnotationDocument> annotationDocuments;

    private boolean selected;

    public DocumentMatrixRow(SourceDocument aSourceDocument, Set<AnnotationSet> aAnnotators)
    {
        sourceDocument = aSourceDocument;
        annotators = aAnnotators;
        annotationDocuments = new HashMap<>();
    }

    public void add(AnnotationDocument aAnnotationDocument)
    {
        annotationDocuments.put(aAnnotationDocument.getAnnotationSet(), aAnnotationDocument);
    }

    public SourceDocument getSourceDocument()
    {
        return sourceDocument;
    }

    public AnnotationDocument getAnnotationDocument(AnnotationSet aSet)
    {
        return annotationDocuments.get(aSet);
    }

    public Set<AnnotationSet> getAnnotators()
    {
        return annotators;
    }

    public void setSelected(boolean aSelected)
    {
        selected = aSelected;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public SourceDocumentState getState()
    {
        var counts = countStates();

        var newCount = counts.newCount();
        var ignoredCount = counts.ignoredCount();
        var inProgressCount = counts.inProgressCount();
        var finishedCount = counts.finishedCount();
        var requiredCount = annotators.size() - ignoredCount;

        SourceDocumentState state = sourceDocument.getState();

        if (!(CURATION_IN_PROGRESS == state || CURATION_FINISHED == state)) {
            if (MatrixCurationReadiness.isReadyForCuration(finishedCount, ignoredCount,
                    annotators.size())) {
                state = ANNOTATION_FINISHED;
            }
            else if (newCount == requiredCount) {
                state = SourceDocumentState.NEW;
            }
            else if (inProgressCount > 0 || finishedCount > 0) {
                state = SourceDocumentState.ANNOTATION_IN_PROGRESS;
            }
        }

        return state;
    }

    /**
     * @return whether the document is ready for curation - see {@link MatrixCurationReadiness}.
     *         Computed from the already-loaded annotation documents of this row, so no query.
     */
    public boolean isReadyForCuration()
    {
        var counts = countStates();

        return MatrixCurationReadiness.isReadyForCuration(counts.finishedCount(),
                counts.ignoredCount(), annotators.size());
    }

    /**
     * Per-state annotator counts of a row. Annotators without an annotation document count as NEW.
     */
    private record StateCounts(long ignoredCount, long inProgressCount, long finishedCount,
            long newCount)
    {}

    private StateCounts countStates()
    {
        long ignoredCount = 0;
        long inProgressCount = 0;
        long finishedCount = 0;
        long newCount = 0;

        for (var annotator : annotators) {
            var annDoc = annotationDocuments.get(annotator);

            if (annDoc == null) {
                newCount++;
                continue;
            }

            switch (annDoc.getState()) {
            case IGNORE:
                ignoredCount++;
                break;
            case IN_PROGRESS:
                inProgressCount++;
                break;
            case FINISHED:
                finishedCount++;
                break;
            case NEW:
                newCount++;
                break;
            }
        }

        return new StateCounts(ignoredCount, inProgressCount, finishedCount, newCount);
    }

    public SourceDocumentState getCurationState()
    {
        switch (sourceDocument.getState()) {
        case CURATION_IN_PROGRESS:
            return CURATION_IN_PROGRESS;
        case CURATION_FINISHED:
            return CURATION_FINISHED;
        default:
            return SourceDocumentState.NEW;
        }
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof DocumentMatrixRow)) {
            return false;
        }
        DocumentMatrixRow castOther = (DocumentMatrixRow) other;
        return Objects.equals(sourceDocument, castOther.sourceDocument);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sourceDocument);
    }
}
