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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.curation.CurationEditorExtension;

public class CurationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -4195790451286055737L;
    
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    
    private CheckGroup<User> selectedUsers;
    
    private AnnotatorState state;
//    private AnnotationPage annoPage;

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
        Form<List<User>> usersForm = new Form<List<User>>("usersForm",
                new ListModel<User>(new ArrayList<>()))
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
        mainContainer.add(usersForm);
    }
    
    private List<User> listUsers()
    {
        return projectService
                .listProjectUsersWithPermissions(state.getProject(), PermissionLevel.ANNOTATOR)
                .stream().filter(user -> !user.equals(userRepository.getCurrentUser()))
                .collect(Collectors.toList());
    }

    private void showUsers()
    {
        ((CurationEditorExtension) extensionRegistry
                .getExtension(CurationEditorExtension.EXTENSION_ID))
                        .selectedUsersChanged(getModelObject(), selectedUsers.getModelObject());
        // refresh should call render of PreRenderer and render of editor-extensions ?
        //annoPage.actionRefreshDocument(aTarget);
    }

}
