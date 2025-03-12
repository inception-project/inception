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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy.authorize;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapFileInputField;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.project.export.ProjectImportExportUtils;
import de.tudarmstadt.ukp.inception.support.io.ZipUtils;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class ProjectImportPanel
    extends Panel
{
    private static final long serialVersionUID = 4612767288793876015L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectImportPanel.class);

    private @SpringBean ProjectExportService exportService;
    private @SpringBean UserDao userRepository;

    private IModel<Project> selectedModel;
    private IModel<Preferences> preferences;
    private BootstrapFileInputField fileUpload;

    public ProjectImportPanel(String aId, IModel<Project> aModel)
    {
        super(aId);

        preferences = Model.of(new Preferences());
        selectedModel = aModel;

        var form = new Form<>("form", CompoundPropertyModel.of(preferences));

        // Only administrators who can access user management can also use the "create missing
        // users" checkbox. Also, the option is only available when importing permissions in the
        // first place.
        var generateUsers = new CheckBox("generateUsers");
        generateUsers.setOutputMarkupPlaceholderTag(true);
        generateUsers.add(visibleWhen(() -> preferences.getObject().importPermissions));
        form.add(generateUsers);
        authorize(generateUsers, Component.RENDER, ROLE_ADMIN.name());

        var importPermissions = new CheckBox("importPermissions");
        importPermissions.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target -> {
            if (!preferences.getObject().importPermissions) {
                preferences.getObject().generateUsers = false;
            }
            _target.add(generateUsers);
        }));
        form.add(importPermissions);
        authorize(importPermissions, Component.RENDER, ROLE_ADMIN.name());

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
        var exportedProjects = fileUpload.getFileUploads();

        var currentUser = userRepository.getCurrentUser();
        var currentUserIsAdministrator = userRepository.isAdministrator(currentUser);
        var currentUserIsProjectCreator = userRepository.isProjectCreator(currentUser);

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

        // If the current user is an administrator and importing of permissions is *DISABLED*, we
        // configure the current user as a project manager. But if importing of permissions is
        // *ENABLED*, we do not set the admin up as a project manager because we would assume that
        // the admin wants to restore a project (maybe one exported from another instance) and in
        // that case we want to maintain the permissions the project originally had without adding
        // the admin as a manager.
        User manager = null;
        if (currentUserIsAdministrator) {
            if (!importPermissions) {
                manager = currentUser;
            }
        }
        // If the current user is NOT an admin but a project creator then we assume that the user is
        // importing the project for own use, so we add the user as a project manager.
        else if (currentUserIsProjectCreator) {
            manager = currentUser;
        }

        List<Project> importedProjects = new ArrayList<>();
        for (var exportedProject : exportedProjects) {
            var request = ProjectImportRequest.builder() //
                    .withCreateMissingUsers(createMissingUsers) //
                    .withImportPermissions(importPermissions) //
                    .withManager(manager) //
                    .build();

            try {
                // Workaround for WICKET-6425
                var tempFile = File.createTempFile("project-import", null);
                try (var is = new BufferedInputStream(exportedProject.getInputStream());
                        var os = new FileOutputStream(tempFile);) {
                    if (!ZipUtils.isZipStream(is)) {
                        throw new IOException("Invalid ZIP file");
                    }
                    IOUtils.copyLarge(is, os);

                    if (!ProjectImportExportUtils.isValidProjectArchive(tempFile)) {
                        throw new IOException(
                                "Uploaded file is not an INCEpTION/WebAnno project archive");
                    }

                    importedProjects
                            .add(exportService.importProject(request, new ZipFile(tempFile)));
                }
                finally {
                    tempFile.delete();

                    request.getMessages().forEach(m -> getSession().warn(m));
                }
            }
            catch (Exception e) {
                error("Error importing project: " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Error importing project", e);
            }
        }

        aTarget.addChildren(getPage(), IFeedback.class);

        if (!importedProjects.isEmpty() && selectedModel != null) {
            selectedModel.setObject(importedProjects.get(importedProjects.size() - 1));
        }

        send(this, BUBBLE, new AjaxProjectImportedEvent(aTarget, importedProjects));
    }

    static class Preferences
        implements Serializable
    {
        private static final long serialVersionUID = 3821654370145608038L;
        boolean generateUsers;
        boolean importPermissions = true;
    }
}
