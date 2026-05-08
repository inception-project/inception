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
package de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument;

import static de.tudarmstadt.ukp.inception.curation.settings.CurationNavigationUserPrefs.KEY_CURATION_NAVIGATION_USER_PREFS;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static wicket.contrib.input.events.EventType.click;

import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * A panel used as Open dialog. It Lists all projects a user is member of for annotation/curation
 * and associated documents
 */
public class CurationOpenDocumentDialogPanel
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = 1299869948010875439L;

    private static final String CID_TABLE = "table";
    private static final String CID_CLOSE_DIALOG = "closeDialog";
    private static final String CID_FINISHED_DOCUMENTS_SKIPPED_BY_NAVIGATION = "finishedDocumentsSkippedByNavigation";

    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userService;
    private @SpringBean PreferencesService preferencesService;

    private final CurationDocumentTable table;

    private final IModel<List<SourceDocument>> documentList;
    private final IModel<Boolean> finishedDocumentsSkippedByNavigation;

    public CurationOpenDocumentDialogPanel(String aId, IModel<AnnotatorState> aState,
            IModel<List<SourceDocument>> aDocumentList)
    {
        super(aId, aState);

        documentList = aDocumentList;

        queue(new LambdaAjaxLink(CID_CLOSE_DIALOG, this::actionCancel)
                .add(new InputBehavior(new KeyType[] { KeyType.Escape }, click)));

        table = new CurationDocumentTable(CID_TABLE, documentList);
        queue(table);

        finishedDocumentsSkippedByNavigation = LambdaModel.of(
                this::isFinishedDocumentsSkippedByNavigation,
                this::setFinishedDocumentsSkippedByNavigation);
        queue(new CheckBox(CID_FINISHED_DOCUMENTS_SKIPPED_BY_NAVIGATION,
                finishedDocumentsSkippedByNavigation) //
                        .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT)));
    }

    private boolean isFinishedDocumentsSkippedByNavigation()
    {
        var project = getModelObject().getProject();
        var sessionOwner = userService.getCurrentUser();
        return preferencesService.loadTraitsForUserAndProject(KEY_CURATION_NAVIGATION_USER_PREFS,
                sessionOwner, project).isFinishedDocumentsSkippedByNavigation();
    }

    private void setFinishedDocumentsSkippedByNavigation(boolean aBoolean)
    {
        var project = getModelObject().getProject();
        var sessionOwner = userService.getCurrentUser();
        var prefs = preferencesService.loadTraitsForUserAndProject(
                KEY_CURATION_NAVIGATION_USER_PREFS, sessionOwner, project);
        prefs.setFinishedDocumentsSkippedByNavigation(aBoolean);
        preferencesService.saveTraitsForUserAndProject(KEY_CURATION_NAVIGATION_USER_PREFS,
                sessionOwner, project, prefs);
    }

    @OnEvent
    public void onCurationDocumentOpenDocumentEvent(CurationDocumentOpenDocumentEvent aEvent)
    {
        getModelObject().setDocument(aEvent.getSourceDocument(), documentList.getObject());

        ((AnnotationPageBase) getPage()).actionLoadDocument(aEvent.getTarget());

        findParent(ModalDialog.class).close(aEvent.getTarget());
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        // If the dialog is aborted without choosing a document, return to a sensible location.
        if (getModelObject().getProject() == null || getModelObject().getDocument() == null) {
            try {
                var ppb = findParent(ProjectPageBase.class);
                if (ppb != null) {
                    ((ProjectPageBase) ppb).backToProjectPage();
                }
            }
            catch (RestartResponseException e) {
                throw e;
            }
            catch (Exception e) {
                setResponsePage(getApplication().getHomePage());
            }
        }

        findParent(ModalDialog.class).close(aTarget);
    }
}
