/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.curation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.curation.CurationService;


public class CurationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -4195790451286055737L;
    private static final String DEFAULT_CURATION_TARGET = "my document";
    
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean CurationService curationService;
    private @SpringBean DocumentService documentService;
    
    private CheckGroup<User> selectedUsers;

    private final List<String> curationTargets = Arrays
            .asList(new String[] { DEFAULT_CURATION_TARGET, "curation document" });
    private String selectedCurationTarget = DEFAULT_CURATION_TARGET;
    
    private AnnotatorState state;
    private AnnotationPage annoPage;

    // TODO: only show to people who are curators
    public CurationSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);
        state = aModel.getObject();
//        annoPage = aAnnotationPage;
        WebMarkupContainer mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);
        
        // set up user-checklist
        Form<List<User>> usersForm = createUserSelection();
        mainContainer.add(usersForm);
        
        // set up curation target radio button
        Form<Void> targetForm = new Form<Void>("settingsForm") {
            
            private static final long serialVersionUID = -5535838955781542216L;

            @Override
            protected void onSubmit()
            {
                updateCurator();
            }            
        };
        RadioChoice<String> curationTargetBtn = new RadioChoice<String>("curationTargetRadioBtn",
                new PropertyModel<String>(this, "selectedCurationTarget"), curationTargets);
        targetForm.add(curationTargetBtn);
        mainContainer.add(targetForm);
    }

    private void updateCurator()
    {
        // no change
        if (selectedCurationTarget.equals(state.getUser().getUsername())) {
            return;
        }
        
        // update stored curator 
        long project = state.getProject().getId();
        User curator = userRepository.getCurrentUser();
        String currentUsername = curator.getUsername();
        if (selectedCurationTarget.equals(DEFAULT_CURATION_TARGET)) {
            curationService.updateCurationName(currentUsername,
                    project, currentUsername);
            state.setMode(Mode.ANNOTATION);
        }
        else {
            if (!userRepository.exists(CURATION_USER)) {
                userRepository.create(new User(CURATION_USER, Role.ROLE_USER));
                // TODO: give rights: curator?
            }
            curator = userRepository.get(CURATION_USER);
            if (curator == null) {
                userRepository.create(new User(CURATION_USER));
            }
            curationService.updateCurationName(currentUsername, project, CURATION_USER);
            state.setMode(Mode.CURATION);
        }
        
        // open curation-doc
        state.setUser(curator);
        RequestCycle.get().find(AjaxRequestTarget.class)
                .ifPresent(t -> annoPage.actionRefreshDocument(t));
    }
    
    private Form<List<User>> createUserSelection()
    {
        Form<List<User>> usersForm = new Form<List<User>>("usersForm",
                LoadableDetachableModel.of(this::listSelectedUsers))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit()
            {
                showUsers();
            }

        };
        selectedUsers = new CheckGroup<User>("selectedUsers", usersForm.getModelObject());
        ListView<User> users = new ListView<User>("users",
                LoadableDetachableModel.of(this::listUsers))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<User> aItem)
            {
                aItem.add(new Check<User>("user", aItem.getModel()));
                aItem.add(new Label("name", aItem.getModelObject().getUsername()));

            }
        };
        selectedUsers.add(users);
        usersForm.add(selectedUsers);
        usersForm.add(visibleWhen(() -> !users.getModelObject().isEmpty()));
        return usersForm;
    }
    
    private List<User> listSelectedUsers()
    {
        Optional<List<User>> users = curationService.listUsersSelectedForCuration(
                userRepository.getCurrentUser().getUsername(), state.getProject().getId());
        if (!users.isPresent()) {
            return new ArrayList<>();
        }
        return users.get();
    }
    
    /**
     * retrieve annotators of this document which finished annotating
     */
    private List<User> listUsers()
    {
        return projectService
                .listProjectUsersWithPermissions(state.getProject(), PermissionLevel.ANNOTATOR)
                .stream().filter(user -> !user.equals(userRepository.getCurrentUser()) 
                        && hasFinishedDoc(user))
                .collect(Collectors.toList());
    }

    private boolean hasFinishedDoc(User aUser)
    {
        SourceDocument doc = state.getDocument();
        String username = aUser.getUsername();
        if (documentService.existsAnnotationDocument(doc, username) && 
                documentService.getAnnotationDocument(doc, username).getState()
                .equals(AnnotationDocumentState.FINISHED)) {
            return true;
        }
        else {
            return false;
        }
    }

    private void showUsers()
    {
        Collection<User> users = selectedUsers.getModelObject();
        curationService.updateUsersSelectedForCuration(
                userRepository.getCurrentUser().getUsername(), state.getProject().getId(), users);
        // refresh should call render of PreRenderer and render of editor-extensions ?
        //annoPage.actionRefreshDocument(aTarget);
    }

}
