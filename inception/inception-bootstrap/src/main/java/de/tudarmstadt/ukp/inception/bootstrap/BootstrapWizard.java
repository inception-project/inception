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
package de.tudarmstadt.ukp.inception.bootstrap;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardStep;

import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class BootstrapWizard
    extends Wizard
{
    private static final long serialVersionUID = 4855036643803029655L;

    public BootstrapWizard(String id)
    {
        super(id);
    }

    public BootstrapWizard(String id, IWizardModel wizardModel)
    {
        super(id, wizardModel);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        queue(new LambdaAjaxLink("closeDialog", t -> findParent(ModalDialog.class).close(t)));
    }

    @Override
    public void onActiveStepChanged(final IWizardStep newStep)
    {
        super.onActiveStepChanged(newStep);

        if (newStep instanceof WizardStep) {
            WizardStep step = (WizardStep) newStep;
            getForm().get(HEADER_ID).add(visibleWhen(() -> isNotBlank(step.getTitle())));
        }
    }

    @Override
    protected Component newFeedbackPanel(String id)
    {
        BootstrapFeedbackPanel panel = new BootstrapFeedbackPanel(id);
        panel.setOutputMarkupId(true);
        return panel;
    }

}
