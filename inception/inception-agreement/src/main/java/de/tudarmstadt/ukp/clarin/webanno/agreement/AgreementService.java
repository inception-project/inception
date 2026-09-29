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
package de.tudarmstadt.ukp.clarin.webanno.agreement;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface AgreementService
{
    Map<SourceDocument, List<AnnotationDocument>> getDocumentsToEvaluate(Project aProject,
            List<SourceDocument> aDocuments, DefaultAgreementTraits aTraits);

    /**
     * The curation counterpart of {@link #getDocumentsToEvaluate}: the curator's data of a document
     * is only evaluated once curation has started - or, if the calculation is limited to finished
     * documents, once it has finished. Before that, the curation data is not a rater's work that
     * could be compared.
     *
     * @param aDocument
     *            the document.
     * @param aTraits
     *            the agreement settings.
     * @return whether the curator's data of the document is to be evaluated.
     */
    static boolean isCurationToEvaluate(SourceDocument aDocument, DefaultAgreementTraits aTraits)
    {
        if (aTraits != null && aTraits.isLimitToFinishedDocuments()) {
            return aDocument.getState() == CURATION_FINISHED;
        }

        return aDocument.getState() == CURATION_IN_PROGRESS
                || aDocument.getState() == CURATION_FINISHED;
    }

    /**
     * 
     * @param aOut
     *            target stream
     * @param aLayer
     *            the layer to diff.
     * @param aFeature
     *            the feature to diff. If this is null, then the diff is only based on positions.
     * @param aTraits
     *            the diff settings
     * @param aDocuments
     *            the documents to diff
     * @param aAnnotators
     *            the annotators to diff
     */
    void exportDiff(OutputStream aOut, AnnotationLayer aLayer, AnnotationFeature aFeature,
            DefaultAgreementTraits aTraits, List<SourceDocument> aDocuments,
            List<AnnotationSet> aAnnotators);
}
