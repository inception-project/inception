/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import org.apache.uima.UIMAException;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.service.DocumentNotCuratableException;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public interface CurationSidebarService
{
    /**
     * @return if user in given annotator state is curating and has finished it
     * 
     * @param state
     *            the annotator state
     */
    boolean isCurationFinished(AnnotatorState state);

    void setShowAll(String aUsername, Long aProjectId, boolean aValue);

    boolean isShowAll(String aUsername, Long aProjectId);

    void setShowScore(String aUsername, Long aProjectId, boolean aValue);

    boolean isShowScore(String aUsername, Long aProjectId);

    /**
     * Merge the annotations of the given data owners into the curation CAS of the document in the
     * given state.
     *
     * @param aState
     *            the annotator state
     * @param aWorkflow
     *            the curation workflow providing the merge strategy
     * @param aDataOwners
     *            the data owners whose annotations to merge
     * @param aClearTargetCas
     *            whether to clear the curation CAS before merging
     * @return the factory of the merge strategy used
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws UIMAException
     *             if there was a UIMA-level problem
     * @throws NotEditableException
     *             if curation of the document is already finished.
     */
    MergeStrategyFactory<?> merge(AnnotatorState aState, CurationWorkflow aWorkflow,
            Collection<AnnotationSet> aDataOwners, boolean aClearTargetCas)
        throws IOException, UIMAException, NotEditableException;

    /**
     * Merges the annotations of all curatable data owners into the curation CAS of the document in
     * the given state, if the document is being curated, auto-merge is enabled for the project and
     * the document requires an initial merge (see
     * {@code CurationDocumentService#isInitialMergeRequired}). The merge moves the document into
     * curation as its last step. If auto-merge is disabled, an empty curation CAS is created
     * instead.
     * <p>
     * If the merge fails, the curation CAS is deleted again, so the document is not left looking
     * merged and the merge is retried the next time the document is opened. The same happens if
     * there are no annotators to merge from.
     * <p>
     * Does not check whether the session owner may curate the document, nor whether the document is
     * curatable (see {@code CurationDocumentService#isDocumentCuratable}). The caller has to ensure
     * both before calling this - otherwise a document that is not ready for curation is moved into
     * curation.
     *
     * @param aState
     *            the annotator state of the document being opened
     * @return the factory of the merge strategy used, or nothing if no initial merge was performed
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws UIMAException
     *             if there was a UIMA-level problem
     * @throws NotEditableException
     *             if curation of the document is already finished.
     * @throws DocumentNotCuratableException
     *             if there are no annotators to merge from.
     */
    Optional<MergeStrategyFactory<?>> performInitialMergeIfRequired(AnnotatorState aState)
        throws IOException, UIMAException, NotEditableException, DocumentNotCuratableException;
}
