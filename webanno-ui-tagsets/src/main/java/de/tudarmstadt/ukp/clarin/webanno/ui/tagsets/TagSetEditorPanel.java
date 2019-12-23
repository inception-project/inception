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
package de.tudarmstadt.ukp.clarin.webanno.ui.tagsets;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSetConstant;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaPanel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;

public class TagSetEditorPanel
    extends LambdaPanel
{
    private static final long serialVersionUID = 3084260865116114184L;

    private @SpringBean AnnotationSchemaService annotationSchemaService;
    
    private ConfirmationDialog confirmationDialog;

    private IModel<Project> selectedProject;
    private IModel<TagSet> selectedTagSet;
    private IModel<Tag> selectedTag;
    private IModel<String> exportFormat;
    
    public TagSetEditorPanel(String aId, IModel<Project> aProject, IModel<TagSet> aTagSet,
            IModel<Tag> aSelectedTag)
    {
        super(aId, aTagSet);
        
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        
        selectedProject = aProject;
        selectedTagSet = aTagSet;
        selectedTag = aSelectedTag;
        exportFormat = Model.of(supportedFormats().get(0));
        
        Form<TagSet> form = new Form<>("form", CompoundPropertyModel.of(aTagSet));
        add(form);
        
        form.add(new TextField<String>("name")
                .add(new TagSetExistsValidator())
                .setRequired(true));
        form.add(new TextField<String>("language"));
        form.add(new TextArea<String>("description"));
        form.add(new CheckBox("createTag"));
        
        form.add(new LambdaAjaxButton<>("save", this::actionSave));
        form.add(new LambdaAjaxLink("delete", this::actionDelete)
                .onConfigure(_this -> _this.setVisible(form.getModelObject().getId() != null)));
        form.add(new LambdaAjaxLink("cancel", this::actionCancel));
        
        form.add(new BootstrapSelect<>("format", exportFormat,
                LambdaModel.of(this::supportedFormats))
                .add(new LambdaAjaxFormComponentUpdatingBehavior("change",  (t) -> { })));
        form.add(new AjaxDownloadLink("export", LambdaModel.of(this::export)));
        
        confirmationDialog = new ConfirmationDialog("confirmationDialog");
        confirmationDialog.setTitleModel(new StringResourceModel("DeleteDialog.title", this));
        add(confirmationDialog);
    }
    
    private List<String> supportedFormats()
    {
        return Arrays.asList(ExportedTagSetConstant.JSON_FORMAT, ExportedTagSetConstant.TAB_FORMAT);
    }
    
    private void actionSave(AjaxRequestTarget aTarget, Form<Tag> aForm) {
        if (isNull(selectedTagSet.getObject().getId())) {
            if (annotationSchemaService.existsTagSet(selectedTagSet.getObject()
                    .getName(), selectedProject.getObject())) {
                error("Only one tagset per project is allowed!");
            }
        }
        
        selectedTagSet.getObject().setProject(selectedProject.getObject());
        annotationSchemaService.createTagSet(selectedTagSet.getObject());
        
        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }
    
    private void actionDelete(AjaxRequestTarget aTarget) {
        confirmationDialog.setContentModel(new StringResourceModel("DeleteDialog.text", this)
                .setParameters(selectedTagSet.getObject().getName()));
        confirmationDialog.show(aTarget);
        
        confirmationDialog.setConfirmAction((_target) -> {
            // If the tagset is used in any features, clear the tagset on these features when
            // the tagset is deleted!
            for (AnnotationFeature ft : annotationSchemaService
                    .listAnnotationFeature(selectedProject.getObject())) {
                if (ft.getTagset() != null && ft.getTagset().equals(selectedTagSet.getObject())) {
                    ft.setTagset(null);
                    annotationSchemaService.createFeature(ft);
                }
            }

            annotationSchemaService.removeTagSet(selectedTagSet.getObject());

            _target.add(getPage());
            actionCancel(_target);
        });
    }
    
    private void actionCancel(AjaxRequestTarget aTarget) {
        selectedTagSet.setObject(null);
        selectedTag.setObject(null);
        
        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }
    
    private FileResourceStream export()
    {
        File exportFile = null;
        if (exportFormat.getObject().equals(ExportedTagSetConstant.JSON_FORMAT)) {
            try {
                exportFile = File.createTempFile("exportedtagsets", ".json");
            }
            catch (IOException e1) {
                error("Unable to create temporary File!!");
                return null;

            }
            if (isNull(selectedTagSet.getObject().getId())) {
                error("Project not yet created. Please save project details first!");
            }
            else {
                TagSet tagSet = selectedTagSet.getObject();
                ExportedTagSet exTagSet = new ExportedTagSet();
                exTagSet.setDescription(tagSet.getDescription());
                exTagSet.setLanguage(tagSet.getLanguage());
                exTagSet.setName(tagSet.getName());

                List<ExportedTag> exportedTags = new ArrayList<>();
                for (Tag tag : annotationSchemaService.listTags(tagSet)) {
                    ExportedTag exportedTag = new ExportedTag();
                    exportedTag.setDescription(tag.getDescription());
                    exportedTag.setName(tag.getName());
                    exportedTags.add(exportedTag);

                }
                exTagSet.setTags(exportedTags);

                try {
                    JSONUtil.generatePrettyJson(exTagSet, exportFile);
                }
                catch (IOException e) {
                    error("File Path not found or No permision to save the file!");
                }
                
                info("TagSets successfully exported to :" + exportFile.getAbsolutePath());
            }
        }
        else if (exportFormat.getObject().equals(ExportedTagSetConstant.TAB_FORMAT)) {
            TagSet tagSet = selectedTagSet.getObject();
            try {
                exportFile = File.createTempFile("exportedtagsets", ".txt");
            }
            catch (IOException e1) {
                error("Unable to create temporary File!!");
            }
            OutputStream os;
            OutputStreamWriter osw;
            BufferedWriter bw;
            try {
                String tagSetDescription = tagSet.getDescription() == null ? ""
                        : tagSet.getDescription();
                os = new FileOutputStream(exportFile);
                osw = new OutputStreamWriter(os, "UTF-8");
                bw = new BufferedWriter(osw);
                bw.write(tagSet.getName() + "\t"
                        + tagSetDescription.replace("\n", "\\n") + "\n");
                bw.write(tagSet.getLanguage() + "\t" + " \n");
                for (Tag tag : annotationSchemaService.listTags(tagSet)) {
                    String tagDescription = tag.getDescription() == null ? "" : tag
                            .getDescription();
                    bw.write(tag.getName() + "\t" + tagDescription.replace("\n", "\\n")
                            + "\n");
                }

                bw.flush();
                bw.close();
            }
            catch (FileNotFoundException e) {
                error("The file for export not found "
                        + ExceptionUtils.getRootCauseMessage(e));
            }
            catch (UnsupportedEncodingException e) {
                error("Unsupported encoding " + ExceptionUtils.getRootCauseMessage(e));
            }
            catch (IOException e) {
                error(ExceptionUtils.getRootCause(e));
            }

        }
        return new FileResourceStream(exportFile);
    }
    
    private class TagSetExistsValidator
        implements IValidator<String>
    {
        private static final long serialVersionUID = -5775158293783334223L;

        @Override
        public void validate(IValidatable<String> aValidatable)
        {
            String newName = aValidatable.getValue();
            String oldName = aValidatable.getModel().getObject();
            if (!StringUtils.equals(newName, oldName) && isNotBlank(newName)
                    && annotationSchemaService.existsTagSet(newName, selectedProject.getObject())) {
                aValidatable.error(new ValidationError(
                        "Another tag set with the same name exists. Please try a different name"));
            }
        }
    }
}
