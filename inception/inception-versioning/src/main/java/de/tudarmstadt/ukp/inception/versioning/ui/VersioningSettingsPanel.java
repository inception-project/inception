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
package de.tudarmstadt.ukp.inception.versioning.ui;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.versioning.VersioningService;

public class VersioningSettingsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 947691148582391801L;

    private static final Logger LOG = LoggerFactory.getLogger(VersioningSettingsPanel.class);

    private @SpringBean VersioningService versioningService;

    private IModel<RepositoryConfig> repositoryConfigModel;
    private IModel<PushConfig> pushConfigModel;

    public VersioningSettingsPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        Project project = aProjectModel.getObject();

        // We create the repository here if it does not exist yet
        try {
            if (!versioningService.repoExists(project)) {
                versioningService.initializeRepo(project);
            }
        }
        catch (GitAPIException e) {
            LOG.error("Error initializing repository: {}", e.getMessage());
            error("Error initializing repository: " + ExceptionUtils.getRootCauseMessage(e));
        }

        // RepositoryConfig
        RepositoryConfig repositoryConfig = new RepositoryConfig();
        repositoryConfig.localPath = versioningService.getRepoDir(project).getAbsolutePath();
        try {
            repositoryConfig.remotePath = versioningService.getRemote(project).orElse("");
        }
        catch (IOException | GitAPIException e) {
            LOG.error("Error getting remote: {}", e.getMessage());
            error("Error getting remote: " + ExceptionUtils.getRootCauseMessage(e));
        }

        repositoryConfigModel = new CompoundPropertyModel<>(repositoryConfig);

        // Settings settingsForm
        Form<RepositoryConfig> settingsForm = new Form<>("settingsForm", repositoryConfigModel);
        settingsForm.setOutputMarkupId(true);
        add(settingsForm);

        // Local path
        TextField<String> localPathField = new TextField<>("localPath");
        settingsForm.add(localPathField);

        // Remote path
        settingsForm.add(new TextField<String>("remotePath").add(new UrlValidator()));
        settingsForm.add(new LambdaAjaxButton<>("setRemoteButton", this::actionSetRemote));

        // Buttons
        settingsForm.add(new LambdaAjaxLink("snapshotProject", this::actionSnapshotProject));

        // Push form
        PushConfig pushConfig = new PushConfig();
        pushConfigModel = new CompoundPropertyModel<>(pushConfig);
        Form<PushConfig> pushForm = new Form<>("pushForm", pushConfigModel);
        pushForm.add(new TextField<>("username"));
        pushForm.add(new PasswordTextField("password"));
        pushForm.add(new LambdaAjaxButton<>("pushButton", this::actionPushToRemote));
        add(pushForm);
    }

    private void actionSetRemote(AjaxRequestTarget aTarget, Form<RepositoryConfig> aForm)
    {
        try {
            versioningService.setRemote(getModelObject(),
                    repositoryConfigModel.getObject().remotePath);
            aTarget.add(this);
            success("Setting remote successful!");
        }
        catch (IOException | GitAPIException | URISyntaxException e) {
            LOG.error("Error setting remote: {}", e.getMessage());
            error("Error setting remote: " + ExceptionUtils.getRootCauseMessage(e));
        }
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private void actionSnapshotProject(AjaxRequestTarget aTarget)
    {
        try {
            versioningService.snapshotCompleteProject(getModelObject(), "Snapshotting");
            aTarget.add(this);
            success("Snapshotting successful!");
        }
        catch (IOException | GitAPIException e) {
            Project project = getModelObject();
            LOG.error("Error snapshotting project [{}]({})", project.getName(), project.getId(), e);
            error("Error snapshotting project: " + ExceptionUtils.getRootCauseMessage(e));
        }

        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private void actionPushToRemote(AjaxRequestTarget aTarget, Form<RepositoryConfig> aForm)
    {
        try {
            PushConfig pushConfig = pushConfigModel.getObject();
            versioningService.pushToOrigin(getModelObject(), pushConfig.username,
                    pushConfig.password);
            aTarget.add(this);
            success("Pushing successful!");
        }
        catch (IOException | GitAPIException e) {
            Project project = getModelObject();
            LOG.error("Error pushing to remote repository in [{}]({})", project.getName(),
                    project.getId(), e);
            error("Error pushing to remote repository: " + ExceptionUtils.getRootCauseMessage(e));
        }

        aTarget.addChildren(getPage(), IFeedback.class);
    }

    @SuppressWarnings("unused")
    private static class RepositoryConfig
        implements Serializable
    {
        private static final long serialVersionUID = 5476961483452271801L;

        private String localPath;
        private String remotePath;
    }

    private static class PushConfig
        implements Serializable
    {
        private static final long serialVersionUID = 3476611483452271801L;

        private String username;
        private String password;
    }
}
