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

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectAccess;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.documents.ProjectDocumentsPage;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.layers.ProjectLayersPage;

public class LayerHintDashlet
    extends Dashlet_ImplBase
{
    private static final long serialVersionUID = -2039509339561190091L;

    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean ProjectAccess projectAccess;

    public LayerHintDashlet(String aId, IModel<Project> aProject)
    {
        super(aId, aProject);

        add(new BookmarkablePageLink<Void>("jumpToLayers", ProjectLayersPage.class,
                new PageParameters().set(ProjectDocumentsPage.PAGE_PARAM_PROJECT,
                        aProject.getObject().getSlug())));
    }

    public Project getModelObject()
    {
        return (Project) getDefaultModelObject();
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(projectAccess.canManageProject(Long.toString(getModelObject().getId()))
                && !schemaService.existsLayer(getModelObject()));
    }
}
