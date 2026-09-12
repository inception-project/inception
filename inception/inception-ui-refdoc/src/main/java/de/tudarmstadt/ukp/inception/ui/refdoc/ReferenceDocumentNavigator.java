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
package de.tudarmstadt.ukp.inception.ui.refdoc;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationNavigationUserPrefs.KEY_ANNOTATION_NAVIGATION_USER_PREFS;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.multiedit.DocumentEditorPanel;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import jakarta.persistence.NoResultException;

public class ReferenceDocumentNavigator
    extends Panel
{
    private static final long serialVersionUID = -2035981248023875332L;

    private @SpringBean DocumentService documentService;
    private @SpringBean PreferencesService preferencesService;
    private @SpringBean UserDao userRepository;

    private final DocumentEditorPanel editor;

    public ReferenceDocumentNavigator(String aId, DocumentEditorPanel aEditor)
    {
        super(aId);

        editor = aEditor;

        setOutputMarkupPlaceholderTag(true);
        add(visibleWhen(() -> editor.getModelObject().getDocument() != null));

        add(new LambdaAjaxLink("showPreviousDocument",
                t -> actionShowAdjacentDocument(t, -1, "previous")));
        add(new LambdaAjaxLink("showNextDocument", t -> actionShowAdjacentDocument(t, 1, "next")));
    }

    private void actionShowAdjacentDocument(AjaxRequestTarget aTarget, int aDirection,
            String aWhich)
    {
        var state = editor.getModelObject();
        var documents = editor.listAccessibleDocuments();
        var prefs = preferencesService.loadTraitsForUserAndProject(
                KEY_ANNOTATION_NAVIGATION_USER_PREFS, userRepository.getCurrentUser(),
                state.getProject());
        var skipFinished = prefs.isFinishedDocumentsSkippedByNavigation();

        var index = documents.indexOf(state.getDocument());
        while (true) {
            index += aDirection;

            if (index < 0 || index >= documents.size()) {
                if (skipFinished) {
                    info("There is no " + aWhich + " unfinished document. Use the Open Document"
                            + " dialog to select finished documents.");
                }
                else {
                    info("There is no " + aWhich + " document.");
                }
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            var candidate = documents.get(index);
            if (!skipFinished || !isTerminal(candidate)) {
                state.setDocument(candidate, documents);
                editor.actionLoadDocument(aTarget);
                return;
            }
        }
    }

    private boolean isTerminal(SourceDocument aDocument)
    {
        var dataOwner = editor.getModelObject().getUser();
        try {
            return documentService.getAnnotationDocument(aDocument, dataOwner).getState()
                    .isTerminal();
        }
        catch (NoResultException e) {
            return AnnotationDocumentState.NEW.isTerminal();
        }
    }
}
