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

import static de.tudarmstadt.ukp.inception.curation.settings.CurationManagerPrefs.KEY_CURATION_MANAGER_PREFS;
import static de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarManagerPrefs.KEY_CURATION_SIDEBAR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarManagerPrefs;
import de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarProperties;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class CurationProjectSettingsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 4618192360418016955L;

    private static final String CID_FORM = "form";
    private static final String CID_MERGE_STRATEGY = "mergeStrategy";
    private static final String CID_SAVE = "save";
    private static final String CID_NAVIGATION_PREFS = "navigationPrefs";

    private @SpringBean CurationService curationService;
    private @SpringBean ProjectService projectService;
    private @SpringBean PreferencesService preferencesService;
    private @SpringBean CurationSidebarProperties curationSidebarProperties;

    private IModel<CurationWorkflow> curationWorkflowModel;
    private CompoundPropertyModel<CurationSidebarManagerPrefs> curationSidebarPrefs;
    private CompoundPropertyModel<CurationManagerPrefs> curationPrefs;

    private MarkupContainer mergeStrategyPanel;

    public CurationProjectSettingsPanel(String aId, IModel<Project> aProject)
    {
        super(aId, CompoundPropertyModel.of(aProject));
        setOutputMarkupPlaceholderTag(true);

        var form = new LambdaForm<Project>(CID_FORM, CompoundPropertyModel.of(aProject));
        add(form);

        curationWorkflowModel = Model.of(loadCurationWorkflow());
        curationSidebarPrefs = new CompoundPropertyModel<>(Model.of(loadSidebarPrefs()));
        curationPrefs = new CompoundPropertyModel<>(Model.of(loadCurationPrefs()));

        mergeStrategyPanel = new MergeStrategyPanel(CID_MERGE_STRATEGY, curationWorkflowModel);
        form.add(mergeStrategyPanel);

        form.add(new CheckBox("anonymousCuration").setOutputMarkupPlaceholderTag(true));

        form.add(new CheckBox("autoMergeCurationSidebar") //
                .setModel(curationSidebarPrefs.bind("autoMergeCurationSidebar")) //
                .add(visibleWhen(curationSidebarProperties::isEnabled)) //
                .setOutputMarkupPlaceholderTag(true));

        form.add(new DropDownChoice<CurationPageType>("curationPageType") //
                .setChoiceRenderer(new EnumChoiceRenderer<>(this)) //
                .setChoices(asList(CurationPageType.values())) //
                .setModel(curationPrefs.bind("curationPageType")) //
                .setOutputMarkupPlaceholderTag(true));

        queue(new CurationNavigationUserPrefsPanel(CID_NAVIGATION_PREFS, aProject));

        form.add(new LambdaAjaxButton<>(CID_SAVE, this::actionSave).triggerAfterSubmit());
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

    private CurationSidebarManagerPrefs loadSidebarPrefs()
    {
        return preferencesService.loadDefaultTraitsForProject(KEY_CURATION_SIDEBAR_MANAGER_PREFS,
                getModelObject());
    }

    private CurationManagerPrefs loadCurationPrefs()
    {
        return preferencesService.loadDefaultTraitsForProject(KEY_CURATION_MANAGER_PREFS,
                getModelObject());
    }

    public void actionSave(AjaxRequestTarget aTarget, Form<Project> aForm)
    {
        preferencesService.saveDefaultTraitsForProject(KEY_CURATION_SIDEBAR_MANAGER_PREFS,
                getModelObject(), curationSidebarPrefs.getObject());

        preferencesService.saveDefaultTraitsForProject(KEY_CURATION_MANAGER_PREFS, getModelObject(),
                curationPrefs.getObject());

        curationService.createOrUpdateCurationWorkflow(curationWorkflowModel.getObject());

        projectService.updateProject(aForm.getModelObject());

        success("Settings saved");
        aTarget.addChildren(getPage(), IFeedback.class);
    }
}
