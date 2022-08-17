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

import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableBiFunction;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

/**
 * A panel used as Open dialog. It Lists all projects a user is member of for annotation/curation
 * and associated documents
 */
public class OpenDocumentDialogPanel
    extends Panel
{
    private static final long serialVersionUID = 1299869948010875439L;

    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userRepository;

    private final DropDownChoice<DecoratedObject<User>> userListChoice;
    private final AnnotationDocumentTable table;

    private final IModel<AnnotatorState> state;

    private final SerializableBiFunction<Project, User, List<AnnotationDocument>> docListProvider;

    public OpenDocumentDialogPanel(String aId, IModel<AnnotatorState> aState,
            SerializableBiFunction<Project, User, List<AnnotationDocument>> aDocListProvider)
    {
        super(aId);

        state = aState;
        docListProvider = aDocListProvider;

        queue(userListChoice = createUserListChoice());

        queue(new LambdaAjaxLink("closeDialog", this::actionCancel));

        table = new AnnotationDocumentTable("table",
                LoadableDetachableModel.of(this::listDocuments));
        table.setOutputMarkupId(true);
        queue(table);
    }

    private DropDownChoice<DecoratedObject<User>> createUserListChoice()
    {
        DecoratedObject<User> currentUser = DecoratedObject.of(userRepository.getCurrentUser());
        DecoratedObject<User> viewUser = DecoratedObject.of(state.getObject().getUser());

        var choice = new DropDownChoice<>("user", Model.of(), listUsers());
        choice.setChoiceRenderer(new ChoiceRenderer<DecoratedObject<User>>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(DecoratedObject<User> aUser)
            {
                User user = aUser.get();
                String username = defaultIfEmpty(aUser.getLabel(), user.getUiName());
                if (user.equals(currentUser.get())) {
                    username += " (me)";
                }
                return username + (user.isEnabled() ? "" : " (deactivated)");
            }
        });
        choice.setOutputMarkupId(true);
        choice.add(visibleWhen(state.map(s -> s.getMode().equals(ANNOTATION) && projectService
                .hasRole(userRepository.getCurrentUser(), s.getProject(), MANAGER))));
        choice.add(OnChangeAjaxBehavior.onChange(this::actionSelectUser));

        if (choice.getChoices().contains(viewUser)) {
            choice.setModelObject(viewUser);
        }
        else if (choice.getChoices().contains(currentUser)) {
            choice.setModelObject(currentUser);
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
        List<DecoratedObject<User>> users = new ArrayList<>();

        Project project = state.getObject().getProject();
        User currentUser = userRepository.getCurrentUser();
        // cannot select other user than themselves if curating or not admin
        if (state.getObject().getMode().equals(Mode.CURATION)
                || !projectService.hasRole(currentUser, project, MANAGER)) {
            DecoratedObject<User> du = DecoratedObject.of(currentUser);
            du.setLabel(currentUser.getUiName());
            users.add(du);
            return users;
        }

        for (User user : projectService.listProjectUsersWithPermissions(project, ANNOTATOR)) {
            DecoratedObject<User> du = DecoratedObject.of(user);
            du.setLabel(user.getUiName());
            if (user.equals(currentUser)) {
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
        Project project = state.getObject().getProject();
        User user = userListChoice.getModel().map(DecoratedObject::get).orElse(null).getObject();

        if (project == null || user == null) {
            return new ArrayList<>();
        }

        return docListProvider.apply(project, user);
    }

    @OnEvent
    public void onSourceDocumentOpenDocumentEvent(AnnotationDocumentOpenDocumentEvent aEvent)
    {
        var documents = listDocuments().stream().map(AnnotationDocument::getDocument)
                .collect(toList());

        state.getObject().setDocument(aEvent.getAnnotationDocument().getDocument(), documents);

        // for curation view in inception: when curating into CURATION_USER's CAS
        // and opening new document it should also be from the CURATION_USER
        if (state.getObject().getUser() != null
                && !CURATION_USER.equals(state.getObject().getUser().getUsername())) {
            state.getObject().setUser(userListChoice.getModelObject().get());
        }

        ((AnnotationPageBase) getPage()).actionLoadDocument(aEvent.getTarget());

        findParent(ModalDialog.class).close(aEvent.getTarget());
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        userListChoice.detach();

        // If the dialog is aborted without choosing a document, return to a sensible
        // location.
        if (state.getObject().getProject() == null || state.getObject().getDocument() == null) {
            try {
                ProjectPageBase ppb = findParent(ProjectPageBase.class);
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
