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
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.LOAD_ONLY;

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
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationEditingService;
import de.tudarmstadt.ukp.inception.curation.service.CurationNotPossibleException;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationOpenDocumentDialog;
import de.tudarmstadt.ukp.inception.ui.curation.editor.SplitCurationEditorFactory;
import de.tudarmstadt.ukp.inception.ui.curation.readiness.CurationReadinessBadgePanel;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}" + SplitCurationPage.PAGE_PATH + "/#{"
        + PAGE_PARAM_DOCUMENT + "}")
public class SplitCurationPage
    extends AnnotationPageBase2
    implements CuratableDocumentPage
{
    public static final String PAGE_PATH = "/curate-split";

    private static final long serialVersionUID = -2016348345152950844L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean CurationEditingService curationEditingService;
    private @SpringBean CurationService curationService;
    private @SpringBean AnnotationEditorRegistry editorRegistry;

    public SplitCurationPage(PageParameters aPageParameters)
    {
        super(aPageParameters);

        add(new CurationAutoOpenDialogBehavior());
        addToFooter(new CurationOpenDocumentDialog(ApplicationPageBase.CID_FOOTER_ITEM,
                getProjectModel(), LoadableDetachableModel.of(this::listCuratableDocuments),
                this::actionOpenCuratedDocument));
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

    /**
     * Always show the split-pane curation view, regardless of which editor the project prefers. The
     * split view is the whole point of this page, and its factory declares itself unsuitable
     * everywhere, so it can only ever arrive by being pinned like this.
     */
    @Override
    protected DocumentEditorPanel createDocumentEditorPanel(String aId)
    {
        return new MainDocumentEditorPanel(aId)
        {
            private static final long serialVersionUID = 6215758593066090666L;

            @Override
            protected AnnotationEditorFactory resolveEditorFactory(SourceDocument aDocument)
            {
                return editorRegistry.getEditorFactory(SplitCurationEditorFactory.ID);
            }
        };
    }

    @Override
    protected DocumentEditorWorkspace_ImplBase createWorkspace(String aId)
    {
        return new SingleDocumentEditorWorkspace(aId, this::createDocumentEditorPanel,
                new SingleDocumentEditorUrlParameterStrategy(userRepository::getCurrentUsername,
                        getPinnedDataOwner() != null));
    }

    /**
     * Curation always edits the curation user's CAS, so the data owner is fixed here rather than
     * taken from the URL.
     */
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
     * Ensure the curation CAS exists before the page's generic load path reads it. Unlike the
     * modern {@code CurationPage} - which relies on the curation sidebar's opt-in auto-merge - the
     * split-pane page always presents a merged document, so the initial merge happens here
     * unconditionally, as it did on the legacy page.
     */
    @Override
    protected void ensureDocumentMayBeOpened(SourceDocument aDocument, AnnotatorState aState)
    {
        try {
            readOrCreateCurationCas(aDocument, aState,
                    curationService.getDefaultMergeStrategy(aDocument.getProject()), LOAD_ONLY);
        }
        catch (CurationNotPossibleException e) {
            getSession().error(e.getMessage());
            backToProjectPage();
        }
        catch (Exception e) {
            // Rethrows ReplaceHandlerException, so a redirect to the next curatable document
            // survives instead of being downgraded to the project page by the fallback below.
            handleException(null, e);

            backToProjectPage();
        }
    }

    @Override
    public CurationEditingService getCurationEditingService()
    {
        return curationEditingService;
    }

    @Override
    public CurationDocumentService getCurationDocumentService()
    {
        return curationDocumentService;
    }

    @Override
    public WorkloadManagementService getWorkloadManagementService()
    {
        return workloadManagementService;
    }

    @Override
    protected void transitionDocumentStateOnLoadDocument(DocumentEditorPanel aPanel,
            AnnotationDocument aAnnotationDocument)
    {
        var state = aPanel.getAnnotatorState();

        // Opening a document on a curation page triggers an initial merge, which is a write
        // operation, so the transition into CURATION_IN_PROGRESS should happen even if the document
        // should for some reason not be editable.
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
