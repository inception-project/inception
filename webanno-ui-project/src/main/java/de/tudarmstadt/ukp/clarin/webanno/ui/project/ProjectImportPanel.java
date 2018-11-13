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

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy.authorize;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.Component;
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
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class ProjectImportPanel
    extends Panel
{
    private static final long serialVersionUID = 4612767288793876015L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectImportPanel.class);
    
    private @SpringBean ProjectExportService exportService;
    private @SpringBean UserDao userRepository;
    
    private IModel<Project> selectedModel;
    private IModel<Preferences> preferences;
    private FileUploadField fileUpload;

    public ProjectImportPanel(String aId, IModel<Project> aModel)
    {
        super(aId);
        
        preferences = Model.of(new Preferences());
        selectedModel = aModel;
        
        Form<Preferences> form = new Form<>("form", CompoundPropertyModel.of(preferences));

        // Only administrators who can access user management can also use the "create missing
        // users" checkbox. Also, the option is only available when importing permissions in the
        // first place.
        CheckBox generateUsers = new CheckBox("generateUsers");
        generateUsers.setOutputMarkupPlaceholderTag(true);
        generateUsers.add(visibleWhen(() -> preferences.getObject().importPermissions));
        form.add(generateUsers);
        authorize(generateUsers, Component.RENDER, ROLE_ADMIN.name());

        CheckBox importPermissions = new CheckBox("importPermissions");
        importPermissions.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target -> {
            if (!preferences.getObject().importPermissions) {
                preferences.getObject().generateUsers = false;
            }
            _target.add(generateUsers);
        }));
        form.add(importPermissions);
        authorize(importPermissions, Component.RENDER, ROLE_ADMIN.name());

        form.add(fileUpload = new FileUploadField("content", new ListModel<>()));
        fileUpload.setRequired(true);
        
        form.add(new LambdaAjaxButton<>("import", this::actionImport));
        
        add(form);
    }

    private void actionImport(AjaxRequestTarget aTarget, Form<Preferences> aForm)
    {
        List<FileUpload> exportedProjects = fileUpload.getFileUploads();
        
        User currentUser = userRepository.getCurrentUser();
        boolean currentUserIsAdministrator = userRepository.isAdministrator(currentUser);
        boolean currentUserIsProjectCreator = userRepository.isProjectCreator(currentUser);
        
        boolean createMissingUsers;
        boolean importPermissions;
        
        // Importing of permissions is only allowed if the importing user is an administrator
        if (currentUserIsAdministrator) {
            createMissingUsers = preferences.getObject().generateUsers;
            importPermissions = preferences.getObject().importPermissions;
        }
        // ... otherwise we force-disable importing of permissions so that the only remaining
        // permission for non-admin users is that they become the managers of projects they import.
        else {
            createMissingUsers = false;
            importPermissions = false;
        }
        
        
        // If the current user is a project creator then we assume that the user is importing the
        // project for own use, so we add the user as a project manager. We do not do this if the
        // user is "just" an administrator but not a project creator.
        Optional<User> manager = currentUserIsProjectCreator ? Optional.of(currentUser)
                : Optional.empty();

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
                    
                    ProjectImportRequest request = new ProjectImportRequest(createMissingUsers,
                            importPermissions, manager);
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
        boolean importPermissions = true;
    }
}
