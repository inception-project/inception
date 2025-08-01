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

import static de.tudarmstadt.ukp.inception.kb.factlinking.feature.FactLinkingConstants.LINKED_LAYER_FEATURE;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
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
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.renderer.TextRenderer;
import org.wicketstuff.jquery.core.template.IJQueryTemplate;
import org.wicketstuff.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBErrorHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderSlotsEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureUtil;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.support.kendo.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

@Deprecated
public class SubjectObjectFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 4230722501745589589L;

    private static final Logger LOG = LoggerFactory.getLogger(SubjectObjectFeatureEditor.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ConceptLinkingService clService;
    private @SpringBean FactLinkingService factService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean KnowledgeBaseService kbService;

    private WebMarkupContainer content;
    private FormComponent<?> focusComponent;
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

        content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        add(content);

        @SuppressWarnings("unchecked")
        var links = (List<LinkWithRoleModel>) SubjectObjectFeatureEditor.this
                .getModelObject().value;

        roleModel = new LinkWithRoleModel();
        roleModel.role = role;
        if (links.size() == 1) {
            roleModel = links.get(0);
        }

        content.add(createSubjectObjectLabel());
        content.add(createRemoveLabelIcon());
        content.add(focusComponent = createAutoCompleteTextField());
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
        label = new Label("label", LoadableDetachableModel.of(this::getSelectionSlotLabel));
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
        @SuppressWarnings("unchecked")
        var links = (List<LinkWithRoleModel>) this.getModelObject().value;
        var state = this.stateModel.getObject();

        var role = roleModel.role;
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
        return stateModel.getObject().isArmedSlot(getModelObject(), 0);
    }

    private void actionToggleArmedState(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = stateModel.getObject();

        if (roleLabelSlotIsSelected()) {
            state.clearArmedSlot();
            aTarget.add(getOwner());
        }
        else {
            state.setArmedSlot(getModelObject(), 0);
            aTarget.add(getOwner());
        }
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

        @SuppressWarnings("unchecked")
        var links = (List<LinkWithRoleModel>) this.getModelObject().value;
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

            @Override
            protected List<KBHandle> getChoices(String input)
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
                CAS cas = actionHandler.getEditorCas();
                FeatureStructure selectedFS = ICasUtil.selectFsByAddr(cas, roleModel.targetAddr);
                FeatureUtil.setFeature(selectedFS, linkedAnnotationFeature,
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
                CAS cas = actionHandler.getEditorCas();
                FeatureStructure selectedFS = ICasUtil.selectFsByAddr(cas, roleModel.targetAddr);
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

        ConceptFeatureTraits traits = readFeatureTraits(linkedAnnotationFeature);
        String repoId = traits.getRepositoryId();
        // Check if kb is actually enabled
        if (!(repoId == null || kbService.isKnowledgeBaseEnabled(project, repoId))) {
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
        return handles;
    }

    private AnnotationFeature getLinkedAnnotationFeature()
    {
        String linkedType = this.getModelObject().feature.getType();
        AnnotationLayer linkedLayer = annotationService
                .findLayer(this.stateModel.getObject().getProject(), linkedType);
        return annotationService.getFeature(LINKED_LAYER_FEATURE, linkedLayer);
    }

    private ConceptFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature)
    {
        FeatureSupport<?> fs = featureSupportRegistry.findExtension(aAnnotationFeature)
                .orElseThrow();
        return (ConceptFeatureTraits) fs.readTraits(aAnnotationFeature);
    }

    private CAS getEditorCas(AnnotationActionHandler aHandler) throws IOException
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

    @OnEvent
    public void onRenderSlotsEvent(RenderSlotsEvent aEvent)
    {
        // Redraw because it could happen that another slot is armed, replacing this.
        aEvent.getRequestHandler().add(this);
    }
}
