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
package de.tudarmstadt.ukp.clarin.webanno.ui.tagsets;

import static java.util.Objects.isNull;
import static org.apache.commons.csv.CSVFormat.EXCEL;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaPanel;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.inception.support.wicket.InputStreamResourceStream;
import de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil;

public class TagSetEditorPanel
    extends LambdaPanel
{
    private static final long serialVersionUID = 3084260865116114184L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean AnnotationSchemaService annotationSchemaService;

    private BootstrapModalDialog confirmationDialog;

    private IModel<Project> selectedProject;
    private IModel<TagSet> selectedTagSet;
    private IModel<Tag> selectedTag;

    public TagSetEditorPanel(String aId, IModel<Project> aProject, IModel<TagSet> aTagSet,
            IModel<Tag> aSelectedTag)
    {
        super(aId, aTagSet);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        selectedProject = aProject;
        selectedTagSet = aTagSet;
        selectedTag = aSelectedTag;

        queue(new Form<>("form", CompoundPropertyModel.of(aTagSet)));

        queue(new TextField<String>("name") //
                .add(new TagSetExistsValidator()) //
                .setRequired(true));
        queue(new TextField<String>("language"));
        queue(new TextArea<String>("description"));
        queue(new CheckBox("createTag").setOutputMarkupId(true));

        queue(new LambdaAjaxButton<>("save", this::actionSave));
        queue(new LambdaAjaxLink("delete", this::actionDelete)
                .onConfigure(_this -> _this.setVisible(aTagSet.getObject().getId() != null)));
        queue(new LambdaAjaxLink("cancel", this::actionClearSelectedTagset));

        queue(new AjaxDownloadLink("exportTagsetAsJson", this::exportTagsetAsJson));
        queue(new AjaxDownloadLink("exportTagsetAsTabSeparated", this::exportTagsetAsTabSeparated));

        confirmationDialog = new BootstrapModalDialog("confirmationDialog");
        confirmationDialog.trapFocus();
        add(confirmationDialog);
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Tag> aForm)
    {
        if (isNull(selectedTagSet.getObject().getId())) {
            if (annotationSchemaService.existsTagSet(selectedTagSet.getObject().getName(),
                    selectedProject.getObject())) {
                error("Only one tagset per project is allowed!");
            }
        }

        selectedTagSet.getObject().setProject(selectedProject.getObject());
        annotationSchemaService.createTagSet(selectedTagSet.getObject());

        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());

        success("Settings saved");
    }

    private void actionDelete(AjaxRequestTarget aTarget)
    {
        var dialogContent = new TagsetDeletionConfirmationDialogPanel(
                BootstrapModalDialog.CONTENT_ID, selectedTagSet);

        dialogContent.setConfirmAction((_target) -> {
            // If the tagset is used in any features, clear the tagset on these features when
            // the tagset is deleted!
            for (var ft : annotationSchemaService
                    .listAnnotationFeature(selectedProject.getObject())) {
                if (ft.getTagset() != null && ft.getTagset().equals(selectedTagSet.getObject())) {
                    ft.setTagset(null);
                    annotationSchemaService.createFeature(ft);
                }
            }

            annotationSchemaService.removeTagSet(selectedTagSet.getObject());

            _target.add(getPage());
            actionClearSelectedTagset(_target);
        });

        confirmationDialog.open(dialogContent, aTarget);
    }

    private void actionClearSelectedTagset(AjaxRequestTarget aTarget)
    {
        selectedTagSet.setObject(null);
        selectedTag.setObject(null);

        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }

    private IResourceStream exportTagsetAsJson()
    {
        try {
            String json = exportTagsetToJson(annotationSchemaService, selectedTagSet.getObject());
            return new InputStreamResourceStream(new ByteArrayInputStream(json.getBytes("UTF-8")),
                    "tagset.json");
        }
        catch (Exception e) {
            WicketExceptionUtil.handleException(LOG, this, e);
            return null;
        }
    }

    private IResourceStream exportTagsetAsTabSeparated()
    {
        try {
            String json = exportTagsetToTsv(annotationSchemaService, selectedTagSet.getObject());
            return new InputStreamResourceStream(new ByteArrayInputStream(json.getBytes("UTF-8")),
                    "tagset.tsv");
        }
        catch (Exception e) {
            WicketExceptionUtil.handleException(LOG, this, e);
            return null;
        }
    }

    private String exportTagsetToTsv(AnnotationSchemaService aSchemaService, TagSet tagSet)
        throws IOException
    {
        StringWriter buf = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(buf, EXCEL)) {
            String tagSetDescription = tagSet.getDescription() == null ? ""
                    : tagSet.getDescription();
            printer.printRecord(tagSet.getName(), tagSetDescription.replace("\n", "\\n"));
            for (Tag tag : annotationSchemaService.listTags(tagSet)) {
                String tagDescription = tag.getDescription() == null ? "" : tag.getDescription();
                printer.printRecord(tag.getName(), tagDescription.replace("\n", "\\n"));
            }
        }
        return buf.toString();
    }

    private String exportTagsetToJson(AnnotationSchemaService aSchemaService, TagSet tagSet)
        throws IOException
    {
        ExportedTagSet exTagSet = new ExportedTagSet();
        exTagSet.setDescription(tagSet.getDescription());
        exTagSet.setLanguage(tagSet.getLanguage());
        exTagSet.setName(tagSet.getName());

        List<ExportedTag> exportedTags = new ArrayList<>();
        for (Tag tag : aSchemaService.listTags(tagSet)) {
            ExportedTag exportedTag = new ExportedTag();
            exportedTag.setDescription(tag.getDescription());
            exportedTag.setName(tag.getName());
            exportedTags.add(exportedTag);
        }
        exTagSet.setTags(exportedTags);
        return JSONUtil.toPrettyJsonString(exTagSet);
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
