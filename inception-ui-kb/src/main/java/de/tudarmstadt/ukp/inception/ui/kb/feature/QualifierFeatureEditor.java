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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
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
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
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
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class QualifierFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 7469241620229001983L;
    private static final Logger LOG = LoggerFactory.getLogger(QualifierFeatureEditor.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ConceptLinkingService clService;
    private @SpringBean FactLinkingService factService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean KnowledgeBaseService kbService;

    private WebMarkupContainer content;
    private Component focusComponent;
    private AnnotationActionHandler actionHandler;
    private IModel<AnnotatorState> stateModel;
    private Project project;
    private LambdaModelAdapter<KBHandle> qualifierModel;
    private KBHandle selectedRole;

    public QualifierFeatureEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
            final IModel<FeatureState> aFeatureStateModel)
    {
        super(aId, aOwner, CompoundPropertyModel.of(aFeatureStateModel));

        stateModel = aStateModel;
        actionHandler = aHandler;
        project = stateModel.getObject().getProject();

        add(new Label("feature", getModelObject().feature.getUiName()));

        // Most of the content is inside this container such that we can refresh it independently
        // from the rest of the form
        content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        add(content);

        content.add(new RefreshingView<LinkWithRoleModel>("slots",
            PropertyModel.of(getModel(), "value"))
        {
            private static final long serialVersionUID = 5475284956525780698L;

            @Override
            protected Iterator<IModel<LinkWithRoleModel>> getItemModels()
            {
                return new ModelIteratorAdapter<LinkWithRoleModel>(
                    (List<LinkWithRoleModel>) QualifierFeatureEditor.this.getModelObject().value)
                {
                    @Override
                    protected IModel<LinkWithRoleModel> model(LinkWithRoleModel aObject)
                    {
                        return Model.of(aObject);
                    }
                };
            }

            @Override
            protected void populateItem(final Item<LinkWithRoleModel> aItem)
            {
                AnnotatorState state = stateModel.getObject();

                aItem.setModel(new CompoundPropertyModel<>(aItem.getModelObject()));
                Label role = new Label("role");
                aItem.add(role);

                final Label label;
                if (aItem.getModelObject().targetAddr == -1
                    && state.isArmedSlot(getModelObject().feature, aItem.getIndex())) {
                    label = new Label("label", "<Select to fill>");
                }
                else {
                    label = new Label("label");
                }
                label.add(new AjaxEventBehavior("click")
                {
                    private static final long serialVersionUID = 7633309278417475424L;

                    @Override
                    protected void onEvent(AjaxRequestTarget aTarget)
                    {
                        actionToggleArmedState(aTarget, aItem);
                    }
                });
                label.add(new AttributeAppender("style", new Model<String>()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject()
                    {
                        if (state.isArmedSlot(getModelObject().feature, aItem.getIndex())) {
                            return "; background: orange";
                        }
                        else {
                            return "";
                        }
                    }
                }));
                // Add a label to select mention of a qualifier
                aItem.add(label);
                // Add a text filed to link concept in KB with mention
                aItem.add(createMentionKBLinkTextField(aItem));
            }
        });

        // Add a text field to select property as a role
        content.add(focusComponent = createSelectPropertyAutoCompleteTextField());

        // Add a new empty slot with the specified role
        content.add(new AjaxButton("add")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();
                setVisible(!(state.isSlotArmed() && QualifierFeatureEditor.this.getModelObject()
                    .feature.equals(state.getArmedFeature())));
                // setEnabled(!(model.isSlotArmed()
                // && aModel.feature.equals(model.getArmedFeature())));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
            {
                actionAdd(aTarget);
            }
        });

        // Allows user to update slot
        content.add(new AjaxButton("set")
        {

            private static final long serialVersionUID = 7923695373085126646L;

            @Override
            protected void onConfigure()
            {
                AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();
                setVisible(state.isSlotArmed() && QualifierFeatureEditor.this.getModelObject()
                    .feature.equals(state.getArmedFeature()));
                // setEnabled(model.isSlotArmed()
                // && aModel.feature.equals(model.getArmedFeature()));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
            {
                actionSet(aTarget);
            }
        });

        // Add a new empty slot with the specified role
        content.add(new AjaxButton("del")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();
                setVisible(state.isSlotArmed() && QualifierFeatureEditor.this.getModelObject()
                    .feature.equals(state.getArmedFeature()));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
            {
                actionDel(aTarget);
            }
        });

        // Add warning that shows up if the knowledge base that is used by the concept feature
        // is disabled
        content.add(createDisabledKbWarningLabel());
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }

    private AutoCompleteTextField<KBHandle> createMentionKBLinkTextField(
        Item<LinkWithRoleModel> aItem)
    {
        AnnotationFeature linkedAnnotationFeature = getLinkedAnnotationFeature();

        qualifierModel = new LambdaModelAdapter<>(() -> this.getSelectedKBItem(aItem), (v) -> {
            this.setSelectedKBItem((KBHandle) v, aItem, linkedAnnotationFeature);
        });

        AutoCompleteTextField<KBHandle> field = new AutoCompleteTextField<KBHandle>("value",
            qualifierModel, new TextRenderer<KBHandle>("uiLabel"), KBHandle.class)
        {

            private static final long serialVersionUID = 5683897252648514996L;

            @Override
            protected List<KBHandle> getChoices(String input)
            {
                return listInstances(actionHandler, input, linkedAnnotationFeature,
                    aItem.getModelObject().label, aItem.getModelObject().targetAddr);
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

        // Ensure that markup IDs of feature editor focus components remain constant across
        // refreshes of the feature editor panel. This is required to restore the focus.
        field.setOutputMarkupId(true);
        return field;
    }

    private AnnotationFeature getLinkedAnnotationFeature() {
        String linkedType = this.getModelObject().feature.getType();
        AnnotationLayer linkedLayer = annotationService
            .getLayer(linkedType, this.stateModel.getObject().getProject());
        AnnotationFeature linkedAnnotationFeature = annotationService
            .getFeature(FactLinkingConstants.LINKED_LAYER_FEATURE, linkedLayer);
        return linkedAnnotationFeature;
    }

    private KBHandle getSelectedKBItem(Item<LinkWithRoleModel> aItem) {
        KBHandle selectedKBHandleItem = null;
        if (aItem.getModelObject().targetAddr != -1) {
            try {
                ConceptFeatureTraits traits = factService.getFeatureTraits(project);
                JCas jCas = actionHandler.getEditorCas();
                int targetAddr = aItem.getModelObject().targetAddr;
                selectedKBHandleItem = factService.getKBHandleFromCasByAddr(jCas, targetAddr,
                    project, traits);
            } catch (Exception e) {
                LOG.error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
            }
        }
        return selectedKBHandleItem;
    }

    private void setSelectedKBItem(KBHandle value, Item<LinkWithRoleModel> aItem,
        AnnotationFeature linkedAnnotationFeature)
    {
        if (aItem.getModelObject().targetAddr != -1) {
            try {
                JCas jCas = actionHandler.getEditorCas();
                AnnotationFS selectedFS = WebAnnoCasUtil
                    .selectByAddr(jCas, aItem.getModelObject().targetAddr);
                WebAnnoCasUtil.setFeature(selectedFS, linkedAnnotationFeature,
                    value != null ? value.getIdentifier() : value);
                LOG.info("change the value");
                qualifierModel.detach();

                // Save the CAS. This must be done explicitly here since the KBItem dropdown
                // is not the focus-component of this editor. In fact, there could be multiple
                // KBItem dropdowns in this feature editor since we can have multilpe modifiers.
                // For focus-components, the AnnotationFeatureForm already handles adding the
                // saving behavior.
                actionHandler
                    .actionCreateOrUpdate(RequestCycle.get().find(AjaxRequestTarget.class), jCas);
            }
            catch (Exception e) {
                LOG.error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
            }
        }
    }


    //TODO: (issue #122 )this method is similar to the method listInstances in ConceptFeatureEditor.
    //It should be refactored.
    private List<KBHandle> listInstances(AnnotationActionHandler aHandler,
        String aTypedString, AnnotationFeature linkedAnnotationFeature, String roleLabe, int
        roleAddr)
    {
        if (linkedAnnotationFeature == null) {
            linkedAnnotationFeature = getLinkedAnnotationFeature();
        }
        List<KBHandle> handles = new ArrayList<>();
        try {
            ConceptFeatureTraits traits = readFeatureTraits(linkedAnnotationFeature);

            // Check if kb is actually enabled
            if (featureUsesDisabledKB(traits)) {
                return Collections.emptyList();
            }

            switch (traits.getAllowedValueType()) {
            case INSTANCE:
                handles = getInstances(traits, project, aHandler, aTypedString, roleLabe, roleAddr);
                break;
            case CONCEPT:
                handles = getConcepts(traits, project, aHandler, aTypedString, roleLabe, roleAddr);
                break;
            default:
                // Allow both
                handles.addAll(
                        getInstances(traits, project, aHandler, aTypedString, roleLabe, roleAddr));
                handles.addAll(
                        getConcepts(traits, project, aHandler, aTypedString, roleLabe, roleAddr));
            }
        }
        catch (Exception e) {
            // LOG.error("Unable to read traits", e);
            error("Unable to read traits: " + ExceptionUtils.getRootCauseMessage(e));
            IPartialPageRequestHandler target = RequestCycle.get()
                    .find(IPartialPageRequestHandler.class);
            if (target != null) {
                target.addChildren(getPage(), IFeedback.class);
            }
        }
        return handles;
    }

    private boolean featureUsesDisabledKB(ConceptFeatureTraits aTraits)
    {
        Optional<KnowledgeBase> kb = null;
        String repositoryId = aTraits.getRepositoryId();
        if (repositoryId != null) {
            kb = kbService.getKnowledgeBaseById(getModelObject().feature.getProject(),
                aTraits.getRepositoryId());
        }
        return kb != null && kb.isPresent() && !kb.get().isEnabled()
            || repositoryId != null && kb == null;
    }

    private ConceptFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature)
    {
        FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry
            .getFeatureSupport(aAnnotationFeature);
        ConceptFeatureTraits traits = fs.readTraits(aAnnotationFeature);
        return traits;
    }

    private List<KBHandle> getInstances(ConceptFeatureTraits traits, Project project,
        AnnotationActionHandler aHandler, String aTypedString, String roleLabe, int
        roleAddr)
    {
        List<KBHandle> handles = new ArrayList<>();
        if (traits.getRepositoryId() != null) {
            // If a specific KB is selected, get its instances
            Optional<KnowledgeBase> kb = kbService
                .getKnowledgeBaseById(project, traits.getRepositoryId());
            if (kb.isPresent()) {
                //TODO: (#122) see ConceptFeatureEditor
                if (kb.get().isSupportConceptLinking()) {
                    handles.addAll(listLinkingInstances(kb.get(), () -> getEditorCas(aHandler),
                            aTypedString, roleLabe, roleAddr));
                }
                else if (traits.getScope() != null) {
                    handles = kbService
                            .listInstancesForChildConcepts(kb.get(), traits.getScope(), false, 50)
                            .stream().filter(inst -> inst.getUiLabel().contains(aTypedString))
                            .collect(Collectors.toList());
                } else {
                    for (KBHandle concept : kbService.listConcepts(kb.get(), false)) {
                        handles.addAll(
                                kbService.listInstances(kb.get(), concept.getIdentifier(), false));
                    }
                }
            }
        }
        else {
            // If no specific KB is selected, collect instances from all KBs
            for (KnowledgeBase kb : kbService.getEnabledKnowledgeBases(project)) {
                //TODO: (#122) see ConceptFeatureEditor
                if (kb.isSupportConceptLinking()) {
                    handles.addAll(listLinkingInstances(kb, () -> getEditorCas
                        (aHandler), aTypedString, roleLabe, roleAddr));
                }
                else if (traits.getScope() != null) {
                    handles.addAll(kbService
                            .listInstancesForChildConcepts(kb, traits.getScope(), false, 50)
                            .stream().filter(inst -> inst.getUiLabel().contains(aTypedString))
                            .collect(Collectors.toList()));
                }
                else {
                    for (KBHandle concept : kbService.listConcepts(kb, false)) {
                        handles.addAll(kbService.listInstances(kb, concept.getIdentifier(), false));
                    }
                }
            }
        }
        return handles;
    }

    private List<KBHandle> getConcepts(ConceptFeatureTraits traits, Project project,
        AnnotationActionHandler aHandler, String aTypedString, String roleLabe, int
        roleAddr)
    {
        List<KBHandle> handles = new ArrayList<>();
        if (traits.getRepositoryId() != null) {
            // If a specific KB is selected, get its instances
            Optional<KnowledgeBase> kb = kbService
                .getKnowledgeBaseById(project, traits.getRepositoryId());
            if (kb.isPresent()) {
                //TODO: (#122) see ConceptFeatureEditor
                if (kb.get().isSupportConceptLinking()) {
                    handles.addAll(listLinkingInstances(kb.get(), () -> getEditorCas
                        (aHandler), aTypedString, roleLabe, roleAddr));
                }
                else if (traits.getScope() != null) {
                    handles = kbService.listChildConcepts(kb.get(), traits.getScope(), false)
                            .stream().filter(conc -> conc.getUiLabel().contains(aTypedString))
                            .collect(Collectors.toList());
                } else {
                    handles.addAll(kbService.listConcepts(kb.get(), false));
                }
            }
        }
        else {
            // If no specific KB is selected, collect instances from all KBs
            for (KnowledgeBase kb : kbService.getEnabledKnowledgeBases(project)) {
                //TODO: (#122) see ConceptFeatureEditor
                if (kb.isSupportConceptLinking()) {
                    handles.addAll(listLinkingInstances(kb, () -> getEditorCas
                        (aHandler), aTypedString, roleLabe, roleAddr));
                }
                else if (traits.getScope() != null) {
                    handles = kbService.listChildConcepts(kb, traits.getScope(), false).stream()
                            .filter(conc -> conc.getUiLabel().contains(aTypedString))
                            .collect(Collectors.toList());
                } else {
                    handles.addAll(kbService.listConcepts(kb, false));
                }
            }
        }
        return handles;
    }

    //TODO: (issue #122 )this method is similar to the method listInstances in ConceptFeatureEditor.
    //It should be refactored.
    private List<KBHandle> listLinkingInstances(KnowledgeBase kb, JCasProvider aJCas,
        String aTypedString, String roleLabel, int roleAddr)
    {
        return kbService.read(kb, (conn) -> {
            try {
                return clService.disambiguate(kb, aTypedString, roleLabel, roleAddr, aJCas.get());
            }
            catch (IOException e) {
                LOG.error("An error occurred while retrieving entity candidates.", e);
                error(e);
                return Collections.emptyList();
            }
        });
    }

    private JCas getEditorCas(AnnotationActionHandler aHandler) throws IOException
    {
        return aHandler.getEditorCas();
    }

    private AutoCompleteTextField<KBHandle> createSelectPropertyAutoCompleteTextField()
    {
        AutoCompleteTextField<KBHandle> field = new AutoCompleteTextField<KBHandle>("newRole",
            new PropertyModel<KBHandle>(this, "selectedRole"),
            new TextRenderer<KBHandle>("uiLabel"), KBHandle.class)
        {

            private static final long serialVersionUID = 1458626823154651501L;

            @Override protected List<KBHandle> getChoices(String input)
            {
                ConceptFeatureTraits traits = factService.getFeatureTraits(project);
                return factService.getPredicatesFromKB(project, traits);
            }

            @Override public void onConfigure(JQueryBehavior behavior)
            {
                super.onConfigure(behavior);
                behavior.setOption("autoWidth", true);
            }

            @Override protected IJQueryTemplate newTemplate()
            {
                return KendoChoiceDescriptionScriptReference.template();
            }
        };

        return field;
    }

    @Override
    public Component getFocusComponent()
    {
        return focusComponent;
    }

    private void actionAdd(AjaxRequestTarget aTarget)
    {
        if (selectedRole == null) {
            error("Must set slot label before adding!");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
        else {
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) QualifierFeatureEditor.this
                .getModelObject().value;
            AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();

            LinkWithRoleModel m = new LinkWithRoleModel();
            m.role = selectedRole.getUiLabel();
            links.add(m);

            // Need to re-render the whole form because a slot in another
            // link editor might get unarmed
            selectedRole = null;
            aTarget.add(getOwner());
        }
    }

    private void actionSet(AjaxRequestTarget aTarget)
    {
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) QualifierFeatureEditor.this
            .getModelObject().value;
        AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();

        // Update the slot
        LinkWithRoleModel m = links.get(state.getArmedSlot());
        m.role = selectedRole.getUiLabel();
        links.set(state.getArmedSlot(), m); // avoid reordering

        aTarget.add(content);

        // Commit change - but only if we set the label on a slot which was already filled/saved.
        // Unset slots only exist in the link editor and if we commit the change here, we trigger
        // a reload of the feature editors from the CAS which makes the unfilled slots disappear
        // and leaves behind an armed slot pointing to a removed slot.
        if (m.targetAddr != -1) {
            try {
                actionHandler.actionCreateOrUpdate(aTarget, actionHandler.getEditorCas());
            }
            catch (Exception e) {
                handleException(this, aTarget, e);
            }
        }
    }

    private void actionDel(AjaxRequestTarget aTarget)
    {
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) QualifierFeatureEditor.this
            .getModelObject().value;
        AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();

        links.remove(state.getArmedSlot());
        state.clearArmedSlot();
        selectedRole = null;

        aTarget.add(content);

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

    private void actionToggleArmedState(AjaxRequestTarget aTarget, Item<LinkWithRoleModel> aItem)
    {
        AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();

        if (state.isArmedSlot(getModelObject().feature, aItem.getIndex())) {
            state.clearArmedSlot();
            selectedRole = null;
            aTarget.add(content);
        }
        else {
            state.setArmedSlot(getModelObject().feature, aItem.getIndex());
            // Need to re-render the whole form because a slot in another
            // link editor might get unarmed
            selectedRole = new KBHandle();
            selectedRole.setName(aItem.getModelObject().role);
            aTarget.add(getOwner());
        }
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
        AnnotationFeature linkedAnnotationFeature = getLinkedAnnotationFeature();

        ConceptFeatureTraits traits = readFeatureTraits(linkedAnnotationFeature);
        warningLabel.add(
            LambdaBehavior.onConfigure(label -> label.setVisible(featureUsesDisabledKB(traits))));

        TooltipBehavior tip = new TooltipBehavior();

        Optional<KnowledgeBase> kb = null;
        if (traits != null && traits.getRepositoryId() != null) {
            kb = kbService.getKnowledgeBaseById(linkedAnnotationFeature.getProject(),
                traits.getRepositoryId());
        }
        String kbName = kb != null && kb.isPresent() ? kb.get().getName() : "";

        tip.setOption("content", Options.asString(
            new StringResourceModel("value.null.disabledKbWarning", this).setParameters(kbName)
                .getString()));
        tip.setOption("width", Options.asString("300px"));
        warningLabel.add(tip);

        return warningLabel;
    }
}
