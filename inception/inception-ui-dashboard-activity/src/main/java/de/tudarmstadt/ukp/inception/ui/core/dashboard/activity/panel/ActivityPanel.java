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

import java.util.Map;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.support.svelte.SvelteBehavior;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.activity.ActivitiesDashletController;

public class ActivityPanel
    extends Panel
{
    private static final long serialVersionUID = -1689813207024061423L;

    private @SpringBean ActivitiesDashletController controller;

    public ActivityPanel(String aId, IModel<Project> aModel)
    {
        super(aId);

        var projectId = aModel.map(Project::getId).orElse(-1l).getObject();
        setDefaultModel(Model.ofMap(Map.of( //
                "overviewDataUrl", controller.getActivityOverviewUrl(projectId), //
                "summaryDataUrl", controller.getActivitySummaryUrl(projectId))));

        setOutputMarkupPlaceholderTag(true);

        add(new WebMarkupContainer("content").setOutputMarkupId(true)
                .add(new SvelteBehavior(this)));
    }
}
