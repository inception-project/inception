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

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.inception.io.xmi.XmiFormatSupport;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.project.export.settings.FormatDropdownChoice;
import de.tudarmstadt.ukp.inception.project.export.settings.ProjectExporterPanelImplBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;

public class CuratedDocumentsProjectExporterPanel
    extends ProjectExporterPanelImplBase

{
    private static final long serialVersionUID = 4106224145358319779L;

    private @SpringBean ProjectExportService projectExportService;

    public CuratedDocumentsProjectExporterPanel(String aId,
            IModel<CuratedDocumentsProjectExportRequest> aModel)
    {
        super(aId);

        CompoundPropertyModel<CuratedDocumentsProjectExportRequest> model = CompoundPropertyModel
                .of(aModel);

        setDefaultModel(model);

        DropDownChoice<String> format = new FormatDropdownChoice("format", model.bind("format"));
        format.add(new LambdaAjaxFormComponentUpdatingBehavior());
        format.add(LambdaBehavior.onConfigure(
                _comp -> model.getObject().setFormat(getDefaultFormat(format.getChoices()))));
        add(format);

        ;

        add(new LambdaAjaxLink("startExport", this::actionStartExport));
    }

    private String getDefaultFormat(List<? extends String> aFormats)
    {
        if (aFormats.contains(XmiFormatSupport.ID)) {
            return XmiFormatSupport.ID;
        }

        if (aFormats.contains(FullProjectExportRequest.FORMAT_AUTO)) {
            return FullProjectExportRequest.FORMAT_AUTO;
        }

        if (aFormats.isEmpty()) {
            return null;
        }

        return aFormats.get(0);
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

        var task = new CuratedDocumentsProjectExportTask(request,
                SecurityContextHolder.getContext().getAuthentication().getName());

        projectExportService.startTask(task);
    }
}
