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
package de.tudarmstadt.ukp.inception.project.export.task.curated;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportExtension;
import de.tudarmstadt.ukp.inception.project.export.config.ProjectExportServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectExportServiceAutoConfiguration#curatedDocumentsProjectExportExtension}.
 * </p>
 */
public class CuratedDocumentsProjectExportExtension
    implements ProjectExportExtension
{
    public static final String ID = "curated";

    private final DocumentService documentService;

    public CuratedDocumentsProjectExportExtension(DocumentService aDocumentService)
    {
        documentService = aDocumentService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return documentService.existsCurationDocument(aContext);
    }

    @Override
    public Panel createExporterPanel(String aId, IModel<Project> aProject)
    {
        var request = new CuratedDocumentsProjectExportRequest(aProject.getObject());

        return new CuratedDocumentsProjectExporterPanel(aId, Model.of(request));
    }
}
