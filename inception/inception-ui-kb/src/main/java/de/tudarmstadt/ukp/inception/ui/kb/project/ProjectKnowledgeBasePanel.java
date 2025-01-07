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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class ProjectKnowledgeBasePanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 2300684106019320208L;

    private static final String CID_DETAILS = "details";

    private IModel<Project> projectModel;
    private IModel<KnowledgeBase> selectedKnowledgeBaseModel;
    private Panel detailsPanel;

    public ProjectKnowledgeBasePanel(String aId, final IModel<Project> aProject)
    {
        super(aId, aProject);

        setOutputMarkupId(true);
        projectModel = aProject;

        detailsPanel = new EmptyPanel(CID_DETAILS);
        add(detailsPanel);

        selectedKnowledgeBaseModel = Model.of();
        var listPanel = new KnowledgeBaseListPanel("list", projectModel,
                selectedKnowledgeBaseModel);
        listPanel.setChangeAction(t -> {
            addOrReplace(detailsPanel);
            detailsPanel.replaceWith(
                    new KnowledgeBaseDetailsPanel(CID_DETAILS, selectedKnowledgeBaseModel));
            t.add(this);
        });
        add(listPanel);
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        selectedKnowledgeBaseModel.setObject(null);
    }
}
