/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.automation;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AutomationModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.MiraTemplateModalPanel;

/**
 * A panel used by {@link AutomationPage} to configure the MIRA template for training and prediction
 *
 * @author Seid Muhie Yimam
 *
 */
public class AutomationTemplateModalPanel
    extends Panel
{
    private static final long serialVersionUID = 671214149298791793L;

    private boolean closeButtonClicked;

    public AutomationTemplateModalPanel(String id, final IModel<BratAnnotatorModel> aBModel,
            final IModel<AutomationModel> aModel)
    {
        super(id, aBModel);
        // dialog window to select annotation layer preferences
        final ModalWindow automationTemplateModal;
        add(automationTemplateModal = new ModalWindow("automationTemplateModal"));
        automationTemplateModal.setOutputMarkupId(true);
        automationTemplateModal.setInitialWidth(450);
        automationTemplateModal.setInitialHeight(450);
        automationTemplateModal.setResizable(true);
        automationTemplateModal.setWidthUnit("px");
        automationTemplateModal.setHeightUnit("px");
        automationTemplateModal.setTitle("MIRA automation Template modal Window");
        automationTemplateModal.setCloseButtonCallback(new ModalWindow.CloseButtonCallback()
        {
            private static final long serialVersionUID = -5423095433535634321L;

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget aTarget)
            {
                closeButtonClicked = true;
                return true;
            }
        });

        add(new AjaxLink<Void>("showAutomationTemplateModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (aBModel.getObject().getProject() == null) {
                    target.appendJavaScript("alert('Please open a project first!')");
                }
                else {
                    closeButtonClicked = false;

                    automationTemplateModal.setContent(new MiraTemplateModalPanel(
                            automationTemplateModal.getContentId(), automationTemplateModal,
                            aBModel.getObject(), aModel.getObject())
                    {

                        private static final long serialVersionUID = -3434069761864809703L;

                        @Override
                        protected void onCancel(AjaxRequestTarget aTarget)
                        {
                            closeButtonClicked = true;
                        };
                    });

                    automationTemplateModal
                            .setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                            {
                                private static final long serialVersionUID = 1643342179335627082L;

                                @Override
                                public void onClose(AjaxRequestTarget target)
                                {
                                }
                            });
                    automationTemplateModal.show(target);
                }

            }
        });

    }

}
