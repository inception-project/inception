/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;

public class PropertyFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = -4649541419448384970L;
    private static final Logger logger = LoggerFactory.getLogger(PropertyFeatureEditor.class);
    private Component focusComponent;
    private IModel<AnnotatorState> stateModel;
    private AnnotationActionHandler actionHandler;
    private Project project;
    private KBStatement statement;
    private @SpringBean AnnotationSchemaService annotationService;

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean FactLinkingService factService;

    public PropertyFeatureEditor(String aId, MarkupContainer aOwner,
        AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
        IModel<FeatureState> aFeatureStateModel)
    {
        super(aId, aOwner, new CompoundPropertyModel<>(aFeatureStateModel));
        stateModel = aStateModel;
        actionHandler = aHandler;
        project = this.getModelObject().feature.getProject();
        add(new Label("feature", getModelObject().feature.getUiName()));
        add(focusComponent = createFieldComboBox());
    }

    private DropDownList<KBHandle> createFieldComboBox()
    {
        DropDownList<KBHandle> field = new DropDownList<KBHandle>("value",
            LambdaModelAdapter.of(this::getSelectedKBItem, this::setStatementInKB),
            LambdaModel.of(() -> factService.getAllPredicatesFromKB(project)), new ChoiceRenderer<>
            ("uiLabel"));
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        return field;
    }

    private KBHandle getSelectedKBItem()
    {
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

    private void setStatementInKB(KBHandle predicate)
    {
        getModelObject().value = predicate;
        if (stateModel.getObject().getFeatureStates().size() <= 1) {
            return;
        }

        KBHandle subject = getHandle("Subject");
        KBHandle object = getHandle("Object");
        // No subject or object set, so do not update the statement
        if (subject == null || object == null) {
            return;
        }

        if (!factService.checkSameKnowledgeBase(subject, predicate, project)) {
            logger.error("Subject and predicate are from different knowledge bases.");
            return;
        }

        String value = object.getUiLabel();
        statement = factService.updateStatement(subject, predicate, value, statement, project);
    }

    private KBHandle getHandle(String name) {
        return factService.getLinkedSubjectObjectKBHandle(name, actionHandler, stateModel.getObject());
    }
}

