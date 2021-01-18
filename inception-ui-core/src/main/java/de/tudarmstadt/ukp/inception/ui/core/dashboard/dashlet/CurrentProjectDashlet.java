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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.clipboardjs.ClipboardJsBehavior;

import com.github.rjeschke.txtmark.Processor;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;


public class CurrentProjectDashlet
    extends Dashlet_ImplBase
{
    private static final long serialVersionUID = 7732921923832675326L;

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    
    private final WebMarkupContainer inviteLinkContainer;
    private final WebMarkupContainer mainContainer;
    private TextField<String> linkField;

    public CurrentProjectDashlet(String aId, IModel<Project> aCurrentProject)
    {
        super(aId, aCurrentProject);

        AjaxEditableLabel<String> name = new AjaxEditableLabel<String>("name",
                PropertyModel.of(aCurrentProject, "name"))
        {
            private static final long serialVersionUID = -2867104998844713915L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                projectService.updateProject(aCurrentProject.getObject());
                success("Project name was updated");
                aTarget.addChildren(getPage(), IFeedback.class);
                // Put the component back into label mode
                onCancel(aTarget);
            }
        };
        boolean isManager = projectService.isManager(getModelObject(), userRepository.getCurrentUser());
        name.setEnabled(isManager);
        add(name);
        
        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);
        
        inviteLinkContainer = createInviteLinkContainer();
        inviteLinkContainer.add(visibleWhen(() -> isManager && linkField.getModelObject() != null));
        inviteLinkContainer.setOutputMarkupId(true);
        mainContainer.add(inviteLinkContainer);
        
        AjaxLink<Void> shareBtn = new AjaxLink<>("shareProject") 
        {
            private static final long serialVersionUID = 1L;
            
            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                shareProject();
                aTarget.add(mainContainer);
            }
            
        };
        shareBtn.add(visibleWhen(() -> !inviteLinkContainer.isVisible()));
        mainContainer.add(shareBtn);

        add(new Label("description", LoadableDetachableModel.of(this::getProjectDescription))
                .setEscapeModelStrings(false));
    }

    private WebMarkupContainer createInviteLinkContainer()
    {
        WebMarkupContainer linkContainer = new WebMarkupContainer("invitelinkContainer");
        linkContainer.setOutputMarkupId(true);
        
        linkField = new TextField<>("linkText", LoadableDetachableModel.of(this::getInviteLink));  
        linkContainer.add(linkField);
        
        Button copyBtn = new Button("copy");
        ClipboardJsBehavior clipboardBehavior = new ClipboardJsBehavior();
        clipboardBehavior.setTarget(linkField);
        copyBtn.add(clipboardBehavior);
        linkContainer.add(copyBtn);
        AjaxLink<Void> regenBtn = new AjaxLink<>("regen")
        {

            private static final long serialVersionUID = 8558630925669881073L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                shareProject();
                aTarget.add(linkContainer);
            }

        };
        linkContainer.add(regenBtn);//linkForm.add(regenBtn);
        AjaxLink<Void> removeBtn = new AjaxLink<>("remove")
        {
            private static final long serialVersionUID = 4847153359605500314L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                removeInviteLink();
                aTarget.add(mainContainer);
            }

        };
        linkContainer.add(removeBtn);//linkForm.add(removeBtn);
        return linkContainer;
    }
    
    private void shareProject() {
        projectService.generateInviteID(getModelObject()); 
    }
    
    private void removeInviteLink() {
        projectService.removeInviteID(getModelObject());
    }
    
    private String getInviteLink() {
        Long projectId = getModelObject().getId();
        String inviteId = projectService.getValidInviteID(projectId);

        if (inviteId == null) {
            return null;
        }
        
        PageParameters pageParams = new PageParameters();
        pageParams.add(ProjectDashboardPage.PAGE_PARAM_INVITE_ID, inviteId);
        pageParams.add(PAGE_PARAM_PROJECT_ID, projectId);
        String fullUrl = RequestCycle.get().getUrlRenderer().renderFullUrl(
                Url.parse(RequestCycle.get().urlFor(ProjectDashboardPage.class, pageParams)));
        return fullUrl;
    }

    public Project getModelObject()
    {
        return getModel().orElse(null).getObject();
    }

    public IModel<Project> getModel()
    {
        return (IModel<Project>) getDefaultModel();
    }

    private String getProjectDescription()
    {
        Project project = getModelObject();
        if (project != null) {
            if (StringUtils.isBlank(project.getDescription())) {
                return "Project has no description.";
            }
            else {
                return Processor.process(project.getDescription(), true);
            }
        }
        else {
            return "Please select a project from the drop-down list above.";
        }
    }
}
