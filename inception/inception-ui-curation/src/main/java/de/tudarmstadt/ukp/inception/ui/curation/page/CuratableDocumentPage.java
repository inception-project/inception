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
package de.tudarmstadt.ukp.inception.ui.curation.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.PAGE_PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.setProjectPageParameter;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationEditingService;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode;
import de.tudarmstadt.ukp.inception.curation.service.DocumentNotCuratableException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

public interface CuratableDocumentPage
{
    Project getProject();

    Session getSession();

    Class<? extends Page> getPageClass();

    /**
     * @return the service used to look for the next curatable document.
     */
    CurationEditingService getCurationEditingService();

    /**
     * @return the service used to list curatable documents.
     */
    CurationDocumentService getCurationDocumentService();

    /**
     * @return the service used to bring the document state up to date before listing.
     */
    WorkloadManagementService getWorkloadManagementService();

    /**
     * @return the documents of this page's project that are ready to be curated.
     */
    default List<SourceDocument> listCuratableDocuments()
    {
        var project = getProject();
        getWorkloadManagementService().getWorkloadManagerExtension(project).freshenStatus(project);
        return getCurationDocumentService().listCuratableSourceDocuments(project);
    }

    /**
     * Provide the curation CAS for the given document, creating or (re-)merging it as requested,
     * and mark curation on the document as in progress.
     * <p>
     * Use this instead of {@link CurationEditingService#readOrCreateCurationCas} so the UI can
     * switch to another suitable document if one cannot be opened.
     *
     * @param aDocument
     *            the document to merge into.
     * @param aState
     *            the state of the merge target editor - it identifies the data owner to merge into
     *            and the layers to consider.
     * @param aMergeStrategy
     *            how to reconcile disagreeing annotators.
     * @param aMergeMode
     *            whether to rebuild the curation document or only fill its gaps.
     * @return the curation CAS.
     */
    default CAS readOrCreateCurationCas(SourceDocument aDocument, AnnotatorState aState,
            MergeStrategy aMergeStrategy, CurationMergeMode aMergeMode)
        throws IOException, UIMAException, AnnotationException
    {
        try {
            return getCurationEditingService().readOrCreateCurationCas(aDocument,
                    aState.getUser().getUsername(), aState.getAnnotationLayers(), aMergeStrategy,
                    aMergeMode);
        }
        catch (DocumentNotCuratableException e) {
            getSession().error(e.getMessage());

            var pageParameters = new PageParameters();
            setProjectPageParameter(pageParameters, getProject());

            // Skip to the next curatable document rather than ejecting the curator to the project
            // page. Returning to the same kind of page rather than a hard-coded class keeps a
            // subclass on its own mount.
            try {
                getCurationEditingService()
                        .findNextCuratableDocument(listCuratableDocuments(), e.getDocument())
                        .ifPresent(next -> pageParameters.set(PAGE_PARAM_DOCUMENT, next.getId()));
            }
            catch (Exception ex) {
                LoggerFactory.getLogger(getClass()).warn(
                        "Unable to determine the next curatable document after [{}]",
                        e.getDocument(), ex);
                getSession().error("Unable to determine the next curatable document: "
                        + getRootCauseMessage(ex));
            }

            throw new RestartResponseException(getPageClass(), pageParameters);
        }
    }
}
