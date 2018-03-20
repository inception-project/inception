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

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyFeatureEditor extends FeatureEditor {

    private static final long serialVersionUID = -4649541419448384970L;
    private static final Logger logger = LoggerFactory.getLogger(PropertyFeatureEditor.class);
    private Component focusComponent;
    private IModel<AnnotatorState> stateModel;
    private AnnotationActionHandler actionHandler;
    private @SpringBean AnnotationSchemaService annotationService;

    private @SpringBean KnowledgeBaseService kbService;

    public PropertyFeatureEditor(String aId, MarkupContainer aOwner,
                                 AnnotationActionHandler aHandler,
                                 final IModel<AnnotatorState> aStateModel,
                                 IModel<FeatureState> aFeatureStateModel)
    {
        super(aId, aOwner, new CompoundPropertyModel<>(aFeatureStateModel));
        stateModel = aStateModel;
        actionHandler = aHandler;
        add(new Label("feature", getModelObject().feature.getUiName()));
        add(focusComponent = createFieldComboBox());
    }

    private DropDownList<KBHandle> createFieldComboBox()
    {
        DropDownList<KBHandle> field = new DropDownList<KBHandle>("value",
            LambdaModelAdapter.of(
                this::getSelectedKBItem,
                this::setStatementInKB
            ),
            LambdaModel.of(() -> {
            AnnotationFeature feat = getModelObject().feature;
            List<KBHandle> handles = new LinkedList<>();
            for (KnowledgeBase kb : kbService.getKnowledgeBases(feat.getProject())) {
                handles.addAll(kbService.listProperties(kb, false));
            }
            return new ArrayList<>(handles);
        }), new ChoiceRenderer<>("uiLabel"));
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        return field;
    }

    private KBHandle getSelectedKBItem() {
        return (KBHandle) this.getModelObject().value;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
    }

    @Override
    public Component getFocusComponent()
    {
        return focusComponent;
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

    private void setStatementInKB(KBHandle predicateHandle) {
        this.getModelObject().value = predicateHandle;
        if (stateModel.getObject().getFeatureStates().size() > 1) {
            KBHandle subject = getSubjectOrObjectKBHandle("Subject");
            if (subject != null) {
                KBStatement statement = new KBStatement();
                statement.setInstance(subject);
                statement.setProperty(predicateHandle);
                KnowledgeBase subjectKB = getKBForKBHandle(subject);
                KnowledgeBase predicateKB = getKBForKBHandle(predicateHandle);
                if (subjectKB.equals(predicateKB)) {
                    KBHandle object = getSubjectOrObjectKBHandle("Object");
                    if (object != null) {
                        statement.setValue(object.getUiLabel());
                    } else {
                        statement.setValue("");
                    }
                    kbService.upsertStatement(subjectKB, statement);
                } else {
                    logger.error("Subject and predicate are from different knowledge bases.");
                }
            }
        }
    }

    private KBHandle getSubjectOrObjectKBHandle(String featureName) {
        KBHandle kbHandle = null;
        AnnotationLayer factLayer = annotationService.getLayer(
            "webanno.custom.Fact", this.stateModel.getObject().getProject());
        AnnotationFeature subjectFeature = annotationService.getFeature(featureName,
            factLayer);
        List<LinkWithRoleModel> subjectList = (List<LinkWithRoleModel>) this.stateModel.
            getObject().getFeatureState(subjectFeature).value;
        int targetAddress = subjectList.get(0).targetAddr;
        if (targetAddress != -1) {
            try {
                JCas jCas = actionHandler.getEditorCas().getCas().getJCas();
                AnnotationFS selectedFS = WebAnnoCasUtil.selectByAddr(jCas, targetAddress);
                String kbHandleIdentifier = WebAnnoCasUtil.getFeature(selectedFS,
                    "KBItems");
                if (kbHandleIdentifier != null) {
                    List<KBHandle> handles = getKBConceptsAndInstances();
                    kbHandle = handles.stream().filter(x -> kbHandleIdentifier
                        .equals(x.getIdentifier())).findAny().orElse(null);
                }
            } catch (CASException | IOException e) {
                logger.error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
            }
        }
        return kbHandle;
    }
}

