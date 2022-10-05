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
package de.tudarmstadt.ukp.inception.ui.project.dangerzone;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;

public class ProjectDangerZonePanel
    extends Panel
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = 7116805031202597222L;

    private @SpringBean ProjectService projectService;

    private ChallengeResponseDialog deleteProjectDialog;

    public ProjectDangerZonePanel(String id, IModel<Project> aModel)
    {
        super(id, aModel);

        var deleteButton = new LambdaAjaxLink("delete", this::actionDelete);
        deleteButton.add(enabledWhen(getModel().map(p -> p.getId() != null).orElse(false)));
        add(deleteButton);

        IModel<String> projectNameModel = PropertyModel.of(getModel(), "name");
        add(deleteProjectDialog = new ChallengeResponseDialog("deleteProjectDialog",
                new StringResourceModel("DeleteProjectDialog.title", this),
                new StringResourceModel("DeleteProjectDialog.text", this).setModel(getModel())
                        .setParameters(projectNameModel),
                projectNameModel));
        deleteProjectDialog.setConfirmAction(this::actionDeletePerform);
    }

    @SuppressWarnings("unchecked")
    public IModel<Project> getModel()
    {
        return (IModel<Project>) getDefaultModel();
    }

    private void actionDelete(AjaxRequestTarget aTarget)
    {
        deleteProjectDialog.show(aTarget);
    }

    private void actionDeletePerform(AjaxRequestTarget aTarget)
    {
        try {
            projectService.removeProject(getModel().getObject());
            setResponsePage(getApplication().getHomePage());
        }
        catch (IOException e) {
            LOG.error("Unable to remove project :" + ExceptionUtils.getRootCauseMessage(e));
            error("Unable to remove project " + ":" + ExceptionUtils.getRootCauseMessage(e));
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }
}
