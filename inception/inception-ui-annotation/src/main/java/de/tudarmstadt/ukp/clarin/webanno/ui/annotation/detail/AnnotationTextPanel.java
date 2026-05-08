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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import jakarta.persistence.NoResultException;

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
        add(new LambdaAjaxLink("jumpToAnnotation", this::actionJumpToAnnotation) //
                .setAlwaysEnabled(true) // avoid disabling in read-only mode
                .add(visibleWhen(() -> !isRelationSelected())));

        add(new Label("originName", LoadableDetachableModel.of(this::getOriginName))
                .add(visibleWhen(() -> isRelationSelected())));
        add(new Label("originText", PropertyModel.of(getModelObject(), "selection.originText"))
                .add(visibleWhen(() -> isRelationSelected())));
        add(new Label("targetName", LoadableDetachableModel.of(this::getTargetName))
                .add(visibleWhen(() -> isRelationSelected())));
        add(new Label("targetText", PropertyModel.of(getModelObject(), "selection.targetText"))
                .add(visibleWhen(() -> isRelationSelected())));
        add(new LambdaAjaxLink("jumpToOrigin", this::actionJumpToOrigin) //
                .setAlwaysEnabled(true) // avoid disabling in read-only mode
                .add(visibleWhen(() -> isRelationSelected())));
        add(new LambdaAjaxLink("jumpToTarget", this::actionJumpToTarget) //
                .setAlwaysEnabled(true) // avoid disabling in read-only mode
                .add(visibleWhen(() -> isRelationSelected())));
    }

    private String getOriginName()
    {
        try {
            return annotationService
                    .getFeature(FEAT_REL_SOURCE, getModelObject().getSelectedAnnotationLayer())
                    .getUiName();
        }
        catch (NoResultException e) {
            return "From";
        }
    }

    private String getTargetName()
    {
        try {
            return annotationService
                    .getFeature(FEAT_REL_TARGET, getModelObject().getSelectedAnnotationLayer())
                    .getUiName();
        }
        catch (NoResultException e) {
            return "To";
        }
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
        actionHandler.actionSelectAndJump(aTarget,
                new VID(getModelObject().getSelection().getOrigin()));
    }

    private void actionJumpToTarget(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        actionHandler.actionSelectAndJump(aTarget,
                new VID(getModelObject().getSelection().getTarget()));
    }
}
