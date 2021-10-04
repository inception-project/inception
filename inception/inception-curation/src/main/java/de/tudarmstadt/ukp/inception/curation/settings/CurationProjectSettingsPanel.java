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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;

public class CurationProjectSettingsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 4618192360418016955L;

    private @SpringBean CurationService curationService;

    private IModel<CurationWorkflow> curationWorkflowModel;

    public CurationProjectSettingsPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        setOutputMarkupPlaceholderTag(true);

        Form<CurationWorkflow> form = new Form<>("form");
        add(form);

        curationWorkflowModel = LoadableDetachableModel.of(this::loadCurationWorkflow);

        form.add(new MergeStrategyPanel("mergeStrategy", curationWorkflowModel));
    }

    private CurationWorkflow loadCurationWorkflow()
    {
        return curationService.readOrCreateCurationWorkflow(getModelObject());
    }

    public void actionSave(AjaxRequestTarget aTarget)
    {
        curationService.createOrUpdateCurationWorkflow(curationWorkflowModel.getObject());

        success("Settings saved");
        aTarget.addChildren(getPage(), IFeedback.class);
    }
}
