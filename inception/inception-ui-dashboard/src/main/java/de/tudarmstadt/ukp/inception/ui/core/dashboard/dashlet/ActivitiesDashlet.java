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

import static de.tudarmstadt.ukp.inception.support.dayjs.DayJsResourceReference.DayJsPlugin.LOCALIZED_FORMAT;
import static de.tudarmstadt.ukp.inception.support.dayjs.DayJsResourceReference.DayJsPlugin.RELATIVE_TIME;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.Map;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.support.axios.AxiosResourceReference;
import de.tudarmstadt.ukp.inception.support.dayjs.DayJsResourceReference;
import de.tudarmstadt.ukp.inception.support.vue.VueBehavior;

public class ActivitiesDashlet
    extends WebMarkupContainer
{
    private static final long serialVersionUID = -2010294259619748756L;

    private @SpringBean ActivitiesDashletController controller;

    public ActivitiesDashlet(String aId, IModel<Project> aCurrentProject)
    {
        super(aId);

        long projectId = aCurrentProject.map(Project::getId).orElse(-1l).getObject();
        setDefaultModel(Model.ofMap(Map.of("dataUrl", controller.listActivitiesUrl(projectId))));

        setOutputMarkupPlaceholderTag(true);

        add(new VueBehavior(
                new PackageResourceReference(getClass(), getClass().getSimpleName() + ".vue")));
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(new DayJsResourceReference(RELATIVE_TIME, LOCALIZED_FORMAT)));
        aResponse.render(forReference(AxiosResourceReference.get()));
    }
}
