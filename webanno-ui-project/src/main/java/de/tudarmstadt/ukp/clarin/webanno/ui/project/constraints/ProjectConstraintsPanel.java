/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.project.constraints;

import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ParseException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.TokenMgrError;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;

/**
 * A Panel used to add Project Constraints Rules in a selected {@link Project}.
 */
@ProjectSettingsPanel(label = "Constraints", prio = 500)
public class ProjectConstraintsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 8910455936756021733L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectConstraintsPanel.class);

    private @SpringBean ConstraintsService constraintsService;

    private SelectionForm selectionForm;
    private DetailForm detailForm;
    
    private IModel<ConstraintSet> selectedConstraints;

    public ProjectConstraintsPanel(String id, IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);
        
        selectedConstraints = Model.of();
        
        add(selectionForm = new SelectionForm("selectionForm", selectedConstraints));
        add(detailForm = new DetailForm("detailForm", selectedConstraints));
        add(new ImportForm("importForm"));
    }
    
    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        selectedConstraints.setObject(null);
    }

    public class SelectionForm
        extends Form<ConstraintSet>
    {
        private static final long serialVersionUID = -4835473062143674510L;

        public SelectionForm(String aId, IModel<ConstraintSet> aModel)
        {
            super(aId, aModel);

            LoadableDetachableModel<List<ConstraintSet>> rulesets = 
                    new LoadableDetachableModel<List<ConstraintSet>>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<ConstraintSet> load()
                {
                    return constraintsService
                            .listConstraintSets(ProjectConstraintsPanel.this.getModelObject());
                }
            };
            
            add(new ListChoice<ConstraintSet>("ruleset", SelectionForm.this.getModel(), rulesets) {
                private static final long serialVersionUID = 1L;
                
                {
                    setChoiceRenderer(new ChoiceRenderer<>("name"));
                    setNullValid(false);
                }
                
                @Override
                protected void onSelectionChanged(ConstraintSet aNewSelection)
                {
                    // Nothing to do - model already updated automatically
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
        
        public DetailForm(String aId, IModel<ConstraintSet> aModel)
        {
            super(aId, new CompoundPropertyModel<>(aModel));
            TextField<String> constraintNameTextField = new TextField<>("name");
            add(constraintNameTextField);
            
            add(script = new TextArea<>("script", new LoadableDetachableModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load() {
                    try {
                        return constraintsService
                                .readConstrainSet(DetailForm.this.getModelObject());
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
                        return constraintsService.exportConstraintAsFile(DetailForm.this
                                .getModelObject());
                    }
                    catch (IOException e) {
                        throw new WicketRuntimeException(e);
                    }
                }
            }; 
            add(new DownloadLink("export", exportFileModel, exportFilenameModel)
                    .setDeleteAfterDownload(true));
            
            Button deleteButton = new Button("delete") {

                private static final long serialVersionUID = -1195565364207114557L;

                @Override
                public void onSubmit()
                {
                    constraintsService.removeConstraintSet(DetailForm.this.getModelObject());
                    DetailForm.this.setModelObject(null);
                }
                
                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(DetailForm.this.getModelObject().getId() != null);
                }
            };
            // Add check to prevent accidental delete operation
            deleteButton.add(new AttributeModifier("onclick",
                    "if(!confirm('Do you really want to delete this Constraints rule?')) return false;"));

            add(deleteButton);

            add(new Button("save") {
                private static final long serialVersionUID = 1L;
                
                @Override
                public void onSubmit()
                {
                    // Actually nothing to do here. Wicket will transfer the values from the
                    // form into the model object and Hibernate will persist it                    
                }

                @Override
                public void validate()
                {
                    super.validate();
                    // Checking if the name provided already exists or not
                    if (constraintsService.existConstraintSet(constraintNameTextField.getInput(),
                            ProjectConstraintsPanel.this.getModelObject())
                            && !constraintNameTextField.getInput()
                                    .equals(constraintNameTextField.getModelObject())) {
                        error("Provided name for Constraint already exists, please choose a different name");
                    }
                }
            });
            add(new Button("cancel") {
                private static final long serialVersionUID = 1L;
                
                {
                    // Avoid saving data
                    setDefaultFormProcessing(false);
                    setVisible(true);
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
        
        private FileUploadField uploads;
        
        public ImportForm(String aId)
        {
            super(aId);

            add(uploads = new FileUploadField("uploads", Model.of()));
            add(new Button("import")
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    try {
                        importAction();
                    }
                    catch (ParseException | TokenMgrError e) {
                        LOG.error(
                                "Exception while parsing the constraint rules file. Please check it.",
                                e);
                        error("Exception while parsing the constraint rules file. Please check it. "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (IOException e) {
                        LOG.error("Unable to read the constraint rules file.", e);
                        error("Unable to read constraints file: "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            });
        }
        
        private void importAction() throws ParseException, IOException
        {
            Project project = ProjectConstraintsPanel.this.getModelObject();

            List<FileUpload> uploadedFiles = uploads.getFileUploads();

            if (isNull(project.getId())) {
                error("Project not yet created, please save project Details!");
                return;
            }

            if (isEmpty(uploadedFiles)) {
                error("No document is selected to upload, please select a document first");
                return;
            }

            for (FileUpload constraintRulesFile : uploadedFiles) {
                // Checking if file is OK as per Constraints Grammar specification
                boolean constraintRuleFileIsOK = false;
                //Handling Windows BOM
                BOMInputStream bomInputStream;

                bomInputStream = new BOMInputStream(constraintRulesFile.getInputStream(), false);
                ConstraintsGrammar parser = new ConstraintsGrammar(bomInputStream);
                // utfInputStream = new
                // InputStreamReader(constraintRulesFile.getInputStream(),"UTF-8");
                // ConstraintsGrammar parser = new ConstraintsGrammar(utfInputStream);
                parser.Parse();
                constraintRuleFileIsOK = true;

                // Persist rules
                if (constraintRuleFileIsOK) {
                    try {
                        ConstraintSet constraintSet = new ConstraintSet();
                        constraintSet.setProject(ProjectConstraintsPanel.this.getModelObject());
                        //Check if ConstraintSet already exists or not
                        String constraintFilename = constraintRulesFile.getClientFileName();
                        if (constraintsService.existConstraintSet(constraintFilename, project)) {
                            constraintFilename = copyConstraintName(constraintsService,
                                    constraintFilename);
                        }
                        constraintSet.setName(constraintFilename);
                        constraintsService.createConstraintSet(constraintSet);
                        constraintsService.writeConstraintSet(constraintSet,
                                constraintRulesFile.getInputStream());
                        selectionForm.setModelObject(constraintSet);
                    }
                    catch (IOException e) {
                        detailForm.setModelObject(null);
                        LOG.error("Unable to write the constraint rules file.", e);
                        error("Unable to write the constraints file "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            }
        }
        
        /**
         * Checks if name exists, if yes, creates an alternate name for ConstraintSet
         */
        private String copyConstraintName(ConstraintsService aConstraintsService,
                String constraintFilename)
        {
            String betterConstraintName = "copy_of_" + constraintFilename;
            int i = 1;
            while (true) {
                if (aConstraintsService.existConstraintSet(betterConstraintName,
                        ProjectConstraintsPanel.this.getModelObject())) {
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
