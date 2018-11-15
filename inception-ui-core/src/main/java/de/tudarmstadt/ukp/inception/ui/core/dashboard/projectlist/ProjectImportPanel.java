/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static java.util.Arrays.asList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInputField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.ImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;

public class ProjectImportPanel
    extends Panel
{
    private static final long serialVersionUID = 4612767288793876015L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectImportPanel.class);
    
    private @SpringBean ProjectExportService exportService;
    private @SpringBean UserDao userRepository;
    
    private BootstrapFileInputField fileUpload;

    public ProjectImportPanel(String aId)
    {
        super(aId);
        
        Form<Void> form = new Form<>("form");
        
        FileInputConfig config = new FileInputConfig();
        config.initialCaption("Browse project files to import ...");
        config.allowedFileExtensions(asList("zip"));
        config.showPreview(false);
        config.showUpload(true);
        form.add(fileUpload = new BootstrapFileInputField("content", new ListModel<>(), config) {
            private static final long serialVersionUID = -6794141937368512300L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                actionImport(aTarget, null);
            }
        });
        add(form);
    }

    private void actionImport(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        
        List<FileUpload> exportedProjects = fileUpload.getFileUploads();
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
                        throw new IOException("ZIP file is not a valid project archive");
                    }
                    
                    ProjectImportRequest request = new ProjectImportRequest(false, false,
                            Optional.of(userRepository.getCurrentUser()));
                    Project importedProject = exportService.importProject(request,
                            new ZipFile(tempFile));
                    
                    success("Imported project: " + importedProject.getName());
                }
                finally {
                    tempFile.delete();
                }
            }
            catch (Exception e) {
                error("Error importing project: " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Error importing project", e);
            }
        }
        
        // After importing new projects, they should be visible in the overview, but we do not
        // redirect to the imported project. Could do that... maybe could do that if only a single
        // project was imported. Could also highlight the freshly imported projects somehow.
    }
}
