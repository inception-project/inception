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

import java.util.List;
import java.util.Optional;

import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.Options;
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
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class PropertyFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = -4649541419448384970L;
    private static final Logger LOG = LoggerFactory.getLogger(PropertyFeatureEditor.class);
    private Component focusComponent;
    private IModel<AnnotatorState> stateModel;
    private AnnotationActionHandler actionHandler;
    private Project project;
    private ConceptFeatureTraits traits;
    private boolean existStatements = false;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean FactLinkingService factService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    public PropertyFeatureEditor(String aId, MarkupContainer aOwner,
        AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
        IModel<FeatureState> aFeatureStateModel)
    {
        super(aId, aOwner, new CompoundPropertyModel<>(aFeatureStateModel));
        stateModel = aStateModel;
        actionHandler = aHandler;
        project = this.getModelObject().feature.getProject();
        traits = factService.getFeatureTraits(project);
        add(new Label("feature", getModelObject().feature.getUiName()));
        add(focusComponent = createAutoCompleteTextField());
        add(createStatementIndicatorLabel());
        add(createNoStatementLabel());
        add(createDisabledKbWarningLabel());
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }

    private AutoCompleteTextField<KBHandle> createAutoCompleteTextField()
    {
        AutoCompleteTextField<KBHandle> field = new AutoCompleteTextField<KBHandle>("value",
            new TextRenderer<KBHandle>("uiLabel"))
        {

            private static final long serialVersionUID = 2499259496065983734L;

            @Override protected List<KBHandle> getChoices(String input)
            {
                return factService.getPredicatesFromKB(project, traits);
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
        return focusComponent;
    }

    @Override
    public void onConfigure()
    {
        super.onConfigure();
        
        KBHandle subject = getHandle(FactLinkingConstants.SUBJECT_ROLE);
        KBHandle object = getHandle(FactLinkingConstants.OBJECT_ROLE);
        KBHandle predicate = (KBHandle) getModelObject().value;
        if (subject == null || object == null || predicate == null) {
            existStatements = false;
        }
        else {
            KBStatement mockStatement = new KBStatement(subject, predicate);
            mockStatement.setValue(object.getUiLabel());
            KnowledgeBase kb = factService.getKBByKBHandleAndTraits(predicate, project, traits);
            existStatements = kbService.statementsMatchSPO(kb, mockStatement);
        }
    }

    private KBHandle getHandle(String name)
    {
        return getLinkedSubjectObjectKBHandle(name, actionHandler, stateModel.getObject());
    }

    public KBHandle getLinkedSubjectObjectKBHandle(String featureName,
        AnnotationActionHandler actionHandler, AnnotatorState aState)
    {
        AnnotationLayer factLayer = annotationService.getLayer(FACT_LAYER, aState.getProject());
        KBHandle kbHandle = null;
        AnnotationFeature annotationFeature = annotationService.getFeature(featureName, factLayer);
        List<LinkWithRoleModel> featureValue = (List<LinkWithRoleModel>) aState
            .getFeatureState(annotationFeature).value;
        if (!featureValue.isEmpty()) {
            int targetAddress = featureValue.get(0).targetAddr;
            if (targetAddress != -1) {
                JCas jCas;
                try {
                    jCas = actionHandler.getEditorCas();
                    kbHandle = factService
                        .getKBHandleFromCasByAddr(jCas, targetAddress, aState.getProject(), traits);
                }
                catch (Exception e) {
                    LOG.error("Error: " + e.getMessage(), e);
                    error("Error: " + e.getMessage());
                }

            }
        }
        return kbHandle;
    }

    private Label createDisabledKbWarningLabel()
    {
        Label warningLabel = new Label("disabledKBWarning", Model.of());
        AnnotationFeature feature = getModelObject().feature;
        warningLabel.add(LambdaBehavior
            .onConfigure(label -> label.setVisible(featureUsesDisabledKB(traits))));

        TooltipBehavior tip = new TooltipBehavior();

        Optional<KnowledgeBase> kb = Optional.empty();
        if (traits.getRepositoryId() != null) {
            kb = kbService.getKnowledgeBaseById(feature.getProject(), traits.getRepositoryId());
        }
        String kbName = kb.isPresent() ? kb.get().getName() : "unknown ID";

        tip.setOption("content", Options.asString(
            new StringResourceModel("value.null.disabledKbWarning", this)
                .setParameters(kbName).getString()));
        tip.setOption("width", Options.asString("300px"));
        warningLabel.add(tip);

        return warningLabel;
    }

    private boolean featureUsesDisabledKB(ConceptFeatureTraits aTraits)
    {
        Optional<KnowledgeBase> kb = Optional.empty();
        String repositoryId = aTraits.getRepositoryId();
        if (repositoryId != null) {
            kb = kbService.getKnowledgeBaseById(getModelObject().feature.getProject(),
                aTraits.getRepositoryId());
        }
        return kb.isPresent() && !kb.get().isEnabled() || repositoryId != null && !kb.isPresent();
    }
}

