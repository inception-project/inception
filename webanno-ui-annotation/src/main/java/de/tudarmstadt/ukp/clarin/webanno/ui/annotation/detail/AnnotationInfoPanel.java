/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.io.IOException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;

public class AnnotationInfoPanel extends Panel
{
    private static final long serialVersionUID = -2911353962253404751L;

    public AnnotationInfoPanel(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId, aModel);

        setOutputMarkupPlaceholderTag(true);
        
        add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()));
        add(createSelectedAnnotationTypeLabel());
        add(createSelectedTextLabel());
        add(createJumpToAnnotationLink());
        add(createSelectedAnnotationLayerLabel());
    }
    
    public AnnotationPageBase getEditorPage()
    {
        return (AnnotationPageBase) getPage();
    }
    
    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }
    
    private Label createSelectedAnnotationLayerLabel()
    {
        Label label = new Label("selectedAnnotationLayer",
                CompoundPropertyModel.of(getDefaultModel()).bind("selectedAnnotationLayer.uiName"));
        label.setOutputMarkupPlaceholderTag(true);
        label.add(visibleWhen(() -> getModelObject().getPreferences().isRememberLayer()));
        return label;
    }

    private Label createSelectedAnnotationTypeLabel()
    {
        Label label = new Label("selectedAnnotationType", LoadableDetachableModel.of(() -> {
            try {
                AnnotationDetailEditorPanel editorPanel = findParent(
                        AnnotationDetailEditorPanel.class);
                return String.valueOf(selectFsByAddr(editorPanel.getEditorCas(),
                        getModelObject().getSelection().getAnnotation().getId())).trim();
            }
            catch (IOException e) {
                return "";
            }
        }));
        label.setOutputMarkupPlaceholderTag(true);
        // We show the extended info on the selected annotation only when run in development mode
        label.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()
                && DEVELOPMENT.equals(getApplication().getConfigurationType())));
        return label;
    }


    
    private Component createSelectedTextLabel()
    {
        return new Label("selectedText", PropertyModel.of(getModelObject(), "selection.text"))
                .setOutputMarkupId(true);
    }
    
    private Component createJumpToAnnotationLink()
    {
        return new LambdaAjaxLink("jumpToAnnotation", this::actionJumpToAnnotation)
                .setOutputMarkupId(true);
    }
    
    private void actionJumpToAnnotation(AjaxRequestTarget aTarget) throws IOException
    {
        AnnotatorState state = getModelObject();
        
        getEditorPage().actionShowSelectedDocument(aTarget, state.getDocument(),
                state.getSelection().getBegin(), state.getSelection().getEnd());
    }
}
