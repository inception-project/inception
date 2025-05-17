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

import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.api.ProjectAccess;

@Order(100)
@Component
public class DocumentHintDashletExtension
    implements ProjectDashboardDashletExtension
{
    private @SpringBean ProjectAccess projectAccess;

    @Override
    public String getId()
    {
        return "noDocuments";
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return projectAccess.canManageProject(String.valueOf(aContext.getId()));
    }

    @Override
    public DocumentHintDashlet createDashlet(String aId, IModel<Project> aModel)
    {
        return new DocumentHintDashlet(aId, aModel);
    }
}
