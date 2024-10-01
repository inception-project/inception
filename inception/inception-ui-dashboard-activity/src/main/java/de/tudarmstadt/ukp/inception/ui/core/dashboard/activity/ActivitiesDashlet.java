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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.activity;

import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.svelte.SvelteBehavior;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.activity.panel.ActivityPanel;

public class ActivitiesDashlet
    extends Panel
{
    private static final long serialVersionUID = -2010294259619748756L;

    private @SpringBean ActivitiesDashletController controller;

    private IModel<Project> project;
    private BootstrapModalDialog dialog;

    public ActivitiesDashlet(String aId, IModel<Project> aProject)
    {
        super(aId);

        project = aProject;

        var projectId = aProject.map(Project::getId).orElse(-1l).getObject();
        setDefaultModel(Model.ofMap(Map.of("dataUrl", controller.getListActivitiesUrl(projectId))));

        setOutputMarkupPlaceholderTag(true);

        add(new WebMarkupContainer("content").setOutputMarkupId(true)
                .add(new SvelteBehavior(this)));

        dialog = new BootstrapModalDialog("dialog");
        dialog.closeOnClick();
        dialog.closeOnEscape();
        add(dialog);

        add(new LambdaAjaxLink("showActivity", this::actionShowActivity));
    }

    public void actionShowActivity(AjaxRequestTarget aTarget)
    {
        var dialogContent = new ActivityPanel(BootstrapModalDialog.CONTENT_ID, project);

        dialog.open(dialogContent, aTarget);
    }
}
