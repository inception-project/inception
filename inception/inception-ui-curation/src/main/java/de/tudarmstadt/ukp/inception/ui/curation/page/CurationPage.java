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
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.DocumentEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.url.SingleDocumentEditorUrlParameterStrategy;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws.DocumentEditorWorkspace_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws.mono.SingleDocumentEditorWorkspace;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar.CurationAutoOpenDialogBehavior;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationOpenDocumentDialog;
import de.tudarmstadt.ukp.inception.ui.curation.readiness.CurationReadinessBadgePanel;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.CurationEditorExtension;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.CurationSidebarBehavior;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}" + CurationPage.PAGE_PATH + "/#{"
        + PAGE_PARAM_DOCUMENT + "}")
public class CurationPage
    extends AnnotationPageBase2
{
    public static final String PAGE_PATH = "/curate";

    private static final long serialVersionUID = 8665608337791132617L;

    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userRepository;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean CurationSessionService curationSessionService;

    public CurationPage(PageParameters aPageParameters)
    {
        super(aPageParameters);

        add(new CurationSidebarBehavior());

        add(new CurationAutoOpenDialogBehavior());
        addToFooter(new CurationOpenDocumentDialog(ApplicationPageBase.CID_FOOTER_ITEM,
                getProjectModel(), LoadableDetachableModel.of(this::listCuratableDocuments),
                this::actionOpenCuratedDocument));

        curationSessionService.startSession(userRepository.getCurrentUsername(), getProject());
    }

    @Override
    protected AnnotatorState createAnnotatorState()
    {
        var state = super.createAnnotatorState();
        state.enableExtension(CurationEditorExtension.EXTENSION_ID);
        return state;
    }

    private void actionOpenCuratedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
    {
        try {
            getDocumentEditorManager().actionShowDocument(aTarget, aDocument, CURATION_SET);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }

    @Override
    protected Component createDocumentStatusBadges(String aId, IModel<AnnotatorState> aState)
    {
        return new CurationReadinessBadgePanel(aId, aState);
    }

    @Override
    protected void ensureDocumentMayBeOpened(SourceDocument aDocument, AnnotatorState aState)
    {
        // Must run before the curation CAS is created: once it exists, isDocumentCuratable treats
        // curation as started and would let the document through on any subsequent attempt.
        if (!curationDocumentService.isDocumentCuratable(aDocument)) {
            getSession().error("Document [" + aDocument.getName()
                    + "] is not ready for curation yet. "
                    + "Annotation on it is not complete. Wait until enough annotators have marked the "
                    + "document as finished.");

            backToProjectPage();
        }
    }

    @Override
    protected DocumentEditorWorkspace_ImplBase createWorkspace(String aId)
    {
        return new SingleDocumentEditorWorkspace(aId, this::createDocumentEditorPanel,
                new SingleDocumentEditorUrlParameterStrategy(userRepository::getCurrentUsername,
                        getPinnedDataOwner() != null));
    }

    @Override
    protected AnnotationSet getPinnedDataOwner()
    {
        return CURATION_SET;
    }

    @Override
    protected void requireAccess(User aSessionOwner)
    {
        requireProjectRole(aSessionOwner, CURATOR);
    }

    /**
     * @see CuratableDocumentPage#listCuratableDocuments()
     */
    public List<SourceDocument> listCuratableDocuments()
    {
        var project = getProject();
        workloadManagementService.getWorkloadManagerExtension(project).freshenStatus(project);
        return curationDocumentService.listCuratableSourceDocuments(project);
    }

    @Override
    protected void transitionDocumentStateOnLoadDocument(DocumentEditorPanel aPanel,
            AnnotationDocument aAnnotationDocument)
    {
        var state = aPanel.getAnnotatorState();

        // Opening a document on the curation page typically triggers an initial merge.
        // This initial merge is a write operation, so even if the document should for
        // some reason not be editable, the transition into CURATION_IN_PROGRESS should
        // happen to indicate the initial merge was done. Note, we also do this even if
        // somebody might have disabled the initial merge - just for consistency.
        curationDocumentService.markCurationInProgress(state.getDocument());

        // State transition may have had an impact on editability, so let's clear the cache
        aPanel.clearIsEditableCache();

        if (aPanel.isEditable()) {
            // We maintain an AnnotationDocument for the `CURATION_USER` now
            if (AnnotationDocumentState.NEW == aAnnotationDocument.getState()) {
                documentService.setAnnotationDocumentState(aAnnotationDocument,
                        AnnotationDocumentState.IN_PROGRESS, EXPLICIT_ANNOTATOR_USER_ACTION);
            }
        }
    }
}
