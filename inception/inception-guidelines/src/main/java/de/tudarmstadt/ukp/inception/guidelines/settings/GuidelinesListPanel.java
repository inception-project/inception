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
package de.tudarmstadt.ukp.inception.guidelines.settings;

import java.io.IOException;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.guidelines.GuidelinesService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.wicket.OverviewListChoice;

public class GuidelinesListPanel
    extends Panel
{
    private static final long serialVersionUID = -60496323542183729L;

    private static final Logger LOG = LoggerFactory.getLogger(GuidelinesListPanel.class);

    private @SpringBean GuidelinesService guidelinesService;

    private BootstrapModalDialog confirmationDialog;
    private OverviewListChoice<String> overviewList;
    private IModel<Project> project;
    private IModel<String> guideline;

    public GuidelinesListPanel(String aId, IModel<Project> aProject)
    {
        super(aId);

        setOutputMarkupId(true);

        project = aProject;
        guideline = Model.of();

        Form<Void> form = new Form<>("form");
        add(form);

        overviewList = new OverviewListChoice<>("guidelines");
        overviewList.setModel(guideline);
        overviewList.setChoices(LoadableDetachableModel.of(this::listGuidelines));
        form.add(overviewList);

        confirmationDialog = new BootstrapModalDialog("confirmationDialog");
        confirmationDialog.trapFocus();
        add(confirmationDialog);

        LambdaAjaxButton<Void> delete = new LambdaAjaxButton<>("delete", (t, f) -> {
            if (!guideline.isPresent().getObject()) {
                return;
            }

            var dialogContent = new DeleteGuidelinesConfirmationDialogPanel(
                    BootstrapModalDialog.CONTENT_ID, guideline);
            dialogContent.setConfirmAction(this::actionDelete);
            confirmationDialog.open(dialogContent, t);
        });
        form.add(delete);
    }

    private List<String> listGuidelines()
    {
        return guidelinesService.listGuidelines(project.getObject());
    }

    private void actionDelete(AjaxRequestTarget aTarget)
    {
        if (guideline.getObject() == null) {
            error("No guideline selected");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        try {
            guidelinesService.removeGuideline(project.getObject(), guideline.getObject());
        }
        catch (IOException e) {
            LOG.error("Unable to delete document", e);
            error("Unable to delete document: " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
        }
        guideline.setObject(null);
        aTarget.add(getPage());
    }
}
