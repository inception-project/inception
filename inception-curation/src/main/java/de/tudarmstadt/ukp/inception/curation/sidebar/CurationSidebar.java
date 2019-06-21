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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;

public class CurationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -4195790451286055737L;
    
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    
    private CheckBoxMultipleChoice<User> selectedUsers;
    private final WebMarkupContainer mainContainer;
    
    private AnnotatorState state;
    private AnnotationPage annoPage;

    public CurationSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);
        state = aModel.getObject();
        annoPage = aAnnotationPage;
        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);
        
        // TODO: put in listview ?
        selectedUsers = new CheckBoxMultipleChoice<User>("users", Model.of(), listUsers());
        selectedUsers.setChoiceRenderer(new ChoiceRenderer<User>() {

            private static final long serialVersionUID = -8165699251116827372L;

            @Override
            public Object getDisplayValue(User aUser)
            {
                return aUser.getUsername();
            }
            
        });
        selectedUsers.setOutputMarkupId(true);
        
        Form<Void> usersForm = new Form<Void>("usersForm");
        usersForm.add(new LambdaAjaxLink("showUsers", this::showUsers));
        usersForm.add(selectedUsers);
        mainContainer.add(usersForm);
    }
    
    private List<? extends User> listUsers()
    {
        return projectService
                .listProjectUsersWithPermissions(state.getProject(), PermissionLevel.ANNOTATOR)
                .stream().filter(user -> !user.equals(userRepository.getCurrentUser()))
                .collect(Collectors.toList());
    }

    private void showUsers(AjaxRequestTarget aTarget)
    {
        // TODO Auto-generated method stub
        // refresh should call render of PreRenderer and render of editor-extensions ?
        annoPage.actionRefreshDocument(aTarget);
    }

}
