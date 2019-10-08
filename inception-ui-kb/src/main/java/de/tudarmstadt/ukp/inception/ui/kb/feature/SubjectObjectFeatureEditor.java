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
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
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
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBErrorHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

public class SubjectObjectFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 4230722501745589589L;

    private static final Logger LOG = LoggerFactory.getLogger(SubjectObjectFeatureEditor.class);

    private @SpringBean AnnotationSchemaService annotationService2;
    private @SpringBean ConceptLinkingService clService2;
    private @SpringBean FactLinkingService factService2;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry2;
    private @SpringBean KnowledgeBaseService kbService2;

    private WebMarkupContainer content2;
    private Component focusComponent2;
    private AnnotationActionHandler actionHandler2;
    private IModel<AnnotatorState> stateModel2;
    private Project project2;
    private LinkWithRoleModel roleModel2;
    private AnnotationFeature linkedAnnotationFeature2;

    public SubjectObjectFeatureEditor(String aId, MarkupContainer aOwner,
        AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
        final IModel<FeatureState> aFeatureStateModel, String role)
    {
        super(aId, aOwner, CompoundPropertyModel.of(aFeatureStateModel));

        stateModel2 = aStateModel;
        actionHandler2 = aHandler;
        project2 = this.getModelObject().feature.getProject();

        add(createDisabledKbWarningLabel());

        content2 = new WebMarkupContainer("content2");
        content2.setOutputMarkupId(true);
        add(content2);

        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) SubjectObjectFeatureEditor.this
            .getModelObject().value;

        roleModel2 = new LinkWithRoleModel();
        roleModel2.role = role;
        if (links.size() == 1) {
            roleModel2 = links.get(0);
        }

        content2.add(createSubjectObjectLabel());
        content2.add(createRemoveLabelIcon());
        content2.add(focusComponent2 = createAutoCompleteTextField());
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
            label.setDefaultModelObject(roleModel2.label);
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
        AnnotatorState state = this.stateModel2.getObject();

        String role = roleModel2.role;
        roleModel2 = new LinkWithRoleModel();
        roleModel2.role = role;
        links.set(0, roleModel2);

        // Auto-commit if working on existing annotation
        if (state.getSelection().getAnnotation().isSet()) {
            try {
                actionHandler2.actionCreateOrUpdate(aTarget, actionHandler2.getEditorCas());
            }
            catch (Exception e) {
                handleException1(this, aTarget, e);
            }
        }
    }

    private String getSelectionSlotLabel()
    {
        if (!roleLabelIsFilled() && roleLabelSlotIsSelected()) {
            return "<Select to fill>";
        }
        else {
            return roleModel2.label;
        }
    }

    private boolean roleLabelIsFilled()
    {

        return roleModel2.targetAddr != -1;
    }

    private boolean roleLabelSlotIsSelected()
    {
        return stateModel2.getObject().isArmedSlot(getModelObject(), 0);
    }

    private void actionToggleArmedState(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = stateModel2.getObject();

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
    public Component getFocusComponent()
    {
        return focusComponent2;
    }

    @Override
    public void onConfigure()
    {
        super.onConfigure();
        
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) this.getModelObject().value;
        if (links.size() == 0) {
            String role = roleModel2.role;
            roleModel2 = new LinkWithRoleModel();
            roleModel2.role = role;
            links.add(roleModel2);
        }
        else {
            roleModel2 = links.get(0);
        }
        linkedAnnotationFeature2 = getLinkedAnnotationFeature();
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
                return listInstances(actionHandler2, input);
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
                CAS cas = actionHandler2.getEditorCas();
                FeatureStructure selectedFS = selectFsByAddr(cas, roleModel2.targetAddr);
                WebAnnoCasUtil.setFeature(selectedFS, linkedAnnotationFeature2,
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
                CAS cas = actionHandler2.getEditorCas();
                FeatureStructure selectedFS = selectFsByAddr(cas, roleModel2.targetAddr);
                selectedKBItemIdentifier = WebAnnoCasUtil.getFeature(selectedFS,
                        linkedAnnotationFeature2.getName());
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
                    ConceptFeatureTraits traits = factService2.getFeatureTraits(project2);
                    selectedKBHandleItem = factService2.getKBInstancesByIdentifierAndTraits(
                            selectedKBItemIdentifier, project2, traits);
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
        if (linkedAnnotationFeature2 == null) {
            linkedAnnotationFeature2 = getLinkedAnnotationFeature();
        }
        List<KBHandle> handles = new ArrayList<>();

        ConceptFeatureTraits traits = readFeatureTraits(linkedAnnotationFeature2);
        String repoId = traits.getRepositoryId();
        // Check if kb is actually enabled
        if (!(repoId == null || kbService2.isKnowledgeBaseEnabled(project2, repoId))) {
            return Collections.emptyList();
        }

        // Use concept linking if enabled
        try {
            handles = clService2.getLinkingInstancesInKBScope(traits.getRepositoryId(),
                    traits.getScope(), traits.getAllowedValueType(), aTypedString, roleModel2.label,
                    roleModel2.targetAddr, getEditorCas(aHandler), project2);
        }
        catch (IOException e) {
            LOG.error("An error occurred while retrieving entity candidates.", e);
            error("An error occurred while retrieving entity candidates: " + e.getMessage());
            RequestCycle.get().find(IPartialPageRequestHandler.class)
                .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
        }
        return handles;
    }

    private AnnotationFeature getLinkedAnnotationFeature() {
        String linkedType = this.getModelObject().feature.getType();
        AnnotationLayer linkedLayer = annotationService2
            .findLayer(this.stateModel2.getObject().getProject(), linkedType);
        AnnotationFeature linkedAnnotationFeature2 = annotationService2
            .getFeature(FactLinkingConstants.LINKED_LAYER_FEATURE, linkedLayer);
        return linkedAnnotationFeature2;
    }

    private ConceptFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature) {
        FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry2
            .getFeatureSupport(aAnnotationFeature);
        ConceptFeatureTraits traits = fs.readTraits(aAnnotationFeature);
        return traits;
    }

    private CAS getEditorCas(AnnotationActionHandler aHandler) throws IOException
    {
        return aHandler.getEditorCas();
    }

    public static void handleException1(Component aComponent, AjaxRequestTarget aTarget,
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

    private DisabledKBWarning createDisabledKbWarningLabel()
    {
        if (linkedAnnotationFeature2 == null) {
            linkedAnnotationFeature2 = getLinkedAnnotationFeature();
        }
        return new DisabledKBWarning("disabledKBWarning", Model.of(linkedAnnotationFeature2));
    }

    @OnEvent
    public void onRenderSlotsEvent(RenderSlotsEvent aEvent)
    {
        // Redraw because it could happen that another slot is armed, replacing this.
        aEvent.getRequestHandler().add(this);
    }
}
