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
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.LOAD_ONLY;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.multiedit.DocumentEditorPanel;
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

    private static final Logger LOG = getLogger(lookup().lookupClass());

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
        addToFooter(new CurationOpenDocumentDialog(ApplicationPageBase.CID_FOOTER_ITEM, getModel(),
                LoadableDetachableModel.of(this::getListOfDocs)));
    }

    @Override
    protected Component createDocumentStatusBadges(String aId)
    {
        return new CurationReadinessBadgePanel(aId, getModel());
    }

    /**
     * Always show the split-pane curation view, regardless of which editor the project prefers. The
     * split view is the whole point of this page, and its factory declares itself unsuitable
     * everywhere, so it can only ever arrive by being pinned like this.
     */
    @Override
    protected DocumentEditorPanel createDocumentEditorPanel(String aId)
    {
        return new MainDocumentEditorPanel(aId, getModel())
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
    protected void handleParameters(StringValue aDocumentParameter, StringValue aFocusParameter,
            StringValue aUserParameter)
    {
        requireProjectRole(userRepository.getCurrentUser(), CURATOR);

        // Pin to the curation user
        var curationUser = userRepository.getCurationUser();
        getModelObject().setUser(curationUser);
        super.handleParameters(aDocumentParameter, aFocusParameter,
                StringValue.valueOf(curationUser.getUsername()));
    }

    /**
     * Ensure the curation CAS exists before the page's generic load path reads it. Unlike the
     * modern {@code CurationPage} - which relies on the curation sidebar's opt-in auto-merge - the
     * split-pane page always presents a merged document, so the initial merge happens here
     * unconditionally, as it did on the legacy page.
     */
    @Override
    protected void ensureDocumentMayBeOpened(SourceDocument aDocument)
    {
        try {
            readOrCreateCurationCas(aDocument,
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
    public List<SourceDocument> getListOfDocs()
    {
        // Since the curatable documents depend on the document state, let's make sure the document
        // state is up-to-date
        var project = getModelObject().getProject();
        workloadManagementService.getWorkloadManagerExtension(project).freshenStatus(project);
        return curationDocumentService.listCuratableSourceDocuments(project);
    }

    @Override
    protected void transitionDocumentStateOnLoadDocument(AnnotatorState state,
            AnnotationDocument annotationDocument)
    {
        // Opening a document on a curation page triggers an initial merge, which is a write
        // operation, so the transition into CURATION_IN_PROGRESS should happen even if the document
        // should for some reason not be editable.
        curationDocumentService.markCurationInProgress(state.getDocument());

        // State transition may have had an impact on editability, so let's clear the cache
        clearIsEditableCache();

        if (isEditable()) {
            // We maintain an AnnotationDocument for the `CURATION_USER` now
            if (AnnotationDocumentState.NEW == annotationDocument.getState()) {
                documentService.setAnnotationDocumentState(annotationDocument,
                        AnnotationDocumentState.IN_PROGRESS, EXPLICIT_ANNOTATOR_USER_ACTION);
            }
        }
    }
}
