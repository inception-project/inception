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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.activity.panel;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.LambdaChoiceRenderer;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.svelte.SvelteBehavior;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.activity.ActivitiesDashletController;

public class ActivityPanel
    extends GenericPanel<Map<String, String>>
{
    private static final long serialVersionUID = -1689813207024061423L;

    private @SpringBean ActivitiesDashletController controller;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userService;
    private @SpringBean DocumentService documentService;

    private IModel<Project> projectModel = Model.of();
    private IModel<AnnotationSet> userModel = Model.of();

    private DropDownChoice<AnnotationSet> userSelection;

    private WebMarkupContainer content;

    public ActivityPanel(String aId, IModel<Project> aModel)
    {
        super(aId);

        projectModel = aModel;
        var sessionOwner = userService.getCurrentUser();
        var isManager = projectService.hasRole(sessionOwner, projectModel.getObject(), MANAGER);

        var projectId = aModel.map(Project::getId).orElse(-1l).getObject();
        setDefaultModel(Model.ofMap(new HashMap<>(Map.of( //
                "overviewDataUrl", controller.getActivityOverviewUrl(projectId), //
                "summaryDataUrl", controller.getActivitySummaryUrl(projectId)))));

        setOutputMarkupPlaceholderTag(true);

        content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        content.add(new SvelteBehavior(this));
        add(content);

        userSelection = new DropDownChoice<AnnotationSet>("user");
        userSelection.setOutputMarkupId(true);
        userSelection.setModel(userModel);
        userSelection.setChoiceRenderer(new LambdaChoiceRenderer<>(dataOwner -> {
            var label = dataOwner.displayName();
            if (dataOwner.id().equals(sessionOwner.getUsername())) {
                label += " (me)";
            }
            return label;
        }));
        userSelection.setChoices(this::getDataOwnersForCurrentProject);
        userSelection.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionChangeDataOwner));
        userSelection.setVisible(isManager);

        var dataOwners = userSelection.getChoices();
        var sessionOwnerSet = AnnotationSet.forUser(sessionOwner);
        if (dataOwners.contains(sessionOwnerSet)) {
            userModel.setObject(sessionOwnerSet);
        }
        else if (!dataOwners.isEmpty()) {
            userModel.setObject(dataOwners.get(0));
        }

        add(userSelection);
    }

    private void actionChangeDataOwner(AjaxRequestTarget aTarget)
    {
        getModelObject().put("dataOwner",
                userModel.getObject() != null ? userModel.getObject().id() : null);
        aTarget.add(userSelection, content);
    }

    private List<AnnotationSet> getDataOwnersForCurrentProject()
    {
        var project = projectModel.getObject();

        // All users currently holding any role in the project, plus the curation user.
        var dataOwners = new LinkedHashMap<String, AnnotationSet>();
        for (var user : projectService.listUsersWithAnyRoleInProject(project)) {
            dataOwners.put(user.getUsername(), AnnotationSet.forUser(user));
        }
        var curationUser = userService.getCurationUser();
        dataOwners.put(curationUser.getUsername(), AnnotationSet.forUser(curationUser));

        // Former annotators that left annotation data behind but no longer hold a role (removed
        // from the project, role changed or account deleted) so their activity stays accessible.
        for (var dataOwner : documentService.listDataOwners(project)) {
            dataOwners.putIfAbsent(dataOwner.id(), dataOwner);
        }

        return new ArrayList<>(dataOwners.values());
    }
}
