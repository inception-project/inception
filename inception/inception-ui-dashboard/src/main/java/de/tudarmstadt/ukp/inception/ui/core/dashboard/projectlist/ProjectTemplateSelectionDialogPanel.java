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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;

public class ProjectTemplateSelectionDialogPanel
    extends Panel
{
    private static final long serialVersionUID = 2112018755924139726L;

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final PackageResourceReference NO_THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "no-thumbnail.svg");

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private LambdaAjaxLink closeDialogButton;

    public ProjectTemplateSelectionDialogPanel(String aId)
    {
        super(aId);

        var initializers = new ListView<QuickProjectInitializer>("templates",
                LambdaModel.of(this::listInitializers))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<QuickProjectInitializer> aItem)
            {
                aItem.queue(new LambdaAjaxLink("create",
                        _target -> actionCreateProject(_target, aItem.getModelObject())));
                aItem.queue(new Label("name", aItem.getModel().map(ProjectInitializer::getName)));
                aItem.queue(new Label("description",
                        aItem.getModel().map(QuickProjectInitializer::getDescription)
                                .map($ -> $.orElse("No description"))));
                aItem.queue(new Image("thumbnail",
                        aItem.getModel().map(QuickProjectInitializer::getThumbnail)
                                .map($ -> $.orElse(NO_THUMBNAIL))));
            }
        };
        queue(initializers);

        var container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        queue(container);

        closeDialogButton = new LambdaAjaxLink("closeDialog", this::actionCancel);
        closeDialogButton.setOutputMarkupId(true);
        queue(closeDialogButton);
    }

    private List<QuickProjectInitializer> listInitializers()
    {
        return projectService.listProjectInitializers().stream()
                .filter(initializer -> initializer instanceof QuickProjectInitializer)
                .map(initializer -> (QuickProjectInitializer) initializer) //
                .toList();
    }

    private void actionCreateProject(AjaxRequestTarget aTarget, ProjectInitializer aInitializer)
    {
        var user = userRepository.getCurrentUser();
        aTarget.addChildren(getPage(), IFeedback.class);
        var projectSlug = projectService.deriveSlugFromName(user.getUsername());
        projectSlug = projectService.deriveUniqueSlug(projectSlug);

        try {
            var project = new Project(projectSlug);
            project.setName(user.getUsername() + " - New project");
            projectService.createProject(project);
            projectService.assignRole(project, user, ANNOTATOR, CURATOR, MANAGER);
            projectService.initializeProject(project, asList(aInitializer));

            var pageParameters = new PageParameters();
            ProjectPageBase.setProjectPageParameter(pageParameters, project);
            setResponsePage(ProjectDashboardPage.class, pageParameters);
        }
        catch (IOException e) {
            LOG.error("Unable to create project [{}]", projectSlug, e);
            error("Unable to create project [" + projectSlug + "]");
        }
    }

    protected void actionCancel(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }
}
