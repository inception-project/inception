/*
 * Copyright 2018
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class SubjectObjectFeatureEditor extends FeatureEditor {

    private static final long serialVersionUID = 4230722501745589589L;
    private static final Logger logger = LoggerFactory.getLogger(SubjectObjectFeatureEditor.class);
    private @SpringBean AnnotationSchemaService annotationService;
    private WebMarkupContainer content;

    @SuppressWarnings("rawtypes")
    private Component focusComponent;
    private boolean hideUnconstraintFeature;

    private AnnotationActionHandler actionHandler;
    private IModel<AnnotatorState> stateModel;

    @SuppressWarnings("unused")
    private LinkWithRoleModel roleModel;

    AnnotationFeature linkedAnnotationFeature;

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

        roleModel = new LinkWithRoleModel();
        roleModel.role = role;
        if (links.size() == 1) {
            roleModel = links.get(0);
        }

        content.add(new Label("role", roleModel.role));
        content.add(createSubjectObjectLabel());
        content.add(focusComponent = createFieldComboBox());
    }

    private Label createSubjectObjectLabel() {
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

    private DropDownList<KBHandle> createFieldComboBox() {
        DropDownList<KBHandle> field = new DropDownList<KBHandle>("value",
            LambdaModelAdapter.of(
                this::getSelectedKBItem,
                this::setSelectedKBItem
            ),
            LambdaModel.of(this::getKBConceptsAndInstances), new ChoiceRenderer<>("uiLabel"));
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
    protected void onInitialize()
    {
        super.onInitialize();
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
        String linkedType = this.getModelObject().feature.getType();
        AnnotationLayer linkedLayer = annotationService.getLayer(linkedType, this.stateModel
            .getObject().getProject());
        linkedAnnotationFeature = annotationService.getFeature("KBItems",
            linkedLayer);
    }

    private void setSelectedKBItem(KBHandle value) {
        if (roleModel.targetAddr != -1) {
            try {
                JCas jCas = actionHandler.getEditorCas().getCas().getJCas();
                AnnotationFS selectedFS = WebAnnoCasUtil.selectByAddr(jCas, roleModel.targetAddr);
                WebAnnoCasUtil.setFeature(selectedFS, linkedAnnotationFeature,
                    value.getIdentifier());
            } catch (CASException | IOException e) {
                logger.error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
            }
            if (roleModel.role.equals("subject")) {
                setStatementInKB(value);

            }
        }
    }

    private KBHandle getSelectedKBItem() {
        KBHandle selectedKBHandleItem = null;
        if (roleModel.targetAddr != -1) {
            try {
                JCas jCas = actionHandler.getEditorCas().getCas().getJCas();
                AnnotationFS selectedFS = WebAnnoCasUtil.selectByAddr(jCas, roleModel.targetAddr);
                String selectedKBItemIdentifier = WebAnnoCasUtil.getFeature(selectedFS,
                    linkedAnnotationFeature.getName());
                if (selectedKBItemIdentifier != null) {
                    List<KBHandle> handles = getKBConceptsAndInstances();
                    selectedKBHandleItem = handles.stream().filter(x -> selectedKBItemIdentifier
                        .equals(x.getIdentifier())).findAny().orElse(null);
                }
            } catch (CASException | IOException e) {
                logger.error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
            }
        }
        return selectedKBHandleItem;
    }

    private List<KBHandle> getKBConceptsAndInstances() {
        AnnotationFeature feat = getModelObject().feature;
        List<KBHandle> handles = new LinkedList<>();
        for (KnowledgeBase kb : kbService.getKnowledgeBases(feat.getProject())) {
            handles.addAll(kbService.listConcepts(kb, false));
            for (KBHandle concept: kbService.listConcepts(kb, false)) {
                handles.addAll(kbService.listInstances(kb, concept.getIdentifier(),
                    false));
            }
        }
        return new ArrayList<>(handles);
    }

    private KnowledgeBase getKBForKBHandle(KBHandle kbHandle) {
        AnnotationFeature feat = getModelObject().feature;
        for (KnowledgeBase kb: kbService.getKnowledgeBases(feat.getProject())) {
            if (kbService.listProperties(kb, false).contains(kbHandle)) {
                return kb;
            }
            if (kbService.listConcepts(kb, false).contains(kbHandle)) {
                return kb;
            }
            for (KBHandle concept: kbService.listConcepts(kb, false)) {
                if (kbService.listInstances(kb, concept.getIdentifier(), false).contains
                    (kbHandle)) {
                    return kb;
                }
            }

        }
        return null;
    }

    private void setStatementInKB(KBHandle value) {
        AnnotationLayer factLayer = annotationService.getLayer(
            "webanno.custom.Fact", this.stateModel.getObject().getProject());
        AnnotationFeature predicateFeature = annotationService.getFeature(
            "KBPredicate", factLayer);
        KBHandle predicateHandle = (KBHandle) this.stateModel.getObject().getFeatureState
            (predicateFeature).value;
        if (predicateHandle != null) {
            KBStatement statement = new KBStatement();
            statement.setInstance(value);
            statement.setProperty(predicateHandle);
            AnnotationFeature objectFeature = annotationService.getFeature("Object",
                factLayer);
            List<LinkWithRoleModel> objectList = (List<LinkWithRoleModel>) this.stateModel.
                getObject().getFeatureState(objectFeature).value;
//            String objectLabel = objectList.get(0).label;
//            String object = objectLabel.equals("<Click to activate>") || objectLabel.equals
//                ("<Select to fill>") ? " " : objectLabel;
            int targetAddress = objectList.get(0).targetAddr;
            String object = "";
            if (targetAddress != -1) {
                try {
                    JCas jCas = actionHandler.getEditorCas().getCas().getJCas();
                    AnnotationFS selectedFS = WebAnnoCasUtil.selectByAddr(jCas, targetAddress);
                    String selectedKBItemIdentifier = WebAnnoCasUtil.getFeature(selectedFS,
                        linkedAnnotationFeature.getName());
                    if (selectedKBItemIdentifier != null) {
                        List<KBHandle> handles = getKBConceptsAndInstances();
                        object = handles.stream().filter(x -> selectedKBItemIdentifier
                            .equals(x.getIdentifier())).findAny().orElse(null).getUiLabel();
                    }
                } catch (CASException | IOException e) {
                    logger.error("Error: " + e.getMessage(), e);
                    error("Error: " + e.getMessage());
                }
            }
            statement.setValue(object);
            KnowledgeBase subjectKB = getKBForKBHandle(value);
            KnowledgeBase predicateKB = getKBForKBHandle(predicateHandle);
            if (subjectKB.equals(predicateKB)) {
                kbService.upsertStatement(subjectKB, statement);
            } else {
                logger.error("Subject and predicate are from different knowledge bases.");
            }
        }
    }

}
