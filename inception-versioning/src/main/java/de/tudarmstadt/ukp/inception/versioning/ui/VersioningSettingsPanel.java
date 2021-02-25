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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.jgit.api.errors.GitAPIException;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.versioning.VersioningService;

public class VersioningSettingsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 947691448582391801L;

    private @SpringBean VersioningService versioningService;

    private final WebMarkupContainer container;

    public VersioningSettingsPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);

        container = createContainer();
        container.setOutputMarkupId(true);
        add(container);
    }

    private WebMarkupContainer createContainer()
    {
        WebMarkupContainer versioningContainer = new WebMarkupContainer("versioningContainer");
        versioningContainer.setOutputMarkupId(true);

        TextField<String> repoPathField = new TextField<String>("repoPath",
                Model.of(versioningService.getRepoDir(getModelObject()).getAbsolutePath()));

        versioningContainer.add(repoPathField);

        LambdaAjaxLink removeBtn = new LambdaAjaxLink("snapshotProject",
                this::actionSnapshotProject);
        versioningContainer.add(removeBtn);

        return versioningContainer;
    }

    private void actionSnapshotProject(AjaxRequestTarget aTarget)
    {
        try {
            versioningService.snapshotCompleteProject(getModelObject());
        }
        catch (IOException | GitAPIException e) {
            // TODO: Show error message
            e.printStackTrace();
            aTarget.add(container);
        }
    }

}
