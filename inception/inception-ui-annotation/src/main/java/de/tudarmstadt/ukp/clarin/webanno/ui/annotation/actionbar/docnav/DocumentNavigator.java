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
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static wicket.contrib.input.events.EventType.click;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarContext;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.export.ExportDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyBindingsProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyBindingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open.OpenDocumentDialog;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import jakarta.persistence.NoResultException;

public class DocumentNavigator
    extends Panel
{
    private static final long serialVersionUID = 7061696472939390003L;

    private @SpringBean KeyBindingsProperties keyBindings;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userService;
    private @SpringBean DocumentAccess documentAccess;
    private @SpringBean DocumentService documentService;
    private @SpringBean PreferencesService preferencesService;

    private AnnotationPageBase page;
    private DocumentEditor editor;
    private IModel<AnnotatorState> state;

    private final ExportDocumentDialog exportDialog;

    public DocumentNavigator(String aId, ActionBarContext aContext)
    {
        super(aId);

        page = aContext.page();
        editor = aContext.editor();
        state = editor.getStateModel();

        add(new LambdaAjaxLink("showPreviousDocument", t -> actionShowPreviousDocument(t))
                .add(keyBindings.getNavigation().getPreviousDocument().toInputBehavior(click))
                .add(visibleWhen(this::hasDocument)).add(
                        AttributeModifier.append("title",
                                () -> " ("
                                        + KeyBindingsUtil.formatShortcut(
                                                keyBindings.getNavigation().getPreviousDocument())
                                        + ")")));

        add(new LambdaAjaxLink("showNextDocument", t -> actionShowNextDocument(t))
                .add(keyBindings.getNavigation().getNextDocument().toInputBehavior(click))
                .add(visibleWhen(this::hasDocument)).add(
                        AttributeModifier.append("title",
                                () -> " ("
                                        + KeyBindingsUtil.formatShortcut(
                                                keyBindings.getNavigation().getNextDocument())
                                        + ")")));

        add(new LambdaAjaxLink("showOpenDocumentDialog", this::actionShowOpenDocumentDialog));

        add(exportDialog = new ExportDocumentDialog("exportDialog", state));
        add(new LambdaAjaxLink("showExportDialog", exportDialog::show)
                .add(visibleWhen(() -> hasDocument() && isExportable())));
    }

    private List<SourceDocument> listDocuments()
    {
        var sessionOwner = userService.getCurrentUser();
        var editorState = state.getObject();
        var dataOwner = editorState.getUser();

        if (editorState.getProject() == null || dataOwner == null) {
            return emptyList();
        }

        return documentService
                .listAccessibleDocuments(editorState.getProject(), dataOwner, sessionOwner).stream()
                .map(AnnotationDocument::getDocument) //
                .collect(toList());
    }

    private boolean isOpenInAnotherEditor(SourceDocument aDocument)
    {
        var dataOwner = state.getObject().getDataOwner();
        if (dataOwner == null) {
            return false;
        }

        return editor.getDocumentEditorManager() //
                .findEditorFor(aDocument, dataOwner) //
                .filter(context -> context != editor) //
                .isPresent();
    }

    private void reportSkippedDocuments(AjaxRequestTarget aTarget, List<SourceDocument> aSkipped)
    {
        if (aSkipped.isEmpty()) {
            return;
        }

        var names = aSkipped.stream() //
                .map(SourceDocument::getName) //
                .collect(joining(", "));

        if (aSkipped.size() == 1) {
            info("Skipped [" + names + "] because it is already open in another editor.");
        }
        else {
            info("Skipped [" + names + "] because they are already open in other editors.");
        }

        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private boolean hasDocument()
    {
        var editorState = state.getObject();
        return editorState != null && editorState.getDocument() != null;
    }

    private boolean isExportable()
    {
        return documentAccess.canExportAnnotationDocument(userService.getCurrentUser(),
                state.getObject().getProject());
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
        var aDocuments = listDocuments();

        var prefs = preferencesService.loadTraitsForUserAndProject(
                KEY_ANNOTATION_NAVIGATION_USER_PREFS, sessionOwner, state.getObject().getProject());

        // Index of the current source document in the list
        var currentDocumentIndex = aDocuments.indexOf(state.getObject().getDocument());

        var skipped = new ArrayList<SourceDocument>();

        while (true) {
            // If the first document
            if (currentDocumentIndex <= 0) {
                reportSkippedDocuments(aTarget, skipped);

                if (prefs.isFinishedDocumentsSkippedByNavigation()) {
                    info("There is no previous unfinished document.");
                }
                else {
                    info("There is no previous document.");
                }
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            currentDocumentIndex--;

            var newDocument = aDocuments.get(currentDocumentIndex);

            if (isOpenInAnotherEditor(newDocument)) {
                skipped.add(newDocument);
                continue;
            }

            if (!prefs.isFinishedDocumentsSkippedByNavigation() || !isTerminal(newDocument)) {
                state.getObject().setDocument(aDocuments.get(currentDocumentIndex), aDocuments);
                editor.actionLoadDocument(aTarget);
                reportSkippedDocuments(aTarget, skipped);
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
        var aDocuments = listDocuments();

        var prefs = preferencesService.loadTraitsForUserAndProject(
                KEY_ANNOTATION_NAVIGATION_USER_PREFS, sessionOwner, state.getObject().getProject());

        // Index of the current source document in the list
        var currentDocumentIndex = aDocuments.indexOf(state.getObject().getDocument());

        var skipped = new ArrayList<SourceDocument>();

        while (true) {
            // If the last document
            if (currentDocumentIndex < 0 || currentDocumentIndex >= aDocuments.size() - 1) {
                reportSkippedDocuments(aTarget, skipped);

                if (prefs.isFinishedDocumentsSkippedByNavigation()) {
                    info("There is no next unfinished document.");
                }
                else {
                    info("There is no next document.");
                }
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            currentDocumentIndex++;

            var newDocument = aDocuments.get(currentDocumentIndex);

            if (isOpenInAnotherEditor(newDocument)) {
                skipped.add(newDocument);
                continue;
            }

            if (!prefs.isFinishedDocumentsSkippedByNavigation() || !isTerminal(newDocument)) {
                state.getObject().setDocument(aDocuments.get(currentDocumentIndex), aDocuments);
                editor.actionLoadDocument(aTarget);
                reportSkippedDocuments(aTarget, skipped);
                break;
            }
        }
    }

    private boolean isTerminal(SourceDocument aDocument)
    {
        var dataOwner = state.getObject().getUser();
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
        state.getObject().clearSelection();
        page.getFooterItems().getObject().stream()
                .filter(component -> component instanceof OpenDocumentDialog)
                .map(component -> (OpenDocumentDialog) component).findFirst()
                .ifPresent(dialog -> dialog.show(aTarget, editor));
    }
}
