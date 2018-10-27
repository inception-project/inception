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

    private static final String DETAILS_PANEL_MARKUP_ID = "details";

    private IModel<Project> projectModel;
    private Panel detailsPanel;

    public ProjectKnowledgeBasePanel(String aId, IModel<Project> aProject)
    {
        super(aId);
        setOutputMarkupId(true);
        projectModel = aProject;

        detailsPanel = new EmptyPanel(DETAILS_PANEL_MARKUP_ID);
        add(detailsPanel);

        IModel<KnowledgeBase> kbModel = Model.of();
        KnowledgeBaseListPanel listPanel = new KnowledgeBaseListPanel("list", projectModel,
                kbModel);
        listPanel.setChangeAction(t -> {
            addOrReplace(detailsPanel);
            detailsPanel
                    .replaceWith(new KnowledgeBaseDetailsPanel(DETAILS_PANEL_MARKUP_ID, kbModel));
            t.add(this);
        });
        add(listPanel);
    }
}
