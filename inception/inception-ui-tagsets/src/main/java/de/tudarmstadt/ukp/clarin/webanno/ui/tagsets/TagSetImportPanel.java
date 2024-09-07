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

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.io.IOUtils.buffer;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.io.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapFileInputField;
import de.tudarmstadt.ukp.inception.export.TagsetImportExportUtils;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil;

public class TagSetImportPanel
    extends Panel
{
    private static final long serialVersionUID = 4612767288793876015L;

    private static final Logger LOG = LoggerFactory.getLogger(TagSetImportPanel.class);

    private @SpringBean AnnotationSchemaService annotationService;

    private IModel<Project> selectedProject;
    private IModel<Preferences> preferences;
    private BootstrapFileInputField fileUpload;

    public TagSetImportPanel(String aId, IModel<Project> aModel)
    {
        super(aId);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        preferences = Model.of(new Preferences());
        selectedProject = aModel;

        Form<Preferences> form = new Form<>("form", CompoundPropertyModel.of(preferences));

        form.add(new CheckBox("overwrite").setOutputMarkupId(true));

        form.add(fileUpload = new BootstrapFileInputField("content", new ListModel<>()));
        fileUpload.getConfig().showPreview(false);
        fileUpload.getConfig().showUpload(false);
        fileUpload.getConfig().showRemove(false);
        fileUpload.setRequired(true);

        form.add(new LambdaAjaxButton<>("import", this::actionImport));

        add(form);
    }

    private void actionImport(AjaxRequestTarget aTarget, Form<Preferences> aForm)
    {
        List<FileUpload> uploadedFiles = fileUpload.getFileUploads();
        if (isEmpty(uploadedFiles)) {
            error("Please choose file with tagset before uploading");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        for (FileUpload tagFile : uploadedFiles) {
            try (var is = buffer(new BOMInputStream(tagFile.getInputStream()), 8 * 1024)) {
                is.mark(32);
                int firstChar = is.read();
                is.reset();
                TagSet tagset;
                if (firstChar == '{') {
                    tagset = TagsetImportExportUtils.importTagsetFromJson(annotationService,
                            selectedProject.getObject(), is, aForm.getModelObject().overwrite);
                }
                else {
                    tagset = TagsetImportExportUtils.importTagsetFromTabSeparated(annotationService,
                            selectedProject.getObject(), is, aForm.getModelObject().overwrite);
                }

                success("Imported tagset: [" + tagset.getName() + "]");
                aTarget.addChildren(getPage(), IFeedback.class);
            }
            catch (Exception e) {
                WicketExceptionUtil.handleException(LOG, this, aTarget, e);
            }
        }

        aTarget.add(findParent(ProjectSettingsPanelBase.class));
    }

    static class Preferences
        implements Serializable
    {
        private static final long serialVersionUID = -8602845573913839851L;

        String format;
        boolean overwrite;
    }
}
