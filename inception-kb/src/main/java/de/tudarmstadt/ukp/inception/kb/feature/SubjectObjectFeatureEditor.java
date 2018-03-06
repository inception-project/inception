/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.kb.feature;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class SubjectObjectFeatureEditor extends FeatureEditor {

    private static final long serialVersionUID = 4230722501745589589L;
    private @SpringBean AnnotationSchemaService annotationService;
    private WebMarkupContainer content;

    @SuppressWarnings("rawtypes")
    private Component focusComponent;
    private boolean hideUnconstraintFeature;

    private AnnotationActionHandler actionHandler;
    private IModel<AnnotatorState> stateModel;

    @SuppressWarnings("unused")
    private LinkWithRoleModel roleModel;

    private @SpringBean KnowledgeBaseService kbService;

    public SubjectObjectFeatureEditor(String aId, MarkupContainer aOwner,
                                      AnnotationActionHandler aHandler,
                                      final IModel<AnnotatorState> aStateModel,
                                      final IModel<FeatureState> aFeatureStateModel, String role) {
        super(aId, aOwner, CompoundPropertyModel.of(aFeatureStateModel));

        stateModel = aStateModel;
        actionHandler = aHandler;

        hideUnconstraintFeature = getModelObject().feature.isHideUnconstraintFeature();

        add(new Label("feature", getModelObject().feature.getUiName()));
        content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        add(content);


        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) SubjectObjectFeatureEditor.this
            .getModelObject().value;

        if (links.size() == 0) {
            AnnotatorState state = SubjectObjectFeatureEditor.this.stateModel.getObject();

            roleModel = new LinkWithRoleModel();
            roleModel.role = role;
            links.add(roleModel);
            state.setArmedSlot(SubjectObjectFeatureEditor.this.getModelObject().feature, 0);
        }

        content.add(new Label("role", roleModel.role));
        content.add(createSubjectLabel());
        content.add(focusComponent = createFieldComboBox());
    }

    private Label createSubjectLabel() {
        AnnotatorState state = stateModel.getObject();
        final Label label;
        label = new Label("label", LambdaModel.of(this::getSelectionSlotLabel));
        label.add(new AjaxEventBehavior("click")
        {
            private static final long serialVersionUID = 7633309278417475424L;

            @Override
            protected void onEvent(AjaxRequestTarget aTarget)
            {
                actionToggleArmedState(aTarget);
            }
        });
        label.add(new AttributeAppender("style", new Model<String>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject()
            {
                if (state.isArmedSlot(getModelObject().feature, 0)) {
                    return "; background: orange";
                }
                else {
                    return "";
                }
            }
        }));
        if (!state.isArmedSlot(getModelObject().feature, 0)) {
            label.setDefaultModelObject(roleModel.label);
        }
        return label;
    }

    private DropDownList<KBHandle> createFieldComboBox()
    {
        DropDownList<KBHandle> field = new DropDownList<>("value", LambdaModel.of(() -> {
            AnnotationFeature feat = getModelObject().feature;
            List<KBHandle> handles = new LinkedList<>();
            for (KnowledgeBase kb : kbService.getKnowledgeBases(feat.getProject())) {
                handles.addAll(kbService.listConcepts(kb, true));
            }
            return new ArrayList<>(handles);
        }), new ChoiceRenderer<>("uiLabel"));
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        return field;
    }

    private String getSelectionSlotLabel()
    {
        if (roleModel.targetAddr == -1
            && stateModel.getObject().isArmedSlot(getModelObject().feature, 0)) {
            return "<Select to fill>";
        }
        else {
            return roleModel.label;
        }
    }


    private void actionToggleArmedState(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = SubjectObjectFeatureEditor.this.stateModel.getObject();

        if (state.isArmedSlot(getModelObject().feature, 0)) {
            state.clearArmedSlot();
            aTarget.add(content);
        }
        else {
            state.setArmedSlot(getModelObject().feature, 0);
            aTarget.add(content);
        }
    }

    @Override
    public Component getFocusComponent() {
        return focusComponent;
    }

    @Override
    public void onConfigure() {
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) this.getModelObject().value;
        if (links.size() == 0) {
            String role = roleModel.role;
            roleModel = new LinkWithRoleModel();
            roleModel.role = role;
            links.add(roleModel);
            this.stateModel.getObject().setArmedSlot(SubjectObjectFeatureEditor.this
                .getModelObject().feature, 0);
        } else {
            roleModel = links.get(0);
        }
    }
}
