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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

/**
 * A yes/NO dialog window to confirm if the user is meant to delete the selected annotation or not.
 *
 *
 */
public class YesNoDeleteModalPanel
    extends Panel
{
    private static final long serialVersionUID = 9059154802785333743L;

    public YesNoDeleteModalPanel(String aId, BratAnnotatorModel aBModel, ModalWindow aModalWindow,
            AnnotationDetailEditorPanel aEditor, AnnotationLayer aLayer)
    {
        super(aId);
        add(new YesNoButtonsForm("yesNoButtonsForm", aModalWindow, aBModel, aEditor, aLayer));  
    }

    private class YesNoButtonsForm
        extends Form<Void>
    {
        private static final long serialVersionUID = 2676469299552059617L;

        public YesNoButtonsForm(String id, final ModalWindow modalWindow,
                BratAnnotatorModel aBModel, AnnotationDetailEditorPanel aEditor, AnnotationLayer aLayer)
        {
            super(id);
            add(new AjaxLink<Void>("yesButton")
            {
                private static final long serialVersionUID = 3201659066846248716L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    
                    try {
                        aEditor.actionDelete(aTarget, aBModel);
                        aBModel.getSelection().setAnnotate(true); 
                        aBModel.getSelection().setAnnotation(VID.NONE_ID);
                        aBModel.setSelectedAnnotationLayer(aLayer);
                        aBModel.setDefaultAnnotationLayer(aLayer);
                        aEditor.getSelectedAnnotationLayer().setDefaultModelObject(aLayer.getUiName());
                        aEditor.populateFeatures(null);
                        aTarget.add(aEditor.getAnnotationFeatureForm());
                    }
                    catch (CASRuntimeException | UIMAException | ClassNotFoundException
                            | IOException | BratAnnotationException e) {
                        aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
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
                    aBModel.setDefaultAnnotationLayer(aBModel.getSelectedAnnotationLayer());
                    aTarget.add(aEditor.getAnnotationFeatureForm());
                    modalWindow.close(aTarget);

                }
            });
        }
    }
}
