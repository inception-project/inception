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
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.ajax.markup.html.modal.theme.DefaultTheme;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.versioning.VersioningService;

public class VersioningSettingsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 947691148582391801L;

    private static final Logger LOG = LoggerFactory.getLogger(VersioningSettingsPanel.class);

    private @SpringBean VersioningService versioningService;

    private TextField<String> remoteTextField;
    private IModel<RepositoryConfig> repositoryConfigModel;

    public VersioningSettingsPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        Project project = aProjectModel.getObject();

        // RepositoryConfig
        RepositoryConfig repositoryConfig = new RepositoryConfig();
        repositoryConfig.setLocalPath(versioningService.getRepoDir(project).getAbsolutePath());
        try {
            repositoryConfig.setRemotePath(versioningService.getRemote(project).orElse(""));
        }
        catch (IOException | GitAPIException e) {
            LOG.error("Error getting remote: {}", e.getMessage());
            error("Error getting remote: " + ExceptionUtils.getRootCauseMessage(e));
            return;
        }

        repositoryConfigModel = new CompoundPropertyModel<>(repositoryConfig);

        // Form
        Form<RepositoryConfig> form = new Form<>("form", repositoryConfigModel);
        form.setOutputMarkupId(true);
        add(form);

        // Local path
        TextField<String> localPathField = new TextField<>("localPath");
        form.add(localPathField);

        // Modal
        ModalDialog modal = new ModalDialog("modal");
        modal.add(new DefaultTheme());
        modal.setOutputMarkupId(true);

        Fragment modalContent = new Fragment(ModalDialog.CONTENT_ID, "pushModalFragment", this);
        modalContent.add(new Label("pushModalLabel", "Test label"));
        modalContent.add(new LambdaAjaxLink("pushModalPushButton", this::actionPushToRemote));
        modalContent.add(new LambdaAjaxLink("pushModalCancelButton", modal::close));

        modal.setContent(modalContent);
        add(modal);

        // Remote path
        form.add(new TextField<>("remotePath"));
        form.add(new LambdaAjaxButton<>("setRemoteButton", this::actionSetRemote));

        // Buttons
        form.add(new LambdaAjaxLink("snapshotProject", this::actionSnapshotProject));
        form.add(new LambdaAjaxLink("pushButton", modal::open));
    }

    private void actionSnapshotProject(AjaxRequestTarget aTarget)
    {
        try {
            versioningService.snapshotCompleteProject(getModelObject(), "Snapshotting");
            aTarget.add(this);
            info("Snapshotting successful!");
        }
        catch (IOException | GitAPIException e) {
            LOG.error("Error snapshotting project: {}", e.getMessage());
            error("Error snapshotting project: " + ExceptionUtils.getRootCauseMessage(e));
        }

        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private void actionPushToRemote(AjaxRequestTarget aTarget)
    {
        try {
            versioningService.pushToOrigin(getModelObject());
            aTarget.add(this);
            info("Pushing successful!");
        }
        catch (IOException | GitAPIException e) {
            LOG.error("Error pushing to remote repository: {}", e.getMessage());
            error("Error pushing to remote repository: " + ExceptionUtils.getRootCauseMessage(e));
        }

        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private void actionSetRemote(AjaxRequestTarget aTarget, Form<RepositoryConfig> aForm)
    {
        try {
            versioningService.setRemote(getModelObject(),
                    repositoryConfigModel.getObject().remotePath);
            aTarget.add(this);
        }
        catch (IOException | GitAPIException | URISyntaxException e) {
            LOG.error("Error setting remote: {}", e.getMessage());
            error("Error setting remote: " + ExceptionUtils.getRootCauseMessage(e));
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private static class RepositoryConfig
        implements Serializable
    {
        private static final long serialVersionUID = 5476961483452271801L;

        private String localPath;
        private String remotePath;

        public String getLocalPath()
        {
            return localPath;
        }

        public void setLocalPath(String aLocalPath)
        {
            localPath = aLocalPath;
        }

        public String getRemotePath()
        {
            return remotePath;
        }

        public void setRemotePath(String aRemotePath)
        {
            remotePath = aRemotePath;
        }
    }

}
