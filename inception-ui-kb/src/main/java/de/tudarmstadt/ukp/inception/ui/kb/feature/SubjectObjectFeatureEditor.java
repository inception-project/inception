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

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.core.renderer.TextRenderer;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;
import com.googlecode.wicket.kendo.ui.widget.tooltip.TooltipBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingServiceImpl;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBErrorHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;

public class SubjectObjectFeatureEditor
    extends FeatureEditor
{

    private static final long serialVersionUID = 4230722501745589589L;
    private static final Logger LOG = LoggerFactory.getLogger(SubjectObjectFeatureEditor.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ConceptLinkingServiceImpl clService;
    private @SpringBean FactLinkingService factService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean KnowledgeBaseService kbService;

    private WebMarkupContainer content;
    private Component focusComponent;
    private AnnotationActionHandler actionHandler;
    private IModel<AnnotatorState> stateModel;
    private Project project;
    private LinkWithRoleModel roleModel;
    private AnnotationFeature linkedAnnotationFeature;

    public SubjectObjectFeatureEditor(String aId, MarkupContainer aOwner,
        AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
        final IModel<FeatureState> aFeatureStateModel, String role)
    {
        super(aId, aOwner, CompoundPropertyModel.of(aFeatureStateModel));

        stateModel = aStateModel;
        actionHandler = aHandler;
        project = this.getModelObject().feature.getProject();

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

        content.add(createSubjectObjectLabel());
        content.add(createRemoveLabelIcon());
        content.add(focusComponent = createAutoCompleteTextField());
        content.add(createDisabledKbWarningLabel());
    }
    
    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
        
        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }

    private Label createSubjectObjectLabel()
    {
        Label label;
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

            @Override public String getObject()
            {
                if (roleLabelSlotIsSelected()) {
                    return "; background: orange";
                }
                else {
                    return "";
                }
            }
        }));
        if (!roleLabelSlotIsSelected()) {
            label.setDefaultModelObject(roleModel.label);
        }
        return label;
    }

    private LambdaAjaxLink createRemoveLabelIcon()
    {
        return new LambdaAjaxLink("removeLabel", this::removeSelectedLabel);
    }

    private void removeSelectedLabel(AjaxRequestTarget aTarget)
    {
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) this.getModelObject().value;
        AnnotatorState state = this.stateModel.getObject();

        String role = roleModel.role;
        roleModel = new LinkWithRoleModel();
        roleModel.role = role;
        links.set(0, roleModel);

        // Auto-commit if working on existing annotation
        if (state.getSelection().getAnnotation().isSet()) {
            try {
                actionHandler.actionCreateOrUpdate(aTarget, actionHandler.getEditorCas());
            }
            catch (Exception e) {
                handleException(this, aTarget, e);
            }
        }
    }

    private String getSelectionSlotLabel()
    {
        if (!roleLabelIsFilled() && roleLabelSlotIsSelected()) {
            return "<Select to fill>";
        }
        else {
            return roleModel.label;
        }
    }

    private boolean roleLabelIsFilled()
    {

        return roleModel.targetAddr != -1;
    }

    private boolean roleLabelSlotIsSelected()
    {
        return stateModel.getObject().isArmedSlot(getModelObject().feature, 0);
    }

    private void actionToggleArmedState(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = stateModel.getObject();

        if (roleLabelSlotIsSelected()) {
            state.clearArmedSlot();
            aTarget.add(getOwner());
        }
        else {
            state.setArmedSlot(getModelObject().feature, 0);
            aTarget.add(getOwner());
        }
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
        
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) this.getModelObject().value;
        if (links.size() == 0) {
            String role = roleModel.role;
            roleModel = new LinkWithRoleModel();
            roleModel.role = role;
            links.add(roleModel);
        }
        else {
            roleModel = links.get(0);
        }
        linkedAnnotationFeature = getLinkedAnnotationFeature();
    }

    private AutoCompleteTextField<KBHandle> createAutoCompleteTextField()
    {
        AutoCompleteTextField<KBHandle> field = new AutoCompleteTextField<KBHandle>("value",
            LambdaModelAdapter.of(this::getSelectedKBItem, this::setSelectedKBItem),
            new TextRenderer<KBHandle>("uiLabel"), KBHandle.class)
        {

            private static final long serialVersionUID = 5683897252648514996L;

            @Override protected List<KBHandle> getChoices(String input)
            {
                return listInstances(actionHandler, input);
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

    private void setSelectedKBItem(KBHandle value)
    {
        // We do not want to store the error handle.
        if (value instanceof KBErrorHandle) {
            return;
        }
        
        if (roleLabelIsFilled()) {
            try {
                JCas jCas = actionHandler.getEditorCas();
                AnnotationFS selectedFS = WebAnnoCasUtil.selectByAddr(jCas, roleModel.targetAddr);
                WebAnnoCasUtil.setFeature(selectedFS, linkedAnnotationFeature,
                    value != null ? value.getIdentifier() : value);
            }
            catch (Exception e) {
                error("Error: " + e.getMessage());
                LOG.error("Error: " + e.getMessage(), e);
            }
        }
    }

    private KBHandle getSelectedKBItem()
    {
        KBHandle selectedKBHandleItem = null;
        if (roleLabelIsFilled()) {
            String selectedKBItemIdentifier;
            
            try {
                JCas jCas = actionHandler.getEditorCas();
                AnnotationFS selectedFS = WebAnnoCasUtil.selectByAddr(jCas, roleModel.targetAddr);
                selectedKBItemIdentifier = WebAnnoCasUtil.getFeature(selectedFS,
                        linkedAnnotationFeature.getName());
            }
            catch (Exception e) {
                LOG.error("Error loading CAS:  " + e.getMessage(), e);
                // We cannot use feedback messages in code that is called from the load() method
                // of a LoadableDetachableModel, so this is an alternative way of passing the
                // error on to the user.
                return new KBErrorHandle("Error loading CAS: " + e.getMessage(), e);
            }
            
            if (selectedKBItemIdentifier != null) {
                try {
                    ConceptFeatureTraits traits = factService.getFeatureTraits(project);
                    selectedKBHandleItem = factService.getKBInstancesByIdentifierAndTraits(
                            selectedKBItemIdentifier, project, traits);
                }
                catch (Exception e) {
                    LOG.error("Unable to resolve [" + selectedKBItemIdentifier + "]: "
                            + e.getMessage(), e);
                    // We cannot use feedback messages in code that is called from the load() method
                    // of a LoadableDetachableModel, so this is an alternative way of passing the
                    // error on to the user.
                    return new KBErrorHandle("Unable to resolve [" + selectedKBItemIdentifier + "]",
                            e);
                }
            }
        }
        return selectedKBHandleItem;
    }

    private List<KBHandle> listInstances(AnnotationActionHandler aHandler, String aTypedString)
    {
        if (linkedAnnotationFeature == null) {
            linkedAnnotationFeature = getLinkedAnnotationFeature();
        }
        List<KBHandle> handles = new ArrayList<>();
        FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry
            .getFeatureSupport(linkedAnnotationFeature);
        ConceptFeatureTraits traits = fs.readTraits(linkedAnnotationFeature);

        // Check if kb is actually enabled
        if (featureUsesDisabledKB(traits)) {
            return Collections.emptyList();
        }

        // Use concept linking if enabled
        try {
            handles = clService.getLinkingInstancesInKBScope(traits.getRepositoryId(),
                    traits.getScope(), traits.getAllowedValueType(), aTypedString, roleModel.label,
                    roleModel.targetAddr, getEditorCas(aHandler), project);
        }
        catch (IOException e) {
            LOG.error("An error occurred while retrieving entity candidates.", e);
            error("An error occurred while retrieving entity candidates: " + e.getMessage());
            RequestCycle.get().find(IPartialPageRequestHandler.class)
                .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
        }

        // if concept linking does not return any results or is disabled
        if (handles.size() == 0) {
            handles = kbService.getEntitiesInScope(traits.getRepositoryId(), traits.getScope(),
                traits.getAllowedValueType(), project);
            // Sort and filter results
            handles = handles.stream()
                .filter(handle -> handle.getUiLabel().toLowerCase().startsWith(aTypedString))
                .sorted(Comparator.comparing(KBObject::getUiLabel)).collect(Collectors.toList());
        }
        return KBHandle.distinctByIri(handles);
    }

    private AnnotationFeature getLinkedAnnotationFeature() {
        String linkedType = this.getModelObject().feature.getType();
        AnnotationLayer linkedLayer = annotationService
            .getLayer(linkedType, this.stateModel.getObject().getProject());
        AnnotationFeature linkedAnnotationFeature = annotationService
            .getFeature(FactLinkingConstants.LINKED_LAYER_FEATURE, linkedLayer);
        return linkedAnnotationFeature;
    }

    private ConceptFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature) {
        FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry
            .getFeatureSupport(aAnnotationFeature);
        ConceptFeatureTraits traits = fs.readTraits(aAnnotationFeature);
        return traits;
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

    private JCas getEditorCas(AnnotationActionHandler aHandler) throws IOException
    {
        return aHandler.getEditorCas();
    }

    public static void handleException(Component aComponent, AjaxRequestTarget aTarget,
        Exception aException)
    {
        try {
            throw aException;
        }
        catch (AnnotationException e) {
            if (aTarget != null) {
                aTarget.prependJavaScript("alert('Error: " + e.getMessage() + "')");
            }
            else {
                aComponent.error("Error: " + e.getMessage());
            }
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (UIMAException e) {
            aComponent.error("Error: " + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (Exception e) {
            aComponent.error("Error: " + e.getMessage());
            LOG.error("Error: " + e.getMessage(), e);
        }
    }

    private Label createDisabledKbWarningLabel()
    {
        Label warningLabel = new Label("disabledKBWarning", Model.of());
        if (linkedAnnotationFeature == null) {
            linkedAnnotationFeature = getLinkedAnnotationFeature();
        }
        ConceptFeatureTraits traits = readFeatureTraits(linkedAnnotationFeature);
        warningLabel.add(
            LambdaBehavior.onConfigure(label -> label.setVisible(featureUsesDisabledKB(traits))));

        TooltipBehavior tip = new TooltipBehavior();

        Optional<KnowledgeBase> kb = Optional.empty();
        if (traits != null && traits.getRepositoryId() != null) {
            kb = kbService.getKnowledgeBaseById(linkedAnnotationFeature.getProject(),
                traits.getRepositoryId());
        }
        String kbName = kb.isPresent() ? kb.get().getName() : "unknown ID";

        tip.setOption("content", Options.asString(
            new StringResourceModel("value.null.disabledKbWarning", this).setParameters(kbName)
                .getString()));
        tip.setOption("width", Options.asString("300px"));
        warningLabel.add(tip);

        return warningLabel;
    }
}
