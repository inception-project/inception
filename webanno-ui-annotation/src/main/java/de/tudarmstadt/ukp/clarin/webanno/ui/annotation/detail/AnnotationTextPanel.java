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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;

public class AnnotationTextPanel
    extends Panel
{
    private static final long serialVersionUID = -7722700418471642307L;

    private @SpringBean AnnotationSchemaService annotationService;
    
    private final AnnotationActionHandler actionHandler;

    public AnnotationTextPanel(String aId, AnnotationActionHandler aActionHandler,
            IModel<AnnotatorState> aModel)
    {
        super(aId, aModel);

        actionHandler = aActionHandler;

        add(new Label("selectedText", PropertyModel.of(getModelObject(), "selection.text"))
                .add(visibleWhen(() -> !isRelationSelected())));
        add(new LambdaAjaxLink("jumpToAnnotation", this::actionJumpToAnnotation)
                .add(visibleWhen(() -> !isRelationSelected())));
        
        add(new Label("originText", PropertyModel.of(getModelObject(), "selection.originText"))
                .add(visibleWhen(() -> isRelationSelected())));
        add(new Label("targetText", PropertyModel.of(getModelObject(), "selection.targetText"))
                .add(visibleWhen(() -> isRelationSelected())));
        add(new LambdaAjaxLink("jumpToOrigin", this::actionJumpToOrigin)
                .add(visibleWhen(() -> isRelationSelected())));
        add(new LambdaAjaxLink("jumpToTarget", this::actionJumpToTarget)
                .add(visibleWhen(() -> isRelationSelected())));
    }

    private boolean isRelationSelected()
    {
        return getModelObject().getSelection().isArc();
    }
    
    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    public AnnotationPageBase getEditorPage()
    {
        return (AnnotationPageBase) getPage();
    }
    
    private void actionJumpToAnnotation(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        actionHandler.actionSelectAndJump(aTarget, getModelObject().getSelection().getAnnotation());
    }

    private void actionJumpToOrigin(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        RelationAdapter typeAdapter = (RelationAdapter) annotationService
                .getAdapter(getModelObject().getSelectedAnnotationLayer());

        if (typeAdapter.getAttachFeatureName() == null) {
            actionHandler.actionSelectAndJump(aTarget,
                    new VID(getModelObject().getSelection().getOrigin()));
            return;
        }
        
        CAS cas = getEditorPage().getEditorCas();
        AnnotationFS fs = selectAnnotationByAddr(cas, getModelObject().getSelection().getOrigin());
        fs = FSUtil.getFeature(fs, typeAdapter.getAttachFeatureName(), AnnotationFS.class);
        actionHandler.actionSelectAndJump(aTarget, new VID(fs));
    }

    private void actionJumpToTarget(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        RelationAdapter typeAdapter = (RelationAdapter) annotationService
                .getAdapter(getModelObject().getSelectedAnnotationLayer());

        if (typeAdapter.getAttachFeatureName() == null) {
            actionHandler.actionSelectAndJump(aTarget,
                    new VID(getModelObject().getSelection().getTarget()));
            return;
        }
        
        CAS cas = getEditorPage().getEditorCas();
        AnnotationFS fs = selectAnnotationByAddr(cas, getModelObject().getSelection().getTarget());
        fs = FSUtil.getFeature(fs, typeAdapter.getAttachFeatureName(), AnnotationFS.class);
        actionHandler.actionSelectAndJump(aTarget, new VID(fs));
    }
}
