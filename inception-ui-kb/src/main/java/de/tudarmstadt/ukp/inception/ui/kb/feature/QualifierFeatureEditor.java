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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
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
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.renderer.TextRenderer;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;

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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderSlotsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;

public class QualifierFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 7469241620229001983L;
    
    private static final Logger LOG = LoggerFactory.getLogger(QualifierFeatureEditor.class);

    private @SpringBean AnnotationSchemaService annotationService1;
    private @SpringBean ConceptLinkingService clService1;
    private @SpringBean FactLinkingService factService1;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry1;
    private @SpringBean KnowledgeBaseService kbService1;

    private WebMarkupContainer content1;
    private Component focusComponent1;
    private AnnotationActionHandler actionHandler1;
    private IModel<AnnotatorState> stateModel1;
    private Project project1;
    private LambdaModelAdapter<KBHandle> qualifierModel1;
    private KBHandle selectedRole1;

    public QualifierFeatureEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
            final IModel<FeatureState> aFeatureStateModel)
    {
        super(aId, aOwner, CompoundPropertyModel.of(aFeatureStateModel));

        stateModel1 = aStateModel;
        actionHandler1 = aHandler;
        project1 = stateModel1.getObject().getProject();

        // Add warning that shows up if the knowledge base that is used by the concept feature
        // is disabled
        add(new DisabledKBWarning("disabledKBWarning", Model.of(getLinkedAnnotationFeature())));

        // Most of the content1 is inside this container such that we can refresh it independently
        // from the rest of the form
        content1 = new WebMarkupContainer("content1");
        content1.setOutputMarkupId(true);
        add(content1);

        content1.add(new RefreshingView<LinkWithRoleModel>("slots",
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
                AnnotatorState state = stateModel1.getObject();

                aItem.setModel(new CompoundPropertyModel<>(aItem.getModelObject()));
                Label role = new Label("role");
                aItem.add(role);

                final Label label;
                if (aItem.getModelObject().targetAddr == -1
                    && state.isArmedSlot(getModelObject(), aItem.getIndex())) {
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
                        if (state.isArmedSlot(getModelObject(), aItem.getIndex())) {
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
        content1.add(focusComponent1 = createSelectPropertyAutoCompleteTextField());

        // Add a new empty slot with the specified role
        content1.add(new AjaxButton("add")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                
                AnnotatorState state = QualifierFeatureEditor.this.stateModel1.getObject();
                setVisible(!(state.isSlotArmed() && QualifierFeatureEditor.this.getModelObject()
                    .feature.equals(state.getArmedFeature())));
                // setEnabled(!(model.isSlotArmed()
                // && aModel.feature.equals(model.getArmedFeature())));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                actionAdd(aTarget);
            }
        });

        // Allows user to update slot
        content1.add(new AjaxButton("set")
        {

            private static final long serialVersionUID = 7923695373085126646L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                
                AnnotatorState state = QualifierFeatureEditor.this.stateModel1.getObject();
                setVisible(state.isSlotArmed() && QualifierFeatureEditor.this.getModelObject()
                    .feature.equals(state.getArmedFeature()));
                // setEnabled(model.isSlotArmed()
                // && aModel.feature.equals(model.getArmedFeature()));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                actionSet(aTarget);
            }
        });

        // Add a new empty slot with the specified role
        content1.add(new AjaxButton("del")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                
                AnnotatorState state = QualifierFeatureEditor.this.stateModel1.getObject();
                setVisible(state.isSlotArmed() && QualifierFeatureEditor.this.getModelObject()
                    .feature.equals(state.getArmedFeature()));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                actionDel(aTarget);
            }
        });
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

        qualifierModel1 = new LambdaModelAdapter<>(() -> this.getSelectedKBItem(aItem), (v) -> {
            this.setSelectedKBItem((KBHandle) v, aItem, linkedAnnotationFeature);
        });

        AutoCompleteTextField<KBHandle> field = new AutoCompleteTextField<KBHandle>("value",
            qualifierModel1, new TextRenderer<KBHandle>("uiLabel"), KBHandle.class)
        {

            private static final long serialVersionUID = 5683897252648514996L;

            @Override
            protected List<KBHandle> getChoices(String input)
            {
                return listInstances(actionHandler1, input, linkedAnnotationFeature,
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
        AnnotationLayer linkedLayer = annotationService1
            .findLayer(this.stateModel1.getObject().getProject(), linkedType);
        AnnotationFeature linkedAnnotationFeature = annotationService1
            .getFeature(FactLinkingConstants.LINKED_LAYER_FEATURE, linkedLayer);
        return linkedAnnotationFeature;
    }

    private KBHandle getSelectedKBItem(Item<LinkWithRoleModel> aItem) {
        KBHandle selectedKBHandleItem = null;
        if (aItem.getModelObject().targetAddr != -1) {
            try {
                ConceptFeatureTraits traits = factService1.getFeatureTraits(project1);
                CAS cas = actionHandler1.getEditorCas();
                int targetAddr = aItem.getModelObject().targetAddr;
                selectedKBHandleItem = factService1.getKBHandleFromCasByAddr(cas, targetAddr,
                    project1, traits);
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
                CAS cas = actionHandler1.getEditorCas();
                FeatureStructure selectedFS = selectFsByAddr(cas,
                        aItem.getModelObject().targetAddr);
                WebAnnoCasUtil.setFeature(selectedFS, linkedAnnotationFeature,
                    value != null ? value.getIdentifier() : value);
                LOG.info("change the value");
                qualifierModel1.detach();

                // Save the CAS. This must be done explicitly here since the KBItem dropdown
                // is not the focus-component of this editor. In fact, there could be multiple
                // KBItem dropdowns in this feature editor since we can have multilpe modifiers.
                // For focus-components, the AnnotationFeatureForm already handles adding the
                // saving behavior.
                actionHandler1.actionCreateOrUpdate(
                        RequestCycle.get().find(AjaxRequestTarget.class).get(), cas);
            }
            catch (Exception e) {
                LOG.error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
            }
        }
    }

    private List<KBHandle> listInstances(AnnotationActionHandler aHandler,
        String aTypedString, AnnotationFeature linkedAnnotationFeature, String roleLabel, int
        roleAddr)
    {
        if (linkedAnnotationFeature == null) {
            linkedAnnotationFeature = getLinkedAnnotationFeature();
        }
        List<KBHandle> handles = new ArrayList<>();

        ConceptFeatureTraits traits = readFeatureTraits(linkedAnnotationFeature);
        // Check if kb is actually enabled
        String repoId = traits.getRepositoryId();
        if (!(repoId == null || kbService1.isKnowledgeBaseEnabled(project1, repoId))) {
            return Collections.emptyList();
        }

        // Use concept linking if enabled
        try {
            handles = clService1.getLinkingInstancesInKBScope(traits.getRepositoryId(),
                    traits.getScope(), traits.getAllowedValueType(), aTypedString, roleLabel,
                    roleAddr, getEditorCas(aHandler), project1);
        }
        catch (IOException e) {
            LOG.error("An error occurred while retrieving entity candidates.", e);
            error("An error occurred while retrieving entity candidates: " + e.getMessage());
            RequestCycle.get().find(IPartialPageRequestHandler.class)
                .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
        }
        return handles;

    }

    private ConceptFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature)
    {
        FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry1
            .getFeatureSupport(aAnnotationFeature);
        ConceptFeatureTraits traits = fs.readTraits(aAnnotationFeature);
        return traits;
    }

    private CAS getEditorCas(AnnotationActionHandler aHandler) throws IOException
    {
        return aHandler.getEditorCas();
    }

    private AutoCompleteTextField<KBProperty> createSelectPropertyAutoCompleteTextField()
    {
        AutoCompleteTextField<KBProperty> field = new AutoCompleteTextField<KBProperty>("newRole",
            new PropertyModel<KBProperty>(this, "selectedRole1"),
            new TextRenderer<KBProperty>("uiLabel"), KBProperty.class)
        {

            private static final long serialVersionUID = 1458626823154651501L;

            @Override protected List<KBProperty> getChoices(String input)
            {
                ConceptFeatureTraits traits = factService1.getFeatureTraits(project1);
                String repoId = traits.getRepositoryId();
                if (!(repoId == null || kbService1.isKnowledgeBaseEnabled(project1, repoId))) {
                    return Collections.emptyList();
                }
                return factService1.listProperties(project1, traits);
            }

            @Override
            public void onConfigure(JQueryBehavior behavior)
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
        return focusComponent1;
    }

    private void actionAdd(AjaxRequestTarget aTarget)
    {
        if (selectedRole1 == null) {
            error("Must set slot label before adding!");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
        else {
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) QualifierFeatureEditor.this
                .getModelObject().value;
            AnnotatorState state = QualifierFeatureEditor.this.stateModel1.getObject();

            LinkWithRoleModel m = new LinkWithRoleModel();
            m.role = selectedRole1.getUiLabel();
            links.add(m);

            // Need to re-render the whole form because a slot in another
            // link editor might get unarmed
            selectedRole1 = null;
            aTarget.add(getOwner());
        }
    }

    private void actionSet(AjaxRequestTarget aTarget)
    {
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) QualifierFeatureEditor.this
            .getModelObject().value;
        AnnotatorState state = QualifierFeatureEditor.this.stateModel1.getObject();

        // Update the slot
        LinkWithRoleModel m = links.get(state.getArmedSlot());
        m.role = selectedRole1.getUiLabel();
        links.set(state.getArmedSlot(), m); // avoid reordering

        aTarget.add(content1);

        // Commit change - but only if we set the label on a slot which was already filled/saved.
        // Unset slots only exist in the link editor and if we commit the change here, we trigger
        // a reload of the feature editors from the CAS which makes the unfilled slots disappear
        // and leaves behind an armed slot pointing to a removed slot.
        if (m.targetAddr != -1) {
            try {
                actionHandler1.actionCreateOrUpdate(aTarget, actionHandler1.getEditorCas());
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
        AnnotatorState state = QualifierFeatureEditor.this.stateModel1.getObject();

        links.remove(state.getArmedSlot());
        state.clearArmedSlot();
        selectedRole1 = null;

        aTarget.add(content1);

        // Auto-commit if working on existing annotation
        if (state.getSelection().getAnnotation().isSet()) {
            try {
                actionHandler1.actionCreateOrUpdate(aTarget, actionHandler1.getEditorCas());
            }
            catch (Exception e) {
                handleException(this, aTarget, e);
            }
        }
    }

    private void actionToggleArmedState(AjaxRequestTarget aTarget, Item<LinkWithRoleModel> aItem)
    {
        AnnotatorState state = QualifierFeatureEditor.this.stateModel1.getObject();

        if (state.isArmedSlot(getModelObject(), aItem.getIndex())) {
            state.clearArmedSlot();
            selectedRole1 = null;
            aTarget.add(content1);
        }
        else {
            state.setArmedSlot(getModelObject(), aItem.getIndex());
            // Need to re-render the whole form because a slot in another
            // link editor might get unarmed
            selectedRole1 = new KBHandle();
            selectedRole1.setName(aItem.getModelObject().role);
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

    @OnEvent
    public void onRenderSlotsEvent(RenderSlotsEvent aEvent)
    {
        // Redraw because it could happen that another slot is armed, replacing this.
        aEvent.getRequestHandler().add(this);
    }
}
