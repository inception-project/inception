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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.docnav;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationNavigationUserPrefs.KEY_ANNOTATION_NAVIGATION_USER_PREFS;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static wicket.contrib.input.events.EventType.click;
import static wicket.contrib.input.events.key.KeyType.Page_down;
import static wicket.contrib.input.events.key.KeyType.Page_up;
import static wicket.contrib.input.events.key.KeyType.Shift;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.export.ExportDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open.OpenDocumentDialog;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import jakarta.persistence.NoResultException;
import wicket.contrib.input.events.key.KeyType;

public class DocumentNavigator
    extends Panel
{
    private static final long serialVersionUID = 7061696472939390003L;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userService;
    private @SpringBean DocumentAccess documentAccess;
    private @SpringBean DocumentService documentService;
    private @SpringBean PreferencesService preferencesService;

    private AnnotationPageBase page;

    private final ExportDocumentDialog exportDialog;

    public DocumentNavigator(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;

        add(new LambdaAjaxLink("showPreviousDocument", t -> actionShowPreviousDocument(t))
                .add(new InputBehavior(new KeyType[] { Shift, Page_up }, click)));

        add(new LambdaAjaxLink("showNextDocument", t -> actionShowNextDocument(t))
                .add(new InputBehavior(new KeyType[] { Shift, Page_down }, click)));

        add(new LambdaAjaxLink("showOpenDocumentDialog", this::actionShowOpenDocumentDialog));

        add(exportDialog = new ExportDocumentDialog("exportDialog", page.getModel()));
        add(new LambdaAjaxLink("showExportDialog", exportDialog::show)
                .add(visibleWhen(this::isExportable)));
    }

    private boolean isExportable()
    {
        return documentAccess.canExportAnnotationDocument(userService.getCurrentUser(),
                page.getModelObject().getProject());
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
        var state = page.getModelObject();
        var aDocuments = page.getListOfDocs();

        var prefs = preferencesService.loadTraitsForUserAndProject(
                KEY_ANNOTATION_NAVIGATION_USER_PREFS, sessionOwner, state.getProject());

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
        var state = page.getModelObject();
        var aDocuments = page.getListOfDocs();

        var prefs = preferencesService.loadTraitsForUserAndProject(
                KEY_ANNOTATION_NAVIGATION_USER_PREFS, sessionOwner, state.getProject());

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

    private boolean isTerminal(SourceDocument aDocument)
    {
        var state = page.getModelObject();
        var dataOwner = state.getUser();
        try {
            var annDoc = documentService.getAnnotationDocument(aDocument, dataOwner);
            return annDoc.getState().isTerminal();
        }
        catch (NoResultException e) {
            return AnnotationDocumentState.NEW.isTerminal();
        }
    }

    public void actionShowOpenDocumentDialog(AjaxRequestTarget aTarget)
    {
        page.getModelObject().getSelection().clear();
        page.getFooterItems().getObject().stream()
                .filter(component -> component instanceof OpenDocumentDialog)
                .map(component -> (OpenDocumentDialog) component).findFirst()
                .ifPresent(dialog -> dialog.show(aTarget));
    }
}
