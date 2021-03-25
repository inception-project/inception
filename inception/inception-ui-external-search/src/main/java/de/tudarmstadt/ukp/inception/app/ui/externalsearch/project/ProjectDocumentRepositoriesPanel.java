/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch.project;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class ProjectDocumentRepositoriesPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 3042218455285633439L;

    private IModel<Project> projectModel;
    private IModel<DocumentRepository> selectedDocumentRepository;

    public ProjectDocumentRepositoriesPanel(String aId, IModel<Project> aProject)
    {
        super(aId);

        setOutputMarkupId(true);

        selectedDocumentRepository = Model.of();
        projectModel = aProject;

        DocumentRepositoryEditorPanel externalSearchProviderEditorPanel = new DocumentRepositoryEditorPanel(
                "documentRepositoryEditor", projectModel, selectedDocumentRepository);
        add(externalSearchProviderEditorPanel);

        DocumentRepositoryListPanel documentRepositoryListPanel = new DocumentRepositoryListPanel(
                "documentRepositoryList", projectModel, selectedDocumentRepository);
        documentRepositoryListPanel.setCreateAction(_target -> {
            externalSearchProviderEditorPanel.modelChanged();
            selectedDocumentRepository.setObject(new DocumentRepository());
        });
        documentRepositoryListPanel.setChangeAction(_target -> {
            externalSearchProviderEditorPanel.modelChanged();
            _target.add(externalSearchProviderEditorPanel);
        });
        add(documentRepositoryListPanel);
    }
}
