/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * A Panel used to add Documents to the selected {@link Project}
 * 
 * 
 */
public class ProjectDocumentsPanel
    extends Panel
{
    private final static Logger LOG = LoggerFactory.getLogger(ProjectDocumentsPanel.class);
    
    private static final long serialVersionUID = 2116717853865353733L;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;
    
    @SpringBean(name = "documentRepository")
    private DocumentService documentService;

    @SpringBean(name = "documentRepository")
    private ImportExportService importExportService;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private ArrayList<String> documents = new ArrayList<String>();
    private ArrayList<String> selectedDocuments = new ArrayList<String>();

    private List<FileUpload> uploadedFiles;
    private FileUploadField fileUpload;

    private ArrayList<String> readableFormats;
    private String selectedFormat;
    private IModel<Project> selectedProjectModel;
    private DropDownChoice<String> readableFormatsChoice;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ProjectDocumentsPanel(String id, IModel<Project> aProjectModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        readableFormats = new ArrayList<String>(importExportService.getReadableFormatLabels());
        selectedFormat = readableFormats.get(0);
        
        add(fileUpload = new FileUploadField("content", new Model()));

        add(readableFormatsChoice = new DropDownChoice<String>("readableFormats", new Model(
                selectedFormat), readableFormats));

        add(new Button("import", new ResourceModel("label"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit()
            {
                uploadedFiles = fileUpload.getFileUploads();
                Project project = selectedProjectModel.getObject();
                if (isEmpty(uploadedFiles)) {
                    error("No document is selected to upload, please select a document first");
                    return;
                }
                if (project.getId() == 0) {
                    error("Project not yet created, please save project Details!");
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
                        File uploadFile = documentToUpload.writeToTempFile();

                        String format = importExportService
                                .getReadableFormatId(readableFormatsChoice.getModelObject());

                        SourceDocument document = new SourceDocument();
                        document.setName(fileName);
                        document.setProject(project);
                        document.setFormat(format);
                        
                        documentService.uploadSourceDocument(uploadFile, document);
                        info("File [" + fileName + "] has been imported successfully!");
                    }
                    catch (Exception e) {
                        error("Error while uploading document " + fileName + ": "
                            + ExceptionUtils.getRootCauseMessage(e));
                        LOG.error(fileName + ": " + e.getMessage(), e);
                    }
                }

            }
        });

        add(new ListMultipleChoice<String>("documents", new Model(selectedDocuments), documents)
        {
            private static final long serialVersionUID = 1L;

            {
                setChoices(new LoadableDetachableModel<List<String>>()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected List<String> load()
                    {
                        Project project = selectedProjectModel.getObject();
                        documents.clear();
                        if (project.getId() != 0) {
                            for (SourceDocument document : documentService
                                    .listSourceDocuments(project)) {
                                if (!document.isTrainingDocument()) {
                                    documents.add(document.getName());
                                }
                            }
                        }
                        return documents;
                    }
                });
            }
        });
        
        Button removeDocumentButton = new Button("remove", new ResourceModel("label"))
        {

            private static final long serialVersionUID = 4053376790104708784L;

            @Override
            public void onSubmit()
            {
                Project project = selectedProjectModel.getObject();
                for (String document : selectedDocuments) {
                    try {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        User user = userRepository.get(username);
                        documentService.removeSourceDocument(
                                documentService.getSourceDocument(project, document));
                    }
                    catch (IOException e) {
                        error("Error while removing a document document "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    documents.remove(document);
                }
            }
        };
        // Add check to prevent accidental delete operation
        removeDocumentButton.add(new AttributeModifier("onclick",
                "if(!confirm('Do you really want to delete this Document?')) return false;"));
        add(removeDocumentButton);
        
//        add(new Button("remove", new ResourceModel("label"))
//        {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onSubmit()
//            {
//                Project project = selectedProjectModel.getObject();
//                for (String document : selectedDocuments) {
//                    try {
//                        String username = SecurityContextHolder.getContext().getAuthentication()
//                                .getName();
//                        User user = userRepository.get(username);
//                        repository.removeSourceDocument(
//                                repository.getSourceDocument(project, document));
//                    }
//                    catch (IOException e) {
//                        error("Error while removing a document document "
//                                + ExceptionUtils.getRootCauseMessage(e));
//                    }
//                    documents.remove(document);
//                }
//            }
//        });
    }
}
