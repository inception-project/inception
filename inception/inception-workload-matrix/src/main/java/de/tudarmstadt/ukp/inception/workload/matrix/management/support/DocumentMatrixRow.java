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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

public class DocumentMatrixRow
    implements Serializable
{
    private static final long serialVersionUID = 7351346533262118753L;

    private final SourceDocument sourceDocument;
    private final Set<String> annotators;
    private final Map<String, AnnotationDocument> annotationDocuments;

    private boolean selected;

    public DocumentMatrixRow(SourceDocument aSourceDocument, Set<String> aAnnotators)
    {
        sourceDocument = aSourceDocument;
        annotators = aAnnotators;
        annotationDocuments = new TreeMap<>();
    }

    public void add(AnnotationDocument aAnnotationDocument)
    {
        annotationDocuments.put(aAnnotationDocument.getUser(), aAnnotationDocument);
    }

    public SourceDocument getSourceDocument()
    {
        return sourceDocument;
    }

    public AnnotationDocument getAnnotationDocument(String aUsername)
    {
        return annotationDocuments.get(aUsername);
    }

    public Set<String> getAnnotators()
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
        long newCount = 0;
        for (String username : annotators) {
            AnnotationDocument annDoc = annotationDocuments.get(username);
            if (annDoc == null || annDoc.getState() == NEW) {
                newCount++;
            }
        }

        long[] counts = new long[3];
        annotationDocuments.values().stream() //
                .filter(annDoc -> annotators.contains(annDoc.getUser())) //
                .forEach(annDoc -> {
                    switch (annDoc.getState()) {
                    case IGNORE:
                        counts[0]++;
                        break;
                    case IN_PROGRESS:
                        counts[1]++;
                        break;
                    case FINISHED:
                        counts[2]++;
                        break;
                    case NEW:
                        // already handled above
                        break;
                    }
                });

        long ignoredCount = counts[0];
        long inProgressCount = counts[1];
        long finishedCount = counts[2];
        long requiredCount = annotators.size() - ignoredCount;

        SourceDocumentState state = sourceDocument.getState();

        if (!(CURATION_IN_PROGRESS == state || CURATION_FINISHED == state)) {
            if (finishedCount >= requiredCount) {
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
