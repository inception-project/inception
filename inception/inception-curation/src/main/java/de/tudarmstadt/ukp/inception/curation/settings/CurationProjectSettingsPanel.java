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
package de.tudarmstadt.ukp.inception.curation.settings;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;

public class CurationProjectSettingsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 4618192360418016955L;

    private static final String MID_FORM = "form";
    private static final String MID_MERGE_STRATEGY = "mergeStrategy";
    private static final String MID_SAVE = "save";

    private @SpringBean CurationService curationService;
    private @SpringBean ProjectService projectService;

    private IModel<CurationWorkflow> curationWorkflowModel;

    private MarkupContainer mergeStrategyPanel;

    public CurationProjectSettingsPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        setOutputMarkupPlaceholderTag(true);

        var form = new Form<Project>(MID_FORM, CompoundPropertyModel.of(aProjectModel));
        add(form);

        curationWorkflowModel = LoadableDetachableModel.of(this::loadCurationWorkflow);

        mergeStrategyPanel = new MergeStrategyPanel(MID_MERGE_STRATEGY, curationWorkflowModel);
        form.add(mergeStrategyPanel);

        form.add(new CheckBox("anonymousCuration").setOutputMarkupPlaceholderTag(true));

        form.add(new LambdaAjaxButton<>(MID_SAVE, this::actionSave).triggerAfterSubmit());
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();

        // Needs to be explicitly notified due to the lambda model
        mergeStrategyPanel.modelChanged();
    }

    private CurationWorkflow loadCurationWorkflow()
    {
        return curationService.readOrCreateCurationWorkflow(getModelObject());
    }

    public void actionSave(AjaxRequestTarget aTarget, Form<Project> aForm)
    {
        curationService.createOrUpdateCurationWorkflow(curationWorkflowModel.getObject());

        projectService.updateProject(aForm.getModelObject());

        success("Settings saved");
        aTarget.addChildren(getPage(), IFeedback.class);
    }
}
