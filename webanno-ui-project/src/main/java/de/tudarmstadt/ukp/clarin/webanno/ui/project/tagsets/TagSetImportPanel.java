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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.tagsets;

import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.JsonImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.export.ImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSetConstant;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;

public class TagSetImportPanel
    extends Panel
{
    private static final long serialVersionUID = 4612767288793876015L;

    private static final Logger LOG = LoggerFactory.getLogger(TagSetImportPanel.class);
    
    private @SpringBean AnnotationSchemaService annotationService;
    
    private IModel<Project> selectedProject;
    private IModel<Preferences> preferences;
    private IModel<TagSet> selectedTagSet;
    private FileUploadField fileUpload;

    private AjaxCallback importCompleteAction;
    
    public TagSetImportPanel(String aId, IModel<Project> aModel, IModel<TagSet> aTagSet)
    {
        super(aId);
        
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        
        preferences = Model.of(new Preferences());
        selectedProject = aModel;
        selectedTagSet = aTagSet;
        
        Form<Preferences> form = new Form<>("form", CompoundPropertyModel.of(preferences));
        form.add(new DropDownChoice<>("format", LambdaModel.of(this::supportedFormats))
                .setRequired(true));
        form.add(new CheckBox("overwrite"));
        form.add(fileUpload = new FileUploadField("content", new ListModel<>()));
        fileUpload.setRequired(true);
        form.add(new LambdaAjaxButton<>("import", this::actionImport));
        add(form);
    }
    
    private List<String> supportedFormats()
    {
        return Arrays.asList(ExportedTagSetConstant.JSON_FORMAT, ExportedTagSetConstant.TAB_FORMAT);
    }

    private void actionImport(AjaxRequestTarget aTarget, Form<Preferences> aForm)
    {
        List<FileUpload> uploadedFiles = fileUpload.getFileUploads();
        Project project = selectedProject.getObject();

        if (isEmpty(uploadedFiles)) {
            error("Please choose file with tagset before uploading");
            return;
        }
        else if (isNull(project.getId())) {
            error("Project not yet created, please save project details!");
            return;
        }
        if (aForm.getModelObject().format.equals(
                ExportedTagSetConstant.JSON_FORMAT)) {
            for (FileUpload tagFile : uploadedFiles) {
                InputStream tagInputStream;
                try {
                    tagInputStream = tagFile.getInputStream();
                    if (aForm.getModelObject().overwrite) {
                        JsonImportUtil.importTagSetFromJsonWithOverwrite(project,
                                tagInputStream, annotationService);
                    }
                    else {
                        JsonImportUtil.importTagSetFromJson(project, tagInputStream,
                                annotationService);
                    }
                }
                catch (IOException e) {
                    error("Error Importing TagSet "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
        else if (aForm.getModelObject().format.equals(
                ExportedTagSetConstant.TAB_FORMAT)) {
            for (FileUpload tagFile : uploadedFiles) {
                InputStream tagInputStream;
                try {
                    tagInputStream = tagFile.getInputStream();
                    String text = IOUtils.toString(tagInputStream, "UTF-8");
                    Map<String, String> tabbedTagsetFromFile = ImportUtil
                            .getTagSetFromFile(text);

                    Set<String> listOfTagsFromFile = tabbedTagsetFromFile.keySet();
                    int i = 0;
                    String tagSetName = "";
                    String tagSetDescription = "";
                    String tagsetLanguage = "";
                    de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = null;
                    for (String key : listOfTagsFromFile) {
                        // the first key is the tagset name and its
                        // description
                        if (i == 0) {
                            tagSetName = key;
                            tagSetDescription = tabbedTagsetFromFile.get(key);
                        }
                        // the second key is the tagset language
                        else if (i == 1) {
                            tagsetLanguage = key;
                            // remove and replace the tagset if it
                            // exist
                            if (annotationService.existsTagSet(tagSetName, project)) {
                                // If overwrite is enabled
                                if (aForm.getModelObject().overwrite) {
                                    tagSet = annotationService.getTagSet(tagSetName,
                                            project);
                                    annotationService.removeAllTags(tagSet);
                                }
                                else {
                                    tagSet = new TagSet();
                                    tagSet.setName(JsonImportUtil.copyTagSetName(
                                            annotationService, tagSetName, project));
                                }
                            }
                            else {
                                tagSet = new TagSet();
                                tagSet.setName(tagSetName);
                            }
                            tagSet.setDescription(tagSetDescription
                                    .replace("\\n", "\n"));
                            tagSet.setLanguage(tagsetLanguage);
                            tagSet.setProject(project);
                            annotationService.createTagSet(tagSet);
                        }
                        // otherwise it is a tag entry, add the tag
                        // to the tagset
                        else {
                            Tag tag = new Tag();
                            tag.setDescription(tabbedTagsetFromFile.get(key).replace(
                                    "\\n", "\n"));
                            tag.setName(key);
                            tag.setTagSet(tagSet);
                            annotationService.createTag(tag);
                        }
                        i++;
                    }
                }
                catch (Exception e) {
                    error("Error importing tag set: "
                            + ExceptionUtils.getRootCauseMessage(e));
                    LOG.error("Error importing tag set", e);
                }
            }
        }
        
        try {
            onImportComplete(aTarget);
        }
        catch (Exception e) {
            error("Error importing tag set: " + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Error importing tag set", e);
        }
    }
    
    protected void onImportComplete(AjaxRequestTarget aTarget) throws Exception
    {
        if (importCompleteAction != null) {
            importCompleteAction.accept(aTarget);
        }
    }

    public TagSetImportPanel setImportCompleteAction(AjaxCallback aAction)
    {
        importCompleteAction = aAction;
        return this;
    }

    static class Preferences implements Serializable
    {
        private static final long serialVersionUID = -8602845573913839851L;
        
        String format;
        boolean overwrite;
    }
}
