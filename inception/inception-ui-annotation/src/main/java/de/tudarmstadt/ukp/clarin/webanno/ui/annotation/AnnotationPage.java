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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.PAGE_PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.AutoOpenDialogBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open.OpenDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.DocumentEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.url.SingleDocumentEditorUrlParameterStrategy;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws.DocumentEditorWorkspace_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws.split.SplitEditorWorkspace;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/annotate/#{" + PAGE_PARAM_DOCUMENT + "}")
public class AnnotationPage
    extends AnnotationPageBase2
{
    private static final long serialVersionUID = 3894448797109813277L;

    private static final int MAX_EDITORS = 2;

    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userRepository;

    public AnnotationPage(PageParameters aPageParameters)
    {
        super(aPageParameters);

        add(new AutoOpenDialogBehavior());
        addToFooter(new OpenDocumentDialog(ApplicationPageBase.CID_FOOTER_ITEM, getProjectModel(),
                LambdaModel.of(this::getDataOwner), this::listAccessibleDocuments));
    }

    private AnnotationSet getDataOwner()
    {
        return getDocumentEditorManager().getActiveEditor() //
                .map(DiamContext::getAnnotatorState) //
                .map(AnnotatorState::getDataOwner) //
                .orElse(null);
    }

    @Override
    protected DocumentEditorWorkspace_ImplBase createWorkspace(String aId)
    {
        return new SplitEditorWorkspace(aId, MAX_EDITORS, this::createDocumentEditorPanel,
                new SingleDocumentEditorUrlParameterStrategy(userRepository::getCurrentUsername,
                        getPinnedDataOwner() != null));
    }

    @Override
    protected void transitionDocumentStateOnLoadDocument(DocumentEditorPanel aPanel,
            AnnotationDocument aAnnotationDocument)
    {
        var state = aPanel.getAnnotatorState();

        if (aPanel.isEditable()) {
            if (SourceDocumentState.NEW == state.getDocument().getState()) {
                documentService.transitionSourceDocumentState(state.getDocument(),
                        NEW_TO_ANNOTATION_IN_PROGRESS);
            }

            if (AnnotationDocumentState.NEW == aAnnotationDocument.getState()) {
                documentService.setAnnotationDocumentState(aAnnotationDocument,
                        AnnotationDocumentState.IN_PROGRESS, EXPLICIT_ANNOTATOR_USER_ACTION);
            }
        }
    }

    @Override
    protected void requireAccess(User aSessionOwner)
    {
        requireAnyProjectRole(aSessionOwner);
    }

}
