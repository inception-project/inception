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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationNavigationUserPrefs.KEY_ANNOTATION_NAVIGATION_USER_PREFS;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static wicket.contrib.input.events.EventType.click;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableBiFunction;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * A panel used as Open dialog. It Lists all projects a user is member of for annotation/curation
 * and associated documents
 */
public class OpenDocumentDialogPanel
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = 1299869948010875439L;

    private static final String CID_CLOSE_DIALOG = "closeDialog";
    private static final String CID_TABLE = "table";
    private static final String CID_FINISHED_DOCUMENTS_SKIPPED_BY_NAVIGATION = "finishedDocumentsSkippedByNavigation";
    private static final String CID_USER = "user";

    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userRepository;
    private @SpringBean PreferencesService preferencesService;

    private final DropDownChoice<DecoratedObject<User>> userListChoice;
    private final AnnotationDocumentTable table;

    private final IModel<Boolean> finishedDocumentsSkippedByNavigation;

    private final SerializableBiFunction<Project, User, List<AnnotationDocument>> docListProvider;

    public OpenDocumentDialogPanel(String aId, IModel<AnnotatorState> aState,
            SerializableBiFunction<Project, User, List<AnnotationDocument>> aDocListProvider)
    {
        super(aId, aState);

        docListProvider = aDocListProvider;

        queue(userListChoice = createUserListChoice(CID_USER));

        queue(new LambdaAjaxLink(CID_CLOSE_DIALOG, this::actionCancel)
                .add(new InputBehavior(new KeyType[] { KeyType.Escape }, click)));

        finishedDocumentsSkippedByNavigation = LambdaModel.of(
                this::isFinishedDocumentsSkippedByNavigation,
                this::setFinishedDocumentsSkippedByNavigation);
        queue(new CheckBox(CID_FINISHED_DOCUMENTS_SKIPPED_BY_NAVIGATION,
                finishedDocumentsSkippedByNavigation) //
                        .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT)));

        table = new AnnotationDocumentTable(CID_TABLE,
                LoadableDetachableModel.of(this::listDocuments));
        table.setOutputMarkupId(true);
        queue(table);
    }

    private boolean isFinishedDocumentsSkippedByNavigation()
    {
        var project = getModelObject().getProject();
        var sessionOwner = userRepository.getCurrentUser();
        return preferencesService.loadTraitsForUserAndProject(KEY_ANNOTATION_NAVIGATION_USER_PREFS,
                sessionOwner, project).isFinishedDocumentsSkippedByNavigation();
    }

    private void setFinishedDocumentsSkippedByNavigation(boolean aBoolean)
    {
        var project = getModelObject().getProject();
        var sessionOwner = userRepository.getCurrentUser();
        var prefs = preferencesService.loadTraitsForUserAndProject(
                KEY_ANNOTATION_NAVIGATION_USER_PREFS, sessionOwner, project);
        prefs.setFinishedDocumentsSkippedByNavigation(aBoolean);
        preferencesService.saveTraitsForUserAndProject(KEY_ANNOTATION_NAVIGATION_USER_PREFS,
                sessionOwner, project, prefs);
    }

    private DropDownChoice<DecoratedObject<User>> createUserListChoice(String aId)
    {
        var sessionOwner = userRepository.getCurrentUser();
        var decoratedSessionOwner = DecoratedObject.of(sessionOwner);
        var dataOwner = DecoratedObject.of(getModelObject().getUser());

        var choice = new DropDownChoice<>(aId, Model.of(), listUsers());
        choice.setChoiceRenderer(new ChoiceRenderer<DecoratedObject<User>>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(DecoratedObject<User> aUser)
            {
                var user = aUser.get();
                var username = defaultIfEmpty(aUser.getLabel(), user.getUiName());
                if (user.equals(sessionOwner)) {
                    username += " (me)";
                }
                return username + (user.isEnabled() ? "" : " (deactivated)");
            }
        });
        choice.setOutputMarkupId(true);
        choice.add(visibleWhen(getModel().map(s -> s.getMode().equals(ANNOTATION)
                && projectService.hasRole(sessionOwner, s.getProject(), MANAGER, CURATOR))));
        choice.add(OnChangeAjaxBehavior.onChange(this::actionSelectUser));

        if (choice.getChoices().contains(dataOwner)) {
            choice.setModelObject(dataOwner);
        }
        else if (choice.getChoices().contains(decoratedSessionOwner)) {
            choice.setModelObject(decoratedSessionOwner);
        }
        else if (!choice.getChoices().isEmpty()) {
            choice.setModelObject(choice.getChoices().get(0));
        }
        else {
            choice.setModelObject(null);
        }

        return choice;
    }

    private void actionSelectUser(AjaxRequestTarget aTarget)
    {
        table.getDataProvider().getModel().setObject(listDocuments());

        aTarget.add(table);
    }

    private List<DecoratedObject<User>> listUsers()
    {
        var project = getModelObject().getProject();
        var sessionOwner = userRepository.getCurrentUser();

        var users = new ArrayList<DecoratedObject<User>>();
        // cannot select other user than themselves if curating or not admin
        if (getModelObject().getMode().equals(Mode.CURATION)
                || !projectService.hasRole(sessionOwner, project, MANAGER, CURATOR)) {
            var du = DecoratedObject.of(sessionOwner);
            du.setLabel(sessionOwner.getUiName());
            users.add(du);
            return users;
        }

        for (var user : projectService.listUsersWithRoleInProject(project, ANNOTATOR)) {
            var du = DecoratedObject.of(user);
            du.setLabel(user.getUiName());
            if (user.equals(sessionOwner)) {
                users.add(0, du);
            }
            else {
                users.add(du);
            }
        }

        return users;
    }

    private List<AnnotationDocument> listDocuments()
    {
        var project = getModelObject().getProject();
        var user = userListChoice.getModel().map(DecoratedObject::get).orElse(null).getObject();

        if (project == null || user == null) {
            return new ArrayList<>();
        }

        return docListProvider.apply(project, user);
    }

    @OnEvent
    public void onSourceDocumentOpenDocumentEvent(AnnotationDocumentOpenDocumentEvent aEvent)
    {
        var documents = listDocuments().stream() //
                .map(AnnotationDocument::getDocument) //
                .collect(toList());

        getModelObject().setDocument(aEvent.getAnnotationDocument().getDocument(), documents);

        // for curation view in inception: when curating into CURATION_USER's CAS
        // and opening new document it should also be from the CURATION_USER
        if (getModelObject().getUser() != null
                && !CURATION_USER.equals(getModelObject().getUser().getUsername())) {
            getModelObject().setUser(userListChoice.getModelObject().get());
        }

        ((AnnotationPageBase) getPage()).actionLoadDocument(aEvent.getTarget());

        findParent(ModalDialog.class).close(aEvent.getTarget());
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        userListChoice.detach();

        // If the dialog is aborted without choosing a document, return to a sensible
        // location.
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
