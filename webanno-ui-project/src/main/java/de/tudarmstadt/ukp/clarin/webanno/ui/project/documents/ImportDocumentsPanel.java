/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.documents;

import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;

public class ImportDocumentsPanel extends Panel
{
    private static final long serialVersionUID = 4927011191395114886L;
    
    private final static Logger LOG = LoggerFactory.getLogger(ImportDocumentsPanel.class);
    
    private @SpringBean DocumentService documentService;
    private @SpringBean ImportExportService importExportService;
    
    private FileUploadField fileUpload;

    private IModel<String> format;
    
    private IModel<Project> projectModel;

    public ImportDocumentsPanel(String aId, IModel<Project> aProject)
    {
        super(aId);

        projectModel = aProject;
        
        Form<Void> form = new Form<>("form");
        add(form);
        
        format = Model.of();
        List<String> readableFormats = listReadableFormats();
        if (!readableFormats.isEmpty()) {
            if (readableFormats.contains("Plain text")) {
                format.setObject("Plain text");
            }
            else {
                format.setObject(readableFormats.get(0));
            }
        }
        
        form.add(fileUpload = new FileUploadField("documents"));

        DropDownChoice<String> formats = new DropDownChoice<String>("format");
        formats.setModel(format);
        formats.setChoices(LambdaModel.of(this::listReadableFormats));
        form.add(formats);
        
        form.add(new LambdaAjaxButton<>("import", this::actionImport));
    }
    
    private List<String> listReadableFormats()
    {
        return importExportService.getReadableFormats().stream().map(FormatSupport::getName)
                .sorted().collect(Collectors.toList());
    }

    private void actionImport(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        
        List<FileUpload> uploadedFiles = fileUpload.getFileUploads();
        Project project = projectModel.getObject();
        if (isEmpty(uploadedFiles)) {
            error("No document is selected to upload, please select a document first");
            return;
        }
        if (isNull(project.getId())) {
            error("Project not yet created, please save project details!");
            return;
        }

        for (FileUpload documentToUpload : uploadedFiles) {
            String fileName = documentToUpload.getClientFileName();

            if (documentService.existsSourceDocument(project, fileName)) {
                error("Document " + fileName + " already uploaded ! Delete "
                        + "the document if you want to upload again");
                continue;
            }

            try {
                SourceDocument document = new SourceDocument();
                document.setName(fileName);
                document.setProject(project);
                document.setFormat(importExportService.getFormatByName(format.getObject())
                        .get().getId());
                
                try (InputStream is = documentToUpload.getInputStream()) {
                    documentService.uploadSourceDocument(is, document);
                }
                info("File [" + fileName + "] has been imported successfully!");
            }
            catch (Exception e) {
                error("Error while uploading document " + fileName + ": "
                    + ExceptionUtils.getRootCauseMessage(e));
                LOG.error(fileName + ": " + e.getMessage(), e);
            }
        }
        
        WicketUtil.refreshPage(aTarget, getPage());
    }
}
