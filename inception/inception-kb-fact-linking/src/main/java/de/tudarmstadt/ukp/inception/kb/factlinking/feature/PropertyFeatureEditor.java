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
package de.tudarmstadt.ukp.inception.kb.factlinking.feature;

import static de.tudarmstadt.ukp.inception.kb.factlinking.feature.FactLinkingConstants.FACT_LAYER;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.Collections;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.renderer.TextRenderer;
import org.wicketstuff.jquery.core.template.IJQueryTemplate;
import org.wicketstuff.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.annotation.feature.string.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.ui.kb.feature.DisabledKBWarning;

@Deprecated
public class PropertyFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = -4649541419448384970L;
    private static final Logger LOG = LoggerFactory.getLogger(PropertyFeatureEditor.class);
    private FormComponent<?> focusComponent;
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
        add(focusComponent = createAutoCompleteTextField());
        add(createStatementIndicatorLabel());
        add(createNoStatementLabel());
        add(new DisabledKBWarning("disabledKBWarning", Model.of(getModelObject().feature),
                Model.of(traits.getRepositoryId())));
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

            @Override
            protected List<KBProperty> getChoices(String input)
            {
                String repoId = traits.getRepositoryId();
                if (!(repoId == null || kbService.isKnowledgeBaseEnabled(project, repoId))) {
                    return Collections.emptyList();
                }
                return factService.listProperties(project, traits);
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
        statementExists.add(
                LambdaBehavior.onConfigure(component -> component.setVisible(existStatements)));
        return statementExists;
    }

    private Label createNoStatementLabel()
    {
        Label statementDoesNotExist = new Label("statementDoesNotExist",
                "There is no statement " + "in the KB which matches this SPO.");
        statementDoesNotExist.add(
                LambdaBehavior.onConfigure(component -> component.setVisible(!existStatements)));
        return statementDoesNotExist;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
    }

    @Override
    public FormComponent<?> getFocusComponent()
    {
        return focusComponent;
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
            KnowledgeBase kb = factService.findKnowledgeBaseContainingProperty(predicate, project,
                    traits);
            existStatements = kbService.exists(kb, mockStatement);
        }
    }

    private KBHandle getHandle(String name)
    {
        return getLinkedSubjectObjectKBHandle(name, actionHandler, stateModel.getObject());
    }

    public KBHandle getLinkedSubjectObjectKBHandle(String featureName,
            AnnotationActionHandler aActionHandler, AnnotatorState aState)
    {
        AnnotationLayer factLayer = annotationService.findLayer(aState.getProject(), FACT_LAYER);
        KBHandle kbHandle = null;
        AnnotationFeature annotationFeature = annotationService.getFeature(featureName, factLayer);
        @SuppressWarnings("unchecked")
        var featureValue = (List<LinkWithRoleModel>) aState
                .getFeatureState(annotationFeature).value;
        if (!featureValue.isEmpty()) {
            int targetAddress = featureValue.get(0).targetAddr;
            if (targetAddress != -1) {
                CAS cas;
                try {
                    cas = aActionHandler.getEditorCas();
                    kbHandle = factService.getKBHandleFromCasByAddr(cas, targetAddress,
                            aState.getProject(), traits);
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
