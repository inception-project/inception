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

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorException;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapFileInputField;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.logging.LogLevel;

public class ImportDocumentsPanel
    extends Panel
{
    private static final long serialVersionUID = 4927011191395114886L;

    private final static Logger LOG = LoggerFactory.getLogger(ImportDocumentsPanel.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean DocumentImportExportService importExportService;
    private @SpringBean AnnotationSchemaService annotationService;

    private BootstrapFileInputField fileUpload;

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
            if (readableFormats.contains(TextFormatSupport.NAME)) {
                format.setObject(TextFormatSupport.NAME);
            }
            else {
                format.setObject(readableFormats.get(0));
            }
        }

        form.add(fileUpload = new BootstrapFileInputField("documents"));
        fileUpload.getConfig().showPreview(false);
        fileUpload.getConfig().showUpload(false);
        fileUpload.getConfig().showRemove(false);
        fileUpload.setRequired(true);

        DropDownChoice<String> formats = new DropDownChoice<>("format");
        formats.setModel(format);
        formats.setChoices(LoadableDetachableModel.of(this::listReadableFormats));
        form.add(formats);

        form.add(new LambdaAjaxButton<>("import", this::actionImport));
    }

    private List<String> listReadableFormats()
    {
        return importExportService.getReadableFormats().stream() //
                .map(FormatSupport::getName) //
                .sorted() //
                .collect(toList());
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

        TypeSystemDescription fullProjectTypeSystem;
        try {
            fullProjectTypeSystem = annotationService.getFullProjectTypeSystem(project);
        }
        catch (Exception e) {
            error("Unable to acquire the type system for project: " + getRootCauseMessage(e));
            LOG.error("Unable to acquire the type system for project [{}]({})", project.getName(),
                    project.getId(), e);
            return;
        }

        // Fetching all documents at once here is faster than calling existsSourceDocument() for
        // every imported document
        Set<String> existingDocuments = documentService.listSourceDocuments(project).stream() //
                .map(SourceDocument::getName) //
                .collect(toCollection(HashSet::new));

        List<SourceDocument> importedDocuments = new ArrayList<>();
        for (FileUpload documentToUpload : uploadedFiles) {
            String fileName = documentToUpload.getClientFileName();

            var nameValidationResult = documentService.validateDocumentName(fileName);
            if (!nameValidationResult.isEmpty()) {
                nameValidationResult
                        .forEach(msg -> error("[" + fileName + "]:" + msg.getMessage()));
                continue;
            }

            if (existingDocuments.contains(fileName)) {
                error("[" + fileName + "]: already uploaded! Delete "
                        + "the document if you want to upload again");
                continue;
            }

            try {
                var document = new SourceDocument();
                document.setName(fileName);
                document.setProject(project);
                document.setFormat(
                        importExportService.getFormatByName(format.getObject()).get().getId());

                try (var is = documentToUpload.getInputStream()) {
                    documentService.uploadSourceDocument(is, document, fullProjectTypeSystem);
                }

                importedDocuments.add(document);
                info("Document [" + fileName + "] has been imported successfully!");

                // Add the imported document to the set of existing documents just in case the user
                // somehow manages to upload two files with the same name...
                existingDocuments.add(fileName);
            }
            catch (Throwable e) {
                if (e.getCause() instanceof CasDoctorException) {
                    var ex = (CasDoctorException) e.getCause();
                    error("Document [" + fileName + "] contains inconsistent data.");
                    ex.getDetails().stream().filter(msg -> msg.level == LogLevel.ERROR)
                            .forEachOrdered(msg -> error(msg.message));
                }
                else {
                    error("Error while uploading document [" + fileName + "]: "
                            + getRootCauseMessage(e));
                    LOG.error(fileName + ": " + e.getMessage(), e);
                }
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }

        send(this, BUBBLE, new SourceDocumentImportedEvent(aTarget, importedDocuments));

        aTarget.add(findParent(ProjectSettingsPanelBase.class));
    }
}
