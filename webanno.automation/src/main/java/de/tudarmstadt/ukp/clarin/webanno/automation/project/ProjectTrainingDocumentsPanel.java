/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.automation.project;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * A Panel used to add Documents to the selected {@link Project}
 *
 * @author Seid Muhie Yimam
 *
 */
public class ProjectTrainingDocumentsPanel
    extends Panel
{
    private static final long serialVersionUID = 2116717853865353733L;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;
    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    private ArrayList<String> documents = new ArrayList<String>();
    private ArrayList<String> selectedDocuments = new ArrayList<String>();

    private List<FileUpload> uploadedFiles;
    private FileUploadField fileUpload;

    private ArrayList<String> readableFormats;
    private String selectedFormat;
    private Model<Project> selectedProjectModel;
    private AnnotationFeature feature;
    private DropDownChoice<String> readableFormatsChoice;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ProjectTrainingDocumentsPanel(String id, Model<Project> aProjectModel, final boolean aFreeDocument,
            Model<AnnotationFeature> afeatureModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        feature = afeatureModel.getObject();
        try {
        	if(aFreeDocument){
        		readableFormats = new ArrayList<String>(Arrays.asList(new String[]{WebAnnoConst.TAB_SEP}));
                selectedFormat = WebAnnoConst.TAB_SEP;
        	}
        	else{
            readableFormats = new ArrayList<String>(repository.getReadableFormatLabels());
            selectedFormat = readableFormats.get(0);
        	}
        }
        catch (IOException e) {
            error("Properties file not found or key not int the properties file " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (ClassNotFoundException e) {
            error("The Class name in the properties is not found " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        add(fileUpload = new FileUploadField("content", new Model()));

        add(readableFormatsChoice = new DropDownChoice<String>("readableFormats", new Model(
                selectedFormat), readableFormats){

					private static final long serialVersionUID = 2476274669926250023L;

			@Override
        	public boolean isEnabled(){
        		return !aFreeDocument;
        	}
        });

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

                    if (repository.existsSourceDocument(project, fileName)) {
                        error("Document " + fileName + " already uploaded ! Delete "
                                + "the document if you want to upload again");
                        continue;
                    }

                    try {
                        File uploadFile = documentToUpload.writeToTempFile();

                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        User user = repository.getUser(username);

                        SourceDocument document = new SourceDocument();
                        document.setName(fileName);
                        document.setProject(project);

                        document.setTrainingDocument(true);
                        // Since new training document is added, all non-tarining document should be
                        // re-annotated
                        for (SourceDocument sd : repository.listSourceDocuments(project)) {
                            if (!sd.isTrainingDocument()) {
                                sd.setProcessed(false);
                            }
                        }
                        document.setFeature(feature);

                        String reader = repository.getReadableFormatId(readableFormatsChoice
                                .getModelObject());
                        document.setFormat(reader);
                        repository.createSourceDocument(document, user);
                        repository.uploadSourceDocument(uploadFile, document, user);
                        info("File [" + fileName + "] has been imported successfully!");
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error("Error uploading document " + e.getMessage());
                    }
                    catch (UIMAException e) {
                        error("Error uploading document " + ExceptionUtils.getRootCauseMessage(e));
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
                            for (SourceDocument document : repository.listSourceDocuments(project)) {
                                if (document.getFeature() != null
                                        && document.getFeature().equals(feature)) {
                                    documents.add(document.getName());
                                }
                            }
                        }
                        return documents;
                    }
                });
            }
        });

        add(new Button("remove", new ResourceModel("label"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit()
            {
                Project project = selectedProjectModel.getObject();
                for (String document : selectedDocuments) {
                    try {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        User user = repository.getUser(username);
                        repository.removeSourceDocument(
                                repository.getSourceDocument(project, document), user);
                    }
                    catch (IOException e) {
                        error("Error while removing a document document "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    documents.remove(document);
                }
            }
        });
    }
}
