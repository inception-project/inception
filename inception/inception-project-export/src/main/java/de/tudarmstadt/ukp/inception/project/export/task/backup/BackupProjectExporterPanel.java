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
package de.tudarmstadt.ukp.inception.project.export.task.backup;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.project.export.settings.FormatDropdownChoice;
import de.tudarmstadt.ukp.inception.project.export.settings.ProjectExporterPanelImplBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class BackupProjectExporterPanel
    extends ProjectExporterPanelImplBase
{
    private static final long serialVersionUID = 4106224145358319779L;

    private @SpringBean ProjectExportService projectExportService;
    private @SpringBean DocumentImportExportService importExportService;

    public BackupProjectExporterPanel(String aId, IModel<Project> aModel)
    {
        super(aId);

        CompoundPropertyModel<FullProjectExportRequest> model = CompoundPropertyModel
                .of(new FullProjectExportRequest(aModel.getObject(), null, true));

        setDefaultModel(model);

        DropDownChoice<String> format = new FormatDropdownChoice("format", model.bind("format"));
        format.setNullValid(true);
        format.add(new LambdaAjaxFormComponentUpdatingBehavior());
        add(format);

        add(new LambdaAjaxLink("startExport", this::actionStartExport));
    }

    @SuppressWarnings("unchecked")
    public IModel<FullProjectExportRequest> getModel()
    {
        return (IModel<FullProjectExportRequest>) getDefaultModel();
    }

    public FullProjectExportRequest getModelObject()
    {
        return (FullProjectExportRequest) getDefaultModelObject();
    }

    private void actionStartExport(AjaxRequestTarget aTarget)
    {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var request = getModelObject();
        var task = new BackupProjectExportTask(request, authentication.getName());

        projectExportService.startTask(task);
    }
}
