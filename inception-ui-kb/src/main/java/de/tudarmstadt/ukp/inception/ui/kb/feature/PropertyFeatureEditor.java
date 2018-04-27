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

import java.util.Arrays;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.renderer.TextRenderer;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
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
    private Boolean existStatements = false;

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
        add(focusComponent = createAutoCompleteTextField());
        add(createStatementIndicatorLabel());
        add(createNoStatementLabel());
    }

    private AutoCompleteTextField<KBHandle> createAutoCompleteTextField()
    {
        AutoCompleteTextField<KBHandle> field = new AutoCompleteTextField<KBHandle>("value",
            new TextRenderer<KBHandle>("uiLabel"))
        {

            private static final long serialVersionUID = 2499259496065983734L;

            @Override protected List<KBHandle> getChoices(String input)
            {
                return factService.getAllPredicatesFromKB(project);
            }

            @Override public void onConfigure(JQueryBehavior behavior)
            {
                super.onConfigure(behavior);
                behavior.setOption("autoWidth", true);
            }

            @Override protected IJQueryTemplate newTemplate()
            {
                return new IJQueryTemplate()
                {
                    private static final long serialVersionUID = 1L;

                    @Override public String getText()
                    {
                        // Some docs on how the templates work in Kendo, in case we need
                        // more fancy dropdowns
                        // http://docs.telerik.com/kendo-ui/framework/templates/overview
                        return "# if (data.reordered == 'true') { #"
                            + "<div title=\"#: data.description #\" "
                            + "onmouseover=\"javascript:applyTooltip(this)\">"
                            + "<b>#: data.name #</b></div>\n" + "# } else { #"
                            + "<div title=\"#: data.description #\" "
                            + "onmouseover=\"javascript:applyTooltip(this)\">"
                            + "#: data.name #</div>\n" + "# } #";
                    }

                    @Override public List<String> getTextProperties()
                    {
                        return Arrays.asList("name", "description");
                    }
                };
            }
        };

        // Ensure that markup IDs of feature editor focus components remain constant across
        // refreshes of the feature editor panel. This is required to restore the focus.
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
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
        KBHandle subject = getHandle(FactLinkingConstants.SUBJECT_ROLE);
        KBHandle object = getHandle(FactLinkingConstants.OBJECT_ROLE);
        KBHandle predicate = (KBHandle) getModelObject().value;
        if (subject == null || object == null || predicate == null) {
            existStatements = false;
        }
        else {
            KBStatement mockStatement = new KBStatement(subject, predicate);
            mockStatement.setValue(object.getUiLabel());
            KnowledgeBase kb = factService.getKBByKBHandle(predicate, project);
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
                JCas jCas = null;
                try {
                    jCas = actionHandler.getEditorCas();
                }
                catch (Exception e) {
                    LOG.error("Error: " + e.getMessage(), e);
                }
                kbHandle = factService
                    .getKBHandleFromCasByAddr(jCas, targetAddress, aState.getProject());
            }
        }
        return kbHandle;
    }
}

