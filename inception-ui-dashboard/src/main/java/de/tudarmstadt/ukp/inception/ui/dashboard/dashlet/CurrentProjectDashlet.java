/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.ui.dashboard.dashlet;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.app.session.SessionMetaData;

public class CurrentProjectDashlet
    extends Dashlet_ImplBase
{
    private static final long serialVersionUID = 7732921923832675326L;

    public CurrentProjectDashlet(String aId)
    {
        super(aId);
        
        add(new Label("name", LambdaModel.of(() -> PropertyModel
                .of(Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT), "name"))));
        
        add(new Label("description", LambdaModel.of(this::getProjectDescription)));
    }
    
    private String getProjectDescription()
    {
        Project project = Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT);
        if (project != null) {
            return project.getDescription();
        }
        else {
            return "Please select a project.";
        }
    }
}
