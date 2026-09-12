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
import de.tudarmstadt.ukp.inception.curation.service.CurationEditingService;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode;
import de.tudarmstadt.ukp.inception.curation.service.DocumentNotCuratableException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public interface CuratableDocumentPage
{
    Project getProject();

    List<SourceDocument> getListOfDocs();

    Session getSession();

    Class<? extends Page> getPageClass();

    AnnotatorState getModelObject();

    /**
     * @return the service used to look for the next curatable document.
     */
    CurationEditingService getCurationEditingService();

    /**
     * Provide the curation CAS for the given document, creating or (re-)merging it as requested,
     * and mark curation on the document as in progress.
     * <p>
     * Use this instead of {@link CurationEditingService#readOrCreateCurationCas} so the UI can
     * switch to another suitable document if one cannot be opened.
     *
     * @param aDocument
     *            the document to merge into. Taken explicitly rather than from the page state
     *            because the initial merge runs while that state is still being set up.
     * @param aMergeStrategy
     *            how to reconcile disagreeing annotators.
     * @param aMergeMode
     *            whether to rebuild the curation document or only fill its gaps.
     * @return the curation CAS.
     */
    default CAS readOrCreateCurationCas(SourceDocument aDocument, MergeStrategy aMergeStrategy,
            CurationMergeMode aMergeMode)
        throws IOException, UIMAException, AnnotationException
    {
        var state = getModelObject();

        try {
            return getCurationEditingService().readOrCreateCurationCas(aDocument,
                    state.getUser().getUsername(), state.getAnnotationLayers(), aMergeStrategy,
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
                        .findNextCuratableDocument(getListOfDocs(), e.getDocument())
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
