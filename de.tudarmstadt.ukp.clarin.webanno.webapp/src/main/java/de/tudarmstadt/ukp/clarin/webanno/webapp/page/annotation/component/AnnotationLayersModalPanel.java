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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.AnnotationPreferenceModalPanel;

/**
 * @author Seid Muhie Yimam
 *
 */
public class AnnotationLayersModalPanel
    extends Panel
{
    private static final long serialVersionUID = 671214149298791793L;

    public AnnotationLayersModalPanel(String id, final IModel<BratAnnotatorModel> aModel)
    {
        super(id, aModel);
        // dialog window to select annotation layer preferences
        final ModalWindow annotationLayerSelectionModal;
        add(annotationLayerSelectionModal = new ModalWindow("annotationLayerModal"));
        annotationLayerSelectionModal.setOutputMarkupId(true);
        annotationLayerSelectionModal.setInitialWidth(440);
        annotationLayerSelectionModal.setInitialHeight(250);
        annotationLayerSelectionModal.setResizable(true);
        annotationLayerSelectionModal.setWidthUnit("px");
        annotationLayerSelectionModal.setHeightUnit("px");
        annotationLayerSelectionModal
                .setTitle("Annotation Layer and window size configuration Window");

        add(new AjaxLink<Void>("showannotationLayerModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (aModel.getObject().getProject() == null) {
                    target.appendJavaScript("alert('Please open a project first!')");
                }
                else {

                    annotationLayerSelectionModal.setContent(new AnnotationPreferenceModalPanel(
                            annotationLayerSelectionModal.getContentId(),
                            annotationLayerSelectionModal, aModel.getObject()));

                    annotationLayerSelectionModal
                            .setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                            {
                                private static final long serialVersionUID = 1643342179335627082L;

                                @Override
                                public void onClose(AjaxRequestTarget target)
                                {
                                    onChange(target);
                                }
                            });
                    annotationLayerSelectionModal.show(target);
                }

            }
        });

    }

    protected void onChange(AjaxRequestTarget aTarget)
    {
        // Overriden in curationPanel
    }

}
