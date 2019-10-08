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

import static de.tudarmstadt.ukp.inception.ui.kb.feature.FactLinkingConstants.FACT_LAYER;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.Collections;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.renderer.TextRenderer;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class PropertyFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = -4649541419448384970L;
    private static final Logger LOG = LoggerFactory.getLogger(PropertyFeatureEditor.class);
    private Component focusComponent3;
    private IModel<AnnotatorState> stateModel3;
    private AnnotationActionHandler actionHandler3;
    private Project project3;
    private ConceptFeatureTraits traits3;
    private boolean existStatements = false;

    private @SpringBean AnnotationSchemaService annotationService3;
    private @SpringBean KnowledgeBaseService kbService3;
    private @SpringBean FactLinkingService factService3;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry3;

    public PropertyFeatureEditor(String aId, MarkupContainer aOwner,
        AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
        IModel<FeatureState> aFeatureStateModel)
    {
        super(aId, aOwner, new CompoundPropertyModel<>(aFeatureStateModel));
        stateModel3 = aStateModel;
        actionHandler3 = aHandler;
        project3 = this.getModelObject().feature.getProject();
        traits3 = factService3.getFeatureTraits(project3);
        add(focusComponent3 = createAutoCompleteTextField());
        add(createStatementIndicatorLabel());
        add(createNoStatementLabel());
        add(new DisabledKBWarning("disabledKBWarning", Model.of(getModelObject().feature),
            Model.of(traits3)));
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }

    private AutoCompleteTextField<KBProperty> createAutoCompleteTextField()
    {
        AutoCompleteTextField<KBProperty> field = new AutoCompleteTextField<KBProperty>("value",
            new TextRenderer<KBProperty>("uiLabel"))
        {

            private static final long serialVersionUID = 2499259496065983734L;

            @Override protected List<KBProperty> getChoices(String input)
            {
                String repoId = traits3.getRepositoryId();
                if (!(repoId == null || kbService3.isKnowledgeBaseEnabled(project3, repoId))) {
                    return Collections.emptyList();
                }
                return factService3.listProperties(project3, traits3);
            }

            @Override
            public void onConfigure(JQueryBehavior behavior)
            {
                super.onConfigure(behavior);

                behavior.setOption("autoWidth", true);
            }

            @Override
            protected IJQueryTemplate newTemplate()
            {
                return KendoChoiceDescriptionScriptReference.template();
            }
        };

        return field;
    }

    private Label createStatementIndicatorLabel()
    {
        Label statementExists = new Label("statementExists",
            "There is at least one statement " + "in the KB which matches for this SPO.");
        statementExists
            .add(LambdaBehavior.onConfigure(component -> component.setVisible(existStatements)));
        return statementExists;
    }

    private Label createNoStatementLabel()
    {
        Label statementDoesNotExist = new Label("statementDoesNotExist",
            "There is no statement " + "in the KB which matches this SPO.");
        statementDoesNotExist
            .add(LambdaBehavior.onConfigure(component -> component.setVisible(!existStatements)));
        return statementDoesNotExist;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
    }

    @Override
    public Component getFocusComponent()
    {
        return focusComponent3;
    }

    @Override
    public void onConfigure()
    {
        super.onConfigure();
        
        KBHandle subject = getHandle(FactLinkingConstants.SUBJECT_ROLE);
        KBHandle object = getHandle(FactLinkingConstants.OBJECT_ROLE);
        KBProperty predicate = (KBProperty) getModelObject().value;
        if (subject == null || object == null || predicate == null) {
            existStatements = false;
        }
        else {
            KBStatement mockStatement = new KBStatement(subject, predicate);
            mockStatement.setValue(object.getUiLabel());
            KnowledgeBase kb = factService3.findKnowledgeBaseContainingProperty(predicate, project3,
                    traits3);
            existStatements = kbService3.exists(kb, mockStatement);
        }
    }

    private KBHandle getHandle(String name)
    {
        return getLinkedSubjectObjectKBHandle(name, actionHandler3, stateModel3.getObject());
    }

    public KBHandle getLinkedSubjectObjectKBHandle(String featureName,
        AnnotationActionHandler actionHandler3, AnnotatorState aState)
    {
        AnnotationLayer factLayer = annotationService3.findLayer(aState.getProject(), FACT_LAYER);
        KBHandle kbHandle = null;
        AnnotationFeature annotationFeature = annotationService3.getFeature(featureName, factLayer);
        List<LinkWithRoleModel> featureValue = (List<LinkWithRoleModel>) aState
            .getFeatureState(annotationFeature).value;
        if (!featureValue.isEmpty()) {
            int targetAddress = featureValue.get(0).targetAddr;
            if (targetAddress != -1) {
                CAS cas;
                try {
                    cas = actionHandler3.getEditorCas();
                    kbHandle = factService3
                        .getKBHandleFromCasByAddr(cas, targetAddress, aState.getProject(), traits3);
                }
                catch (Exception e) {
                    LOG.error("Error: " + e.getMessage(), e);
                    error("Error: " + e.getMessage());
                }

            }
        }
        return kbHandle;
    }
}

