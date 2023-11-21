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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.documents;

import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;

/**
 * A Panel used to add Documents to the selected {@link Project}
 */
public class ProjectDocumentsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 2116717853865353733L;

    private @SpringBean DocumentService documentService;

    private final SourceDocumentTable sourceDocumentTable;

    public ProjectDocumentsPanel(String id, IModel<Project> aProject)
    {
        super(id, aProject);

        queue(new ImportDocumentsPanel("import", aProject));
        sourceDocumentTable = new SourceDocumentTable("documents",
                aProject.map(documentService::listSourceDocuments));
        queue(sourceDocumentTable);
    }

    @OnEvent
    public void onSourceDocumentImported(SourceDocumentImportedEvent aEvent)
    {
        sourceDocumentTable.getDataProvider().refresh();
        aEvent.getTarget().add(sourceDocumentTable);
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        sourceDocumentTable.getDataProvider().refresh();
    }
}
