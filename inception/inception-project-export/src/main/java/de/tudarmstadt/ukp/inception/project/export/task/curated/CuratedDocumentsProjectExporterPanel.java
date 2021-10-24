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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.project.export.settings.ProjectExporterPanelImplBase;

public class CuratedDocumentsProjectExporterPanel
    extends ProjectExporterPanelImplBase

{
    private static final long serialVersionUID = 4106224145358319779L;

    private @SpringBean ProjectExportService projectExportService;

    public CuratedDocumentsProjectExporterPanel(String aId,
            IModel<CuratedDocumentsProjectExportRequest> aModel)
    {
        super(aId);

        setDefaultModel(aModel);

        add(new LambdaAjaxLink("startExport", this::actionStartExport));
    }

    @SuppressWarnings("unchecked")
    public IModel<CuratedDocumentsProjectExportRequest> getModel()
    {
        return (IModel<CuratedDocumentsProjectExportRequest>) getDefaultModel();
    }

    public CuratedDocumentsProjectExportRequest getModelObject()
    {
        return (CuratedDocumentsProjectExportRequest) getDefaultModelObject();
    }

    private void actionStartExport(AjaxRequestTarget aTarget)
    {
        var request = getModelObject();
        request.setFilenameTag("_project");

        CuratedDocumentsProjectExportTask task = new CuratedDocumentsProjectExportTask(request,
                SecurityContextHolder.getContext().getAuthentication().getName());

        projectExportService.startTask(task);
    }
}
