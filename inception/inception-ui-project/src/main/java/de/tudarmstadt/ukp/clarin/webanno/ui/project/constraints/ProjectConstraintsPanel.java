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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.constraints;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.io.IOUtils.toInputStream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ParseException;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapFileInputField;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

/**
 * A Panel used to add Project Constraints Rules in a selected {@link Project}.
 */
public class ProjectConstraintsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 8910455936756021733L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectConstraintsPanel.class);

    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean UserDao userService;
    private @SpringBean AnnotationSchemaService schemaService;

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

            var rulesets = new LoadableDetachableModel<List<ConstraintSet>>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<ConstraintSet> load()
                {
                    return constraintsService
                            .listConstraintSets(ProjectConstraintsPanel.this.getModelObject());
                }
            };

            add(new ListChoice<ConstraintSet>("ruleset", SelectionForm.this.getModel(), rulesets)
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoiceRenderer(new ChoiceRenderer<>("name"));
                    setNullValid(false);
                    // Turn this into a LambdaFormComponentUpdatingBehavior?
                    add(new FormComponentUpdatingBehavior());
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

        private BootstrapModalDialog confirmationDialog;

        public DetailForm(String aId, IModel<ConstraintSet> aModel)
        {
            super(aId, new CompoundPropertyModel<>(aModel));

            setOutputMarkupPlaceholderTag(true);

            var constraintNameTextField = new TextField<String>("name");
            add(constraintNameTextField);

            add(script = new TextArea<String>("script",
                    LoadableDetachableModel.of(this::getScript)));
            script.setOutputMarkupId(true);
            // Script not editable - if we remove this flag, then the script area will not update
            // when switching set selection
            // script.setEnabled(false);

            final var exportFilenameModel = new Model<String>();
            final var exportFileModel = new LoadableDetachableModel<File>()
            {
                private static final long serialVersionUID = 840863954694163375L;

                @Override
                protected File load()
                {
                    try {
                        // Use the name of the constraints set instead of the ID under which the
                        // file is saved internally.
                        exportFilenameModel.setObject(DetailForm.this.getModelObject().getName());
                        return constraintsService
                                .exportConstraintAsFile(DetailForm.this.getModelObject());
                    }
                    catch (IOException e) {
                        throw new WicketRuntimeException(e);
                    }
                }
            };

            confirmationDialog = new BootstrapModalDialog("confirmationDialog");
            confirmationDialog.trapFocus();
            add(confirmationDialog);

            // The file that is returned by exportConstraintAsFile is the internal constraints
            // file - it must NOT be deleted after the export is complete!
            add(new DownloadLink("export", exportFileModel, exportFilenameModel));

            var deleteButton = new LambdaAjaxLink("delete", this::actionDelete);
            deleteButton.add(visibleWhen(() -> DetailForm.this.getModelObject().getId() != null));
            add(deleteButton);

            add(new Button("save")
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    var constraintSet = DetailForm.this.getModelObject();

                    try {
                        ConstraintsParser.parse(script.getModelObject());
                    }
                    catch (ParseException e) {
                        error("Unable to parse constraints file [" + constraintSet.getName() + "]"
                                + ExceptionUtils.getRootCauseMessage(e));
                        return;
                    }

                    constraintsService.createOrUpdateConstraintSet(constraintSet);

                    // Persist rules
                    try (var rules = toInputStream(script.getModelObject(), UTF_8)) {
                        constraintsService.writeConstraintSet(constraintSet, rules);

                        selectionForm.setModelObject(constraintSet);
                        detailForm.setModelObject(constraintSet);

                        success("Successfully updated constraints file [" + constraintSet.getName()
                                + "]");
                    }
                    catch (IOException e) {
                        LOG.error("Unable to write the constraint rules file.", e);
                        error("Unable to write the constraints file "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
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

            LambdaAjaxLink cancelButton = new LambdaAjaxLink("cancel", this::actionCancel);
            add(cancelButton);
        }

        private String getScript()
        {
            try {
                return constraintsService.readConstrainSet(DetailForm.this.getModelObject());
            }
            catch (IOException e) {
                // Cannot call "Component.error()" here - it causes a
                // org.apache.wicket.WicketRuntimeException: Cannot modify component
                // hierarchy after render phase has started (page version can't change then
                // anymore)
                LOG.error("Unable to load script", e);
                return "Unable to load script: " + ExceptionUtils.getRootCauseMessage(e);
            }
        }

        private void actionCancel(AjaxRequestTarget aTarget)
        {
            DetailForm.this.setModelObject(null);
            aTarget.add(findParent(ProjectSettingsPanelBase.class));
        }

        private void actionDelete(AjaxRequestTarget aTarget)
        {
            var dialogContent = new ConstraintsDeletionConfirmationDialogPanel(
                    BootstrapModalDialog.CONTENT_ID, DetailForm.this.getModel());

            dialogContent.setConfirmAction((_target) -> {
                constraintsService.removeConstraintSet(DetailForm.this.getModelObject());
                DetailForm.this.setModelObject(null);
                _target.add(findParent(ProjectSettingsPanelBase.class));
            });

            confirmationDialog.open(dialogContent, aTarget);
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

        private BootstrapFileInputField uploads;

        public ImportForm(String aId)
        {
            super(aId);

            add(new LambdaAjaxLink("create", this::createAction));

            add(uploads = new BootstrapFileInputField("uploads"));
            uploads.getConfig().showPreview(false);
            uploads.getConfig().showUpload(false);
            uploads.getConfig().showRemove(false);
            uploads.setRequired(true);

            add(new LambdaAjaxButton<Void>("import", this::importAction));
        }

        private void createAction(AjaxRequestTarget aTarget)
        {
            var constraintFilename = "New constraints set.rules";
            var content = "/* Constraint rules set created by " + userService.getCurrentUsername()
                    + " */\n\n";

            var disambiguationMap = new HashMap<String, AtomicInteger>();
            for (var layer : schemaService
                    .listAnnotationLayer(ProjectConstraintsPanel.this.getModelObject())) {

                if (asList(Token._TypeName, Sentence._TypeName).contains(layer.getName())) {
                    continue;
                }

                var shortName = StringUtils.substringAfterLast(layer.getName(), ".");
                if (shortName.isEmpty()) {
                    shortName = layer.getName();
                }
                var disambiguation = disambiguationMap.computeIfAbsent(shortName,
                        $ -> new AtomicInteger(0));
                disambiguation.incrementAndGet();
                content += "import " + layer.getName() + " as " + shortName;
                if (disambiguation.get() >= 2) {
                    content += disambiguation.get();
                }
                content += ";\n";
            }
            content += "\n";

            var constraintSet = createConstraintsSet(constraintFilename);

            aTarget.addChildren(getPage(), IFeedback.class);
            aTarget.add(selectionForm);
            aTarget.add(detailForm);
            aTarget.focusComponent(detailForm.script);

            // Persist rules
            try (var rules = toInputStream(content, UTF_8)) {
                constraintsService.writeConstraintSet(constraintSet, rules);

                selectionForm.setModelObject(constraintSet);
                detailForm.setModelObject(constraintSet);

                success("Created new constraints set [" + constraintSet.getName() + "]");
            }
            catch (IOException e) {
                LOG.error("Unable to write the constraint rules file.", e);
                error("Unable to write the constraints file "
                        + ExceptionUtils.getRootCauseMessage(e));
            }
        }

        private void importAction(AjaxRequestTarget aTarget, Form<Void> aForm)
        {
            var project = ProjectConstraintsPanel.this.getModelObject();

            var uploadedFiles = uploads.getFileUploads();

            selectionForm.setModelObject(null);
            detailForm.setModelObject(null);

            aTarget.addChildren(getPage(), IFeedback.class);
            aTarget.add(selectionForm);
            aTarget.add(detailForm);

            if (isNull(project.getId())) {
                error("Project not yet created, please save project Details!");
                return;
            }

            if (isEmpty(uploadedFiles)) {
                error("No document is selected to upload, please select a document first");
                return;
            }

            nextFile: for (FileUpload constraintRulesFile : uploadedFiles) {
                var constraintFilename = constraintRulesFile.getClientFileName();

                // Handling Windows BOM
                try (var bomInputStream = new BOMInputStream(constraintRulesFile.getInputStream(),
                        false)) {
                    ConstraintsParser.parse(bomInputStream);
                }
                catch (IOException e) {
                    error("Unable to read the constraints file [" + constraintFilename + "]"
                            + ExceptionUtils.getRootCauseMessage(e));
                    continue nextFile;
                }
                catch (ParseException e) {
                    error("Unable to parse constraints file [" + constraintFilename + "]"
                            + ExceptionUtils.getRootCauseMessage(e));
                    continue nextFile;
                }

                // Persist rules
                try (var rules = new BOMInputStream(constraintRulesFile.getInputStream(), false)) {
                    ConstraintSet constraintSet = createConstraintsSet(constraintFilename);

                    constraintsService.writeConstraintSet(constraintSet, rules);

                    selectionForm.setModelObject(constraintSet);
                    detailForm.setModelObject(constraintSet);

                    success("Successfully imported constraints file [" + constraintFilename + "]");
                }
                catch (IOException e) {
                    LOG.error("Unable to write the constraint rules file.", e);
                    error("Unable to write the constraints file "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }

        private ConstraintSet createConstraintsSet(String constraintFilename)
        {
            var project = ProjectConstraintsPanel.this.getModelObject();

            var constraintSet = new ConstraintSet();
            constraintSet.setProject(ProjectConstraintsPanel.this.getModelObject());

            // Check if ConstraintSet already exists or not
            if (constraintsService.existConstraintSet(constraintFilename, project)) {
                constraintFilename = copyConstraintName(constraintsService, constraintFilename);
            }
            constraintSet.setName(constraintFilename);
            constraintsService.createOrUpdateConstraintSet(constraintSet);

            return constraintSet;
        }

        /**
         * Checks if name exists, if yes, creates an alternate name for ConstraintSet
         */
        private String copyConstraintName(ConstraintsService aConstraintsService,
                String constraintFilename)
        {
            var baseConstraintName = FilenameUtils.getBaseName(constraintFilename);
            var betterConstraintName = baseConstraintName;
            var suffix = FilenameUtils.getExtension(constraintFilename);
            int i = 1;
            while (true) {
                if (aConstraintsService.existConstraintSet(betterConstraintName + "." + suffix,
                        ProjectConstraintsPanel.this.getModelObject())) {
                    betterConstraintName = baseConstraintName + " (" + i + ")";
                    i++;
                }
                else {
                    return betterConstraintName + "." + suffix;
                }
            }
        }
    }
}
