/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;

/**
 * A yes/NO dialog window to confirm if the user is meant to delete the selected annotation or not.
 *
 *
 */
public class DeleteOrReplaceAnnotationModalPanel
    extends Panel
{
    private static final long serialVersionUID = 9059154802785333743L;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public DeleteOrReplaceAnnotationModalPanel(String aId, AnnotatorState aBModel,
            ModalWindow aModalWindow, AnnotationDetailEditorPanel aEditor, AnnotationLayer aLayer,
            boolean aIsReplace)
    {
        super(aId);
        add(new YesNoButtonsForm("yesNoButtonsForm", aModalWindow, aBModel, aEditor, aLayer,
                aIsReplace));
    }

    private class YesNoButtonsForm
        extends Form<Void>
    {
        private static final long serialVersionUID = 2676469299552059617L;

        public YesNoButtonsForm(String id, final ModalWindow modalWindow,
                AnnotatorState aState, AnnotationDetailEditorPanel aEditor,
                AnnotationLayer aLayer, boolean aIsReplace)
        {
            super(id);
            add(new AjaxLink<Void>("yesButton")
            {
                private static final long serialVersionUID = 3201659066846248716L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    try {
                        if (aIsReplace) {
                            // Delete current annotation
                            aEditor.actionDelete(aTarget);
                            
                            // Set up the action to create the replacement annotation
                            aState.getAction().setAnnotate(true);
                            aState.getSelection().setAnnotation(VID.NONE_ID);
                            aState.setSelectedAnnotationLayer(aLayer);
                            aState.setDefaultAnnotationLayer(aLayer);
                            aEditor.getSelectedAnnotationLayer()
                                    .setDefaultModelObject(aLayer.getUiName());
                            aEditor.loadFeatureEditorModels(aTarget);
                            
                            // Create the replacement annotation
                            aEditor.actionAnnotate(aTarget);
                            aTarget.add(aEditor.getAnnotationFeatureForm());
                        }
                        else {
                            aEditor.actionDelete(aTarget);
                        }
                    }
                    catch (Exception e) {
                        log.error(e.getMessage(), e);
                        error(e.getMessage());
                    }
                    modalWindow.close(aTarget);
                }
            });

            add(new AjaxLink<Void>("noButton")
            {
                private static final long serialVersionUID = -9043394507438053205L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    if (aIsReplace) {
                        aState.setDefaultAnnotationLayer(aState.getSelectedAnnotationLayer());
                        aTarget.add(aEditor.getAnnotationFeatureForm());
                        modalWindow.close(aTarget);
                    }
                    else {
                        modalWindow.close(aTarget);
                    }

                }
            });
        }
    }
}
