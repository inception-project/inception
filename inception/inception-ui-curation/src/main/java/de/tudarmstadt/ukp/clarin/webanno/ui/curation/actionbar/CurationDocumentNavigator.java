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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.inception.curation.settings.CurationNavigationUserPrefs.KEY_CURATION_NAVIGATION_USER_PREFS;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static wicket.contrib.input.events.EventType.click;
import static wicket.contrib.input.events.key.KeyType.Page_down;
import static wicket.contrib.input.events.key.KeyType.Page_up;
import static wicket.contrib.input.events.key.KeyType.Shift;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.export.ExportDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationOpenDocumentDialog;
import wicket.contrib.input.events.key.KeyType;

public class CurationDocumentNavigator
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = 7061696472939390003L;

    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentAccess documentAccess;
    private @SpringBean UserDao userService;
    private @SpringBean PreferencesService preferencesService;
    private @SpringBean DocumentService documentService;

    private AnnotationPageBase page;

    private final ExportDocumentDialog exportDialog;

    public CurationDocumentNavigator(String aId, AnnotationPageBase aPage)
    {
        super(aId, aPage.getModel());

        page = aPage;

        queue(new LambdaAjaxLink("showPreviousDocument", t -> actionShowPreviousDocument(t))
                .add(new InputBehavior(new KeyType[] { Shift, Page_up }, click)));

        queue(new LambdaAjaxLink("showNextDocument", t -> actionShowNextDocument(t))
                .add(new InputBehavior(new KeyType[] { Shift, Page_down }, click)));

        queue(new LambdaAjaxLink("showOpenDocumentDialog", this::actionShowOpenDocumentDialog));

        queue(exportDialog = new ExportDocumentDialog("exportDialog", getModel()));

        queue(new LambdaAjaxLink("showExportDialog", exportDialog::show) //
                .add(visibleWhen(this::isExportable)));
    }

    private boolean isExportable()
    {
        var sessionOwner = userService.getCurrentUser();
        return documentAccess.canExportAnnotationDocument(sessionOwner,
                getModelObject().getProject());
    }

    /**
     * Show the previous document, if exist
     * 
     * @param aTarget
     *            the AJAX request target
     */
    public void actionShowPreviousDocument(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userService.getCurrentUser();
        var state = getModelObject();
        var aDocuments = page.getListOfDocs();

        var prefs = preferencesService.loadTraitsForUserAndProject(
                KEY_CURATION_NAVIGATION_USER_PREFS, sessionOwner, state.getProject());

        // Index of the current source document in the list
        var currentDocumentIndex = aDocuments.indexOf(state.getDocument());

        while (true) {
            // If the first document
            if (currentDocumentIndex <= 0) {
                if (prefs.isFinishedDocumentsSkippedByNavigation()) {
                    info("There is no previous unfinished document. Use the Open Document dialog to select finished documents.");
                }
                else {
                    info("There is no previous document.");
                }
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            currentDocumentIndex--;

            var newDocument = aDocuments.get(currentDocumentIndex);

            if (!prefs.isFinishedDocumentsSkippedByNavigation() || !isTerminal(newDocument)) {
                state.setDocument(aDocuments.get(currentDocumentIndex), aDocuments);
                page.actionLoadDocument(aTarget);
                break;
            }
        }
    }

    /**
     * Show the next document if exist
     * 
     * @param aTarget
     *            the AJAX request target
     */
    public void actionShowNextDocument(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userService.getCurrentUser();
        var state = getModelObject();
        var aDocuments = page.getListOfDocs();

        var prefs = preferencesService.loadTraitsForUserAndProject(
                KEY_CURATION_NAVIGATION_USER_PREFS, sessionOwner, state.getProject());

        // Index of the current source document in the list
        var currentDocumentIndex = aDocuments.indexOf(state.getDocument());

        while (true) {
            // If the last document
            if (currentDocumentIndex < 0 || currentDocumentIndex >= aDocuments.size() - 1) {
                if (prefs.isFinishedDocumentsSkippedByNavigation()) {
                    info("There is no next unfinished document. Use the Open Document dialog to select finished documents.");
                }
                else {
                    info("There is no next document.");
                }
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            currentDocumentIndex++;

            var newDocument = aDocuments.get(currentDocumentIndex);
            if (!prefs.isFinishedDocumentsSkippedByNavigation() || !isTerminal(newDocument)) {
                state.setDocument(aDocuments.get(currentDocumentIndex), aDocuments);
                page.actionLoadDocument(aTarget);
                break;
            }
        }
    }

    public void actionShowOpenDocumentDialog(AjaxRequestTarget aTarget)
    {
        getModelObject().getSelection().clear();
        page.getFooterItems().getObject().stream()
                .filter(component -> component instanceof CurationOpenDocumentDialog)
                .map(component -> (CurationOpenDocumentDialog) component) //
                .findFirst() //
                .ifPresent(dialog -> dialog.show(aTarget));
    }

    private boolean isTerminal(SourceDocument aDocument)
    {
        var doc = documentService.getSourceDocument(aDocument.getProject().getId(),
                aDocument.getId());
        return doc.getState() == CURATION_FINISHED;
    }
}
