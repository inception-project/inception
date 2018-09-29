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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.CheckBox;
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

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.ImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;

public class ProjectImportPanel
    extends Panel
{
    private static final long serialVersionUID = 4612767288793876015L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectImportPanel.class);
    
    //private @SpringBean ImportService importService;
    private @SpringBean ProjectExportService exportService;
    
    private IModel<Project> selectedModel;
    private IModel<Preferences> preferences;
    private FileUploadField fileUpload;

    public ProjectImportPanel(String aId, IModel<Project> aModel)
    {
        super(aId);
        
        preferences = Model.of(new Preferences());
        selectedModel = aModel;
        
        Form<Preferences> form = new Form<>("form", CompoundPropertyModel.of(preferences));
        form.add(new CheckBox("generateUsers"));
        form.add(fileUpload = new FileUploadField("content", new ListModel<>()));
        fileUpload.setRequired(true);
        form.add(new LambdaAjaxButton<>("import", this::actionImport));
        add(form);
    }

    private void actionImport(AjaxRequestTarget aTarget, Form<Preferences> aForm)
    {
        List<FileUpload> exportedProjects = fileUpload.getFileUploads();
        boolean aGenerateUsers = preferences.getObject().generateUsers;
        // import multiple projects!
        Project importedProject = null;
        for (FileUpload exportedProject : exportedProjects) {
            try {
                // Workaround for WICKET-6425
                File tempFile = File.createTempFile("webanno-training", null);
                try (
                        InputStream is = new BufferedInputStream(exportedProject.getInputStream());
                        OutputStream os = new FileOutputStream(tempFile);
                ) {
                    if (!ZipUtils.isZipStream(is)) {
                        throw new IOException("Invalid ZIP file");
                    }
                    IOUtils.copyLarge(is, os);
                    
                    if (!ImportUtil.isZipValidWebanno(tempFile)) {
                        throw new IOException("ZIP file is not a WebAnno project archive");
                    }
                    
                    //importedProject = importService.importProject(tempFile, aGenerateUsers);
                    ProjectImportRequest request = new ProjectImportRequest(aGenerateUsers);
                    importedProject = exportService.importProject(request, new ZipFile(tempFile));
                }
                finally {
                    tempFile.delete();
                }
            }
            catch (Exception e) {
                aTarget.addChildren(getPage(), IFeedback.class);
                error("Error importing project: " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Error importing project", e);
            }
        }
        
        if (importedProject != null) {
            selectedModel.setObject(importedProject);
            aTarget.add(getPage());
        }
    }

    static class Preferences implements Serializable
    {
        private static final long serialVersionUID = 3821654370145608038L;
        boolean generateUsers;
    }
}
