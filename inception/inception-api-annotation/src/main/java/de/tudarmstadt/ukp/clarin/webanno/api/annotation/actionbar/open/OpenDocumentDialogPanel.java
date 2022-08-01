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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.open;

import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableBiFunction;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;
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

    private final WebMarkupContainer buttonsContainer;
    private final OverviewListChoice<DecoratedObject<SourceDocument>> docListChoice;
    private final OverviewListChoice<DecoratedObject<User>> userListChoice;
    private final LambdaAjaxButton<Void> openButton;

    private final IModel<AnnotatorState> state;

    private final SerializableBiFunction<Project, User, List<DecoratedObject<SourceDocument>>> docListProvider;

    public OpenDocumentDialogPanel(String aId, IModel<AnnotatorState> aState,
            SerializableBiFunction<Project, User, List<DecoratedObject<SourceDocument>>> aDocListProvider)
    {
        super(aId);

        state = aState;
        docListProvider = aDocListProvider;

        queue(userListChoice = createUserListChoice(aState));
        queue(docListChoice = createDocListChoice());

        openButton = new LambdaAjaxButton<>("openButton", this::actionOpenDocument);
        openButton.add(enabledWhen(() -> docListChoice.getModelObject() != null));
        queue(openButton);

        queue(new LambdaAjaxLink("cancelButton", this::actionCancel));
        queue(new LambdaAjaxLink("closeDialog", this::actionCancel));

        buttonsContainer = new WebMarkupContainer("buttons");
        buttonsContainer.setOutputMarkupId(true);
        queue(buttonsContainer);

        Form<Void> form = new Form<>("form");
        form.setOutputMarkupId(true);
        form.setDefaultButton(openButton);
        queue(form);
    }

    private OverviewListChoice<DecoratedObject<SourceDocument>> createDocListChoice()
    {
        var choice = new OverviewListChoice<>("documents", Model.of(), listDocuments());
        choice.setChoiceRenderer(new ChoiceRenderer<DecoratedObject<SourceDocument>>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(DecoratedObject<SourceDocument> aDoc)
            {
                return defaultIfEmpty(aDoc.getLabel(), aDoc.get().getName());
            }
        });
        choice.setOutputMarkupId(true);
        choice.add(new OnChangeAjaxBehavior()
        {
            private static final long serialVersionUID = -8232688660762056913L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                aTarget.add(buttonsContainer);
            }
        }).add(AjaxEventBehavior.onEvent("dblclick", _target -> actionOpenDocument(_target, null)));

        if (!choice.getChoices().isEmpty()) {
            choice.setModelObject(choice.getChoices().get(0));
        }

        choice.setDisplayMessageOnEmptyChoice(true);
        return choice;
    }

    private OverviewListChoice<DecoratedObject<User>> createUserListChoice(
            IModel<AnnotatorState> aState)
    {
        var state = aState.getObject();
        DecoratedObject<User> currentUser = DecoratedObject.of(userRepository.getCurrentUser());
        DecoratedObject<User> viewUser = DecoratedObject.of(state.getUser());

        var choice = new OverviewListChoice<>("user", Model.of(), listUsers());
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
                return username + (user.isEnabled() ? "" : " (login disabled)");
            }
        });
        choice.setOutputMarkupId(true);
        choice.add(visibleWhen(() -> state.getMode().equals(ANNOTATION) && projectService
                .hasRole(userRepository.getCurrentUser(), state.getProject(), MANAGER)));
        choice.add(new OnChangeAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                docListChoice.setChoices(listDocuments());

                if (!docListChoice.getChoices().isEmpty()) {
                    docListChoice.setModelObject(docListChoice.getChoices().get(0));
                }
                else {
                    docListChoice.setModelObject(null);
                }

                aTarget.add(buttonsContainer);
                aTarget.add(docListChoice);
            }
        });

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

    private List<DecoratedObject<SourceDocument>> listDocuments()
    {
        Project project = state.getObject().getProject();
        User user = userListChoice.getModel().map(DecoratedObject::get).orElse(null).getObject();

        if (project == null || user == null) {
            return new ArrayList<>();
        }

        return docListProvider.apply(project, user);
    }

    private void actionOpenDocument(AjaxRequestTarget aTarget, Form<?> aForm)
    {
        if (docListChoice.getModelObject() == null) {
            return;
        }

        state.getObject().setDocument(docListChoice.getModelObject().get(),
                docListChoice.getChoices().stream().map(t -> t.get()).collect(Collectors.toList()));

        // for curation view in inception: when curating into CURATION_USER's CAS
        // and opening new document it should also be from the CURATION_USER
        if (state.getObject().getUser() != null
                && !CURATION_USER.equals(state.getObject().getUser().getUsername())) {
            state.getObject().setUser(userListChoice.getModelObject().get());
        }

        ((AnnotationPageBase) getPage()).actionLoadDocument(aTarget);

        findParent(ModalDialog.class).close(aTarget);
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        userListChoice.detach();
        docListChoice.detach();

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

    public Component getFocusComponent()
    {
        return docListChoice;
    }
}
