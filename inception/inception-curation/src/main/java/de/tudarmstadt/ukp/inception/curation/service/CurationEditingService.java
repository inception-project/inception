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
package de.tudarmstadt.ukp.inception.curation.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;

public interface CurationEditingService
{
    /**
     * Provide the curation CAS for the given document, creating or (re-)merging it as requested,
     * and mark curation on the document as in progress.
     *
     * @param aDocument
     *            the document to curate.
     * @param aCurationUser
     *            the name of the user owning the curation CAS.
     * @param aLayers
     *            the layers to merge.
     * @param aMergeStrategy
     *            the merge strategy to apply.
     * @param aMergeMode
     *            whether to load the existing curation CAS, fill it, or recreate it.
     * @return the curation CAS.
     * @throws DocumentNotCuratableException
     *             if the document cannot be opened for curation at all.
     * @throws CurationNotPossibleException
     *             if the requested merge mode cannot be applied to the document.
     * @throws UIMAException
     *             if there was an UIMA-level problem.
     * @throws IOException
     *             if an I/O error occurs.
     */
    CAS readOrCreateCurationCas(SourceDocument aDocument, String aCurationUser,
            List<AnnotationLayer> aLayers, MergeStrategy aMergeStrategy,
            CurationMergeMode aMergeMode)
        throws CurationNotPossibleException, UIMAException, IOException;

    /**
     * Provide the curation CAS for the given document without touching the document state. This is
     * the read-only counterpart of
     * {@link #readOrCreateCurationCas(SourceDocument, String, List, MergeStrategy, CurationMergeMode)}
     * for callers that already know which CASes to merge from - e.g. when calculating the agreement
     * overview.
     *
     * @param aDocument
     *            the document to curate.
     * @param aCurationUser
     *            the name of the user owning the curation CAS.
     * @param aCasses
     *            the CASes to merge from.
     * @param aTemplateUser
     *            the annotation document used as a template when the curation CAS has to be created
     *            from scratch. May be {@code null} only if a curation CAS already exists.
     * @param aUpgrade
     *            whether to upgrade the curation CAS to the current type system.
     * @param aLayers
     *            the layers to merge.
     * @param aMergeStrategy
     *            the merge strategy to apply.
     * @param aMergeMode
     *            whether to load the existing curation CAS, fill it, or recreate it.
     * @return the curation CAS.
     * @throws UIMAException
     *             if there was an UIMA-level problem.
     * @throws IOException
     *             if an I/O error occurs.
     */
    CAS readCurationCas(SourceDocument aDocument, String aCurationUser, Map<String, CAS> aCasses,
            String aTemplateUser, boolean aUpgrade, List<AnnotationLayer> aLayers,
            MergeStrategy aMergeStrategy, CurationMergeMode aMergeMode)
        throws UIMAException, IOException;

    /**
     * @param aDocument
     *            the document to check.
     * @return whether the given document may be opened for curation. Mirrors the refusal condition
     *         in
     *         {@link #readOrCreateCurationCas(SourceDocument, String, List, MergeStrategy, CurationMergeMode)}.
     */
    boolean isOpenableForCuration(SourceDocument aDocument);

    /**
     * @param aDocuments
     *            the documents to search, in the order they are offered to the curator.
     * @param aDocument
     *            the document to start after.
     * @return the first document after the given one that can actually be opened for curation, if
     *         any (skipping any that are not openable).
     */
    Optional<SourceDocument> findNextCuratableDocument(List<SourceDocument> aDocuments,
            SourceDocument aDocument);
}
