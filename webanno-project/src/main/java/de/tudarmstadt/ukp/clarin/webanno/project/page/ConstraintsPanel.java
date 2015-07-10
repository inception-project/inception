/*******************************************************************************
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.project.page;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import com.googlecode.wicket.jquery.ui.widget.progressbar.ProgressBar;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ParseException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

/**
 * A Panel used to add Project Constraints Rules in a selected {@link Project}
 *
 * @author aakash
 *
 */
public class ConstraintsPanel
    extends Panel
{

    private static final long serialVersionUID = 8910455936756021733L;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    // private ArrayList<String> documents = new ArrayList<String>();
    // private ArrayList<String> selectedDocuments = new ArrayList<String>();

    private List<FileUpload> uploadedFiles;
    private FileUploadField fileUpload;

    private Model<Project> selectedProjectModel;
    private boolean isThereAConstraintRulesFile;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ConstraintsPanel(String id, Model<Project> aProjectModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        add(fileUpload = new FileUploadField("content", new Model()));
        Project tempProject = selectedProjectModel.getObject();
        isThereAConstraintRulesFile = projectRepository.getConstraintRulesFile(tempProject).exists();
        add(new Button("importConstraintRules", new ResourceModel("label"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit()
            {
                uploadedFiles = fileUpload.getFileUploads();
                if (uploadedFiles.size() > 1) {
                    error("Please select only 1 document for Constraint Rules");
                    return;
                }
                Project project = selectedProjectModel.getObject();

                if (project.getId() == 0) {
                    error("Project not yet created, please save project Details!");
                    return;
                }
                if (isEmpty(uploadedFiles)) {
                    error("No document is selected to upload, please select a document first");
                    return;
                }

                for (FileUpload constraintRulesFile : uploadedFiles) {

                    try {
                        File tempFile = constraintRulesFile.writeToTempFile();
                        String fileName = constraintRulesFile.getClientFileName();
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        boolean constraintRuleFileIsOK = false;
                        // Checking if file is OK as per Constraints Grammar specification
                        ConstraintsGrammar parser;
                        Parse p;
                        parser = new ConstraintsGrammar(new FileInputStream(tempFile));
                        p = parser.Parse();
                        constraintRuleFileIsOK=true;
                        if (constraintRuleFileIsOK) {
                            projectRepository.createConstraintRules(project, tempFile, fileName,
                                    username);
                            isThereAConstraintRulesFile = true;
                        }
                    }
                    catch (IOException e) {
                        error("Unable to write constraints file "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ParseException e) {
                        error("Exception while parsing the constraint rules file. Please check it"
                                + ExceptionUtils.getRootCauseMessage(e));
                    }                    
                }

            }
        });

        add(new Button("removeConstraintRules", new ResourceModel("label"))
        {
            private static final long serialVersionUID = 1L;
            
            
            @Override
            protected IModel<String> initModel()
            {
                this.setEnabled(isThereAConstraintRulesFile);
                return super.initModel();
            }


            @Override
            public void updateModel()
            {
                this.setEnabled(isThereAConstraintRulesFile);
                super.updateModel();
            }


            @Override
            public void onSubmit()
            {
                Project project = selectedProjectModel.getObject();

                try {
                    String username = SecurityContextHolder.getContext().getAuthentication()
                            .getName();
                    projectRepository.removeConstraintRules(project, username);
                    isThereAConstraintRulesFile = false;
                    this.updateModel();
                }
                catch (IOException e) {
                    error("Error while removing Constraint Rules "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
                // documents.remove(document);

            }
        });
    }
}
