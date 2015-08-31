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
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ParseException;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;

/**
 * A Panel used to add Project Constraints Rules in a selected {@link Project}
 *
 *
 */
public class ProjectConstraintsPanel
    extends Panel
{
    private static final long serialVersionUID = 8910455936756021733L;

    private static final Log LOG = LogFactory.getLog(ProjectConstraintsPanel.class);

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private SelectionForm selectionForm;
    
    private DetailForm detailForm;
    
    private ImportForm importForm;

    public ProjectConstraintsPanel(String id, IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);
        
        add(selectionForm = new SelectionForm("selectionForm"));
        add(detailForm = new DetailForm("detailForm"));
        add(importForm = new ImportForm("importForm"));
    }

    public Project getModelObject()
    {
        return (Project) getDefaultModelObject();
    }

    public class SelectionForm
        extends Form<ConstraintSet>
    {
        private static final long serialVersionUID = -4835473062143674510L;

        public SelectionForm(String aId)
        {
            super(aId, Model.of((ConstraintSet) null));

            LoadableDetachableModel<List<ConstraintSet>> rulesets = new LoadableDetachableModel<List<ConstraintSet>>()
                    {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<ConstraintSet> load()
                {
                    return projectRepository.listConstraintSets(ProjectConstraintsPanel.this
                            .getModelObject());
                }
            };
            
            add(new ListChoice<ConstraintSet>("ruleset", SelectionForm.this.getModel(), rulesets) {
                private static final long serialVersionUID = 1L;
                
                {
                    setChoiceRenderer(new ChoiceRenderer<ConstraintSet>("name"));
                    setNullValid(false);
                }
                
                @Override
                protected void onSelectionChanged(ConstraintSet aNewSelection)
                {
                    ProjectConstraintsPanel.this.detailForm.setModelObject(aNewSelection);
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
        }
    }

    public class DetailForm
        extends Form<ConstraintSet>
    {
        private static final long serialVersionUID = 8696334789027911595L;

        private TextArea<String> script;
        
        public DetailForm(String aId)
        {
            super(aId, new CompoundPropertyModel<ConstraintSet>(new EntityModel<ConstraintSet>(null)));
            
            add(new TextField<String>("name"));
            
            add(script = new TextArea<String>("script", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load()
                {
                    try {
                        return projectRepository.readConstrainSet(DetailForm.this.getModelObject());
                    }
                    catch (IOException e) {
                        // Cannot call "Component.error()" here - it causes a 
                        // org.apache.wicket.WicketRuntimeException: Cannot modify component 
                        // hierarchy after render phase has started (page version cant change then
                        // anymore)
                        LOG.error("Unable to load script", e);
                        return "Unable to load script: " + ExceptionUtils.getRootCauseMessage(e);
                    }
                }
            }));
            // Script not editable - if we remove this flag, then the script area will not update
            // when switching set selection
            script.setEnabled(false); 

            final IModel<String> exportFilenameModel = new Model<>();
            final IModel<File> exportFileModel = new LoadableDetachableModel<File>()
            {
                private static final long serialVersionUID = 840863954694163375L;

                @Override
                protected File load()
                {
                    try {
                        // Use the name of the constraints set instead of the ID under which the
                        // file is saved internally.
                        exportFilenameModel.setObject(DetailForm.this.getModelObject().getName());
                        return projectRepository.exportConstraintAsFile(DetailForm.this
                                .getModelObject());
                    }
                    catch (IOException e) {
                        throw new WicketRuntimeException(e);
                    }
                }
            }; 
            add(new DownloadLink("export", exportFileModel, exportFilenameModel));

            add(new Button("delete", new ResourceModel("label")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    projectRepository.removeConstraintSet(DetailForm.this.getModelObject());
                    DetailForm.this.setModelObject(null);
                    selectionForm.setModelObject(null);
                }
                
                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(DetailForm.this.getModelObject().getId() >= 0);
                }
            }).add(new AttributeModifier(
                "onclick", "if(!confirm('Do you really want to delete this Constraint rule?')) return false;"));
            add(new Button("save", new ResourceModel("label")) {
                private static final long serialVersionUID = 1L;
                
                @Override
                public void onSubmit()
                {
                    // Actually nothing to do here. Wicket will transfer the values from the
                    // form into the model object and Hibernate will persist it
                    
                    //Check if the modified name already exists, then ignore the changes.
//                    if(projectRepository.existConstraintSet(modifiedName, ProjectConstraintsPanel.this.getModelObject()))
//                    {
//                        setDefaultFormProcessing(false);
//                        setVisible(false);
//                    }
                    
                }
            });
            add(new Button("cancel", new ResourceModel("label")) {
                private static final long serialVersionUID = 1L;
                
                {
                    // Avoid saving data
                    setDefaultFormProcessing(false);
                    
                    // This is currently the only "cancel" button in the project settings. Better
                    // activate when we add such buttons to other panels as well.
                    setVisible(false);
                }
                
                @Override
                public void onSubmit()
                {
                    DetailForm.this.setModelObject(null);
                }
            });
        }
        
        @Override
        protected void onConfigure()
        {
            super.onConfigure();
            
            setVisible(getModelObject() != null);
        }
    }
    
    public class ImportForm
        extends Form<Void>
    {
        private static final long serialVersionUID = 8121850699963791359L;
        
        private List<FileUpload> uploads;
        
        public ImportForm(String aId)
        {
            super(aId);

            add(new FileUploadField("uploads", PropertyModel.<List<FileUpload>> of(this, "uploads")));
            add(new Button("import", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    importAction();
                }
            });
        }
        
        private void importAction()
        {
            Project project = ProjectConstraintsPanel.this.getModelObject();

            if (project.getId() == 0) {
                error("Project not yet created, please save project Details!");
                return;
            }

            if (isEmpty(uploads)) {
                error("No document is selected to upload, please select a document first");
                return;
            }

            for (FileUpload constraintRulesFile : uploads) {
                // Checking if file is OK as per Constraints Grammar specification
                boolean constraintRuleFileIsOK = false;
                try {
                    ConstraintsGrammar parser = new ConstraintsGrammar(
                            constraintRulesFile.getInputStream());
                    parser.Parse();
                    constraintRuleFileIsOK = true;
                }
                catch (IOException e) {
                    error("Unable to read constraints file "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
                catch (ParseException e) {
                    error("Exception while parsing the constraint rules file. Please check it"
                            + ExceptionUtils.getRootCauseMessage(e));
                }

                // Persist rules
                if (constraintRuleFileIsOK) {
                    try {
                        ConstraintSet constraintSet = new ConstraintSet();
                        constraintSet.setProject(ProjectConstraintsPanel.this.getModelObject());
                        //Check if ConstraintSet already exists or not
                        String constraintFilename = constraintRulesFile.getClientFileName();
                        if(projectRepository.existConstraintSet(constraintFilename, project)){
                            constraintFilename = copyConstraintName(projectRepository,constraintFilename);
                        }
                        constraintSet.setName(constraintFilename);
                        projectRepository.createConstraintSet(constraintSet);
                        projectRepository.writeConstraintSet(constraintSet,
                                constraintRulesFile.getInputStream());
                        detailForm.setModelObject(constraintSet);
                        selectionForm.setModelObject(constraintSet);
                    }
                    catch (IOException e) {
                        detailForm.setModelObject(null);
                        error("Unable to write constraints file "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            }
        }
        /**
         * Checks if name exists, if yes, creates an alternate name for ConstraintSet
         * @param projectRepository
         * @param constraintFilename
         * @return
         */
        private String copyConstraintName(RepositoryService projectRepository, String constraintFilename)
        {
            String betterConstraintName = "copy_of_" + constraintFilename;
            int i = 1;
            while (true) {
                if (projectRepository.existConstraintSet(betterConstraintName, ProjectConstraintsPanel.this.getModelObject())) {
                    betterConstraintName = "copy_of_" + constraintFilename + "(" + i + ")";
                    i++;
                }
                else {
                    return betterConstraintName;
                }

            }
        }
    }
}
