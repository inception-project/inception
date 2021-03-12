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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE_TYPE_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getNextToken;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSame;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxPreventSubmitBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.kendo.ui.form.TextField;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.AttachedAnnotation;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.AnnotationCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.AnnotationEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.event.LinkFeatureDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.AnnotatorViewportChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.SelectionChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.Evaluator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.RulesIndicator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ValuesGenerator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.event.DefaultLayerChangedEvent;

/**
 * Annotation Detail Editor Panel.
 */
public abstract class AnnotationDetailEditorPanel
    extends Panel
    implements AnnotationActionHandler
{
    private static final long serialVersionUID = 7324241992353693848L;

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationDetailEditorPanel.class);

    private static final String KEY_BACKSPACE = "8";
    private static final String KEY_ENTER = "13";

    private @SpringBean AnnotationSchemaService annotationService;

    // Top-level containers
    private final LayerSelectionPanel layerSelectionPanel;
    private final AnnotationInfoPanel selectedAnnotationInfoPanel;
    private final FeatureEditorListPanel featureEditorListPanel;
    private final WebMarkupContainer buttonContainer;
    private final AttachedAnnotationListPanel relationListPanel;

    // Components
    private final ConfirmationDialog deleteAnnotationDialog;
    private final ConfirmationDialog replaceAnnotationDialog;
    private final AnnotationPageBase editorPage;

    private TextField<String> forwardAnnotationTextField;
    private String forwardAnnotationKeySequence = "";

    public AnnotationDetailEditorPanel(String id, AnnotationPageBase aPage,
            IModel<AnnotatorState> aModel)
    {
        super(id, new CompoundPropertyModel<>(aModel));

        editorPage = aPage;

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        setMarkupId("annotationDetailEditorPanel");

        add(createForwardAnnotationKeySequenceCapturingForm());

        add(deleteAnnotationDialog = new ConfirmationDialog("deleteAnnotationDialog",
                new StringResourceModel("DeleteDialog.title", this, null)));
        add(replaceAnnotationDialog = new ConfirmationDialog("replaceAnnotationDialog",
                new StringResourceModel("ReplaceDialog.title", this, null),
                new StringResourceModel("ReplaceDialog.text", this, null)));
        add(layerSelectionPanel = new LayerSelectionPanel("layerContainer", getModel(), this));
        add(selectedAnnotationInfoPanel = new AnnotationInfoPanel("infoContainer", getModel(),
                this));
        add(featureEditorListPanel = new FeatureEditorListPanel("featureEditorContainer",
                getModel(), this));
        add(relationListPanel = new AttachedAnnotationListPanel("relationListContainer", aPage,
                this, aModel));
        relationListPanel.setOutputMarkupPlaceholderTag(true);

        buttonContainer = new WebMarkupContainer("buttonContainer");
        buttonContainer.setOutputMarkupPlaceholderTag(true);
        buttonContainer.add(createDeleteButton());
        buttonContainer.add(createReverseButton());
        buttonContainer.add(createClearButton());
        add(buttonContainer);
    }

    private Component createForwardAnnotationKeySequenceCapturingForm()
    {
        Form<Void> form = new Form<>("forwardForm");

        TextField<String> textfield = new TextField<>("forwardAnno");
        textfield.setModel(Model.of());
        textfield.setOutputMarkupId(true);
        // We don't want the form to be submitted when the user pressed ENTER. Instead, we want to
        // capture the key event and send it as part of the AJAX request. Note that the
        // AjaxPreventSubmitBehavior triggers on "keydown" while our
        // AjaxFormComponentUpdatingBehavior has to trigger on "keyup", otherwise the pressed key
        // does not end up in the TextField's model.
        textfield.add(new AjaxPreventSubmitBehavior());
        textfield.add(new AjaxFormComponentUpdatingBehavior("keyup")
        {
            private static final long serialVersionUID = 4554834769861958396L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
            {
                super.updateAjaxAttributes(attributes);

                attributes.getDynamicExtraParameters()
                        .add("return { 'keycode': Wicket.Event.keyCode(attrs.event) };");
            }

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = getModelObject();

                // Forward annotation mode only works on span layers
                if (!state.getSelection().isSpan()) {
                    return;
                }

                // If the user has selected an annotation of a different type or no annotation at
                // all, then the forward-annotation key bindings must not be considered.
                AnnotationLayer layer = state.getSelectedAnnotationLayer();
                if (layer == null || !layer.equals(state.getDefaultAnnotationLayer())) {
                    return;
                }

                try {
                    final Request request = RequestCycle.get().getRequest();
                    final String jsKeycode = request.getRequestParameters()
                            .getParameterValue("keycode").toString("");

                    if (KEY_ENTER.equals(jsKeycode)) {
                        CAS cas = getEditorCas();
                        actionCreateForward(aTarget, cas);
                        setForwardAnnotationKeySequence(null, "complete annotation (space)");
                        return;
                    }
                    else if (KEY_BACKSPACE.equals(jsKeycode)) {
                        FeatureState featureState = getModelObject().getFeatureStates().get(0);
                        featureState.value = null;
                        setForwardAnnotationKeySequence(null, "delete annotation (backspace)");
                        CAS cas = getEditorCas();
                        actionCreateForward(aTarget, cas);
                    }
                    else {
                        String newTag = (textfield.getModelObject() == null ? ""
                                : textfield.getModelObject().charAt(0))
                                + getForwardAnnotationKeySequence();
                        setForwardAnnotationKeySequence(newTag, "cycle tags");

                        Map<String, String> bindTags = buildKeySequenceToTagMap();
                        if (!bindTags.isEmpty()) {
                            FeatureState featureState = getModelObject().getFeatureStates().get(0);
                            featureState.value = getTagForKeySequence(
                                    getForwardAnnotationKeySequence(), bindTags);
                        }
                    }

                    aTarget.add(textfield);

                    getFeatureEditorListPanel().getFirstFeatureEditor().ifPresent(aTarget::add);
                }
                catch (Exception e) {
                    handleException(textfield, aTarget, e);
                }
            }
        });
        form.add(textfield);

        forwardAnnotationTextField = textfield;

        return form;
    }

    /**
     * Part of <i>forward annotation</i> mode with tagsets: when the forward annotation mode is used
     * on a string feature with a tagset, the user presses the first letter of a tag repeatedly to
     * cycle through the tags starting with that letter. Thus e.g. {@code nn} means <i>the second
     * tag starting with an {@code n}</i>. This class field stores the key sequence. The <i>key</i>
     * in the method name does not refer to a keyboard key, but rather to being a key in the map
     * returned by {@link #buildKeySequenceToTagMap()}.
     * 
     * @see #getTagForKeySequence(String, Map)
     */
    protected void setForwardAnnotationKeySequence(String aSelectedTag, String aReason)
    {
        LOG.trace("setForwardAnnotationKeySequence({}) - {}", aSelectedTag, aReason);

        forwardAnnotationKeySequence = aSelectedTag;
    }

    /**
     * Part of <i>forward annotation</i> mode with tagsets: for details see
     * {@link #setForwardAnnotationKeySequence(String, String)}.
     * 
     * @see #setForwardAnnotationKeySequence(String, String)
     */
    protected String getForwardAnnotationKeySequence()
    {
        return forwardAnnotationKeySequence;
    }

    /**
     * Part of <i>forward annotation</i> mode with tagsets: returns a map which assigns key
     * sequences to tags from the tagset associated with the forward feature, e.g.:
     * <ul>
     * <li>{@code a} -> {@code ADJ}</li>
     * <li>{@code aa} -> {@code ADP}</li>
     * <li>{@code n} -> {@code NOUN}</li>
     * <li>{@code nn} -> {@code NUM}</li>
     * <li>...</li>
     * </ul>
     * 
     * @see #getTagForKeySequence(String, Map)
     */
    private Map<String, String> buildKeySequenceToTagMap()
    {
        AnnotationFeature f = annotationService
                .listAnnotationFeature(getModelObject().getDefaultAnnotationLayer()).get(0);
        TagSet tagSet = f.getTagset();
        Map<Character, String> tagNames = new LinkedHashMap<>();
        Map<String, String> bindTag2Key = new LinkedHashMap<>();
        for (Tag tag : annotationService.listTags(tagSet)) {
            if (tagNames.containsKey(tag.getName().toLowerCase().charAt(0))) {
                String oldBinding = tagNames.get(tag.getName().toLowerCase().charAt(0));
                String newBinding = oldBinding + tag.getName().toLowerCase().charAt(0);
                tagNames.put(tag.getName().toLowerCase().charAt(0), newBinding);
                bindTag2Key.put(newBinding, tag.getName());
            }
            else {
                tagNames.put(tag.getName().toLowerCase().charAt(0),
                        tag.getName().toLowerCase().substring(0, 1));
                bindTag2Key.put(tag.getName().toLowerCase().substring(0, 1), tag.getName());
            }
        }
        return bindTag2Key;
    }

    /**
     * Part of <i>forward annotation</i> mode: returns the tag associated with the given key
     * sequence.
     * 
     * This method has a side-effect on {@link #setForwardAnnotationKeySequence(String, String)}: If
     * the sequence is is too long (e.g. {@code nnn} when there are only two tags starting with an
     * {@code n}) then the sequence is suitably truncated. If the sequence consists of different
     * characters, it is truncated to the last character in order to select tags starting with that
     * character.
     * 
     * @see #buildKeySequenceToTagMap()
     */
    private String getTagForKeySequence(String aSequence, Map<String, String> aBindTags)
    {
        // check if all the key pressed are the same character
        // if not, just check a Tag for the last char pressed
        if (aSequence.isEmpty()) {
            return aBindTags.get(aBindTags.keySet().iterator().next());
        }
        char prevC = aSequence.charAt(0);
        for (char ch : aSequence.toCharArray()) {
            if (ch != prevC) {
                break;
            }
        }

        if (aBindTags.get(aSequence) != null) {
            return aBindTags.get(aSequence);
        }
        // re-cycle suggestions
        if (aBindTags.containsKey(aSequence.substring(0, 1))) {
            setForwardAnnotationKeySequence(aSequence.substring(0, 1), "reset tag cycling");
            return aBindTags.get(aSequence.substring(0, 1));
        }
        // set it to the first in the tag list , when arbitrary key is pressed
        return aBindTags.get(aBindTags.keySet().iterator().next());
    }

    private void createNewAnnotation(AjaxRequestTarget aTarget, TypeAdapter aAdapter, CAS aCas)
        throws AnnotationException, IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getSelection().isArc()) {
            if (aAdapter instanceof SpanAdapter) {
                error("Layer [" + aAdapter.getLayer().getUiName()
                        + "] does not support arc annotation.");
                aTarget.addChildren(getPage(), IFeedback.class);
            }
            else if (aAdapter instanceof RelationAdapter) {
                createNewRelationAnnotation((RelationAdapter) aAdapter, aCas);
            }
            else if (aAdapter instanceof ChainAdapter) {
                createNewChainLinkAnnotation((ChainAdapter) aAdapter, aCas);
            }
            else {
                throw new IllegalStateException("I don't know how to use ["
                        + aAdapter.getClass().getSimpleName() + "] in this situation.");
            }
        }
        else {
            if (aAdapter instanceof SpanAdapter) {
                createNewSpanAnnotation(aTarget, (SpanAdapter) aAdapter, aCas);
            }
            else if (aAdapter instanceof ChainAdapter) {
                createNewChainElement(aTarget, (ChainAdapter) aAdapter, aCas);
            }
            else {
                throw new IllegalStateException("I don't know how to use ["
                        + aAdapter.getClass().getSimpleName() + "] in this situation.");
            }
        }
    }

    private void createNewRelationAnnotation(RelationAdapter aAdapter, CAS aCas)
        throws AnnotationException
    {
        LOG.trace("createNewRelationAnnotation()");

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();

        AnnotationFS originFs = selectAnnotationByAddr(aCas, selection.getOrigin());
        AnnotationFS targetFs = selectAnnotationByAddr(aCas, selection.getTarget());

        // Creating a relation
        AnnotationFS arc = aAdapter.add(state.getDocument(), state.getUser().getUsername(),
                originFs, targetFs, aCas);
        selection.selectArc(new VID(arc), originFs, targetFs);
    }

    private void createNewSpanAnnotation(AjaxRequestTarget aTarget, SpanAdapter aAdapter, CAS aCas)
        throws IOException, AnnotationException
    {
        AnnotatorState state = getModelObject();

        Selection selection = state.getSelection();

        AnnotationFS annoFs = aAdapter.add(state.getDocument(), state.getUser().getUsername(), aCas,
                selection.getBegin(), selection.getEnd());
        selection.selectSpan(annoFs);
    }

    private void createNewChainElement(AjaxRequestTarget aTarget, ChainAdapter aAdapter, CAS aCas)
        throws AnnotationException
    {
        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();

        AnnotationFS annoFs = aAdapter.addSpan(state.getDocument(), state.getUser().getUsername(),
                aCas, selection.getBegin(), selection.getEnd());
        selection.selectSpan(annoFs);
    }

    private void createNewChainLinkAnnotation(ChainAdapter aAdapter, CAS aCas)
    {
        LOG.trace("createNewChainLinkAnnotation()");

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();

        AnnotationFS originFs = selectAnnotationByAddr(aCas, selection.getOrigin());
        AnnotationFS targetFs = selectAnnotationByAddr(aCas, selection.getTarget());

        // Creating a new chain link
        int addr = aAdapter.addArc(state.getDocument(), state.getUser().getUsername(), aCas,
                originFs, targetFs);
        selection.selectArc(new VID(addr), originFs, targetFs);
    }

    @Override
    public void actionFillSlot(AjaxRequestTarget aTarget, CAS aCas, int aSlotFillerBegin,
            int aSlotFillerEnd, VID aExistingSlotFillerId)
        throws AnnotationException, IOException
    {
        assert aCas != null;

        AnnotatorState state = getModelObject();

        editorPage.ensureIsEditable();

        // If this method is called when no slot is armed, it must be a bug!
        if (!state.isSlotArmed()) {
            throw new IllegalStateException("No slot is armed.");
        }

        // If the user did not select an existing annotation but simply marked a span of text to
        // fill a slot, then we try to create a new span annotation corresponding to the slot
        // filler type defined in the feature. However, this only works if the slot filler is a
        // concrete span type defined in the project, not if the user simply defined
        // CAS.TYPE_NAME_ANNOTATION to allow for arbitrary slot fillers. In the latter case, we
        // abort the operation with an IllegalPlacementException.
        int slotFillerAddr;
        if (aExistingSlotFillerId.isNotSet()) {
            if (!CAS.TYPE_NAME_ANNOTATION.equals(state.getArmedFeature().feature.getType())) {
                SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(annotationService
                        .findLayer(state.getProject(), state.getArmedFeature().feature.getType()));

                slotFillerAddr = getAddr(adapter.add(state.getDocument(),
                        state.getUser().getUsername(), aCas, aSlotFillerBegin, aSlotFillerEnd));
            }
            else {
                throw new IllegalPlacementException(
                        "Unable to create annotation of type [" + CAS.TYPE_NAME_ANNOTATION
                                + "]. Please click an annotation in stead of selecting new text.");
            }
        }
        else {
            slotFillerAddr = aExistingSlotFillerId.getId();
        }

        // Inject the slot filler into the respective slot
        FeatureStructure slotHostFS = selectFsByAddr(aCas, state.getArmedFeature().vid.getId());
        AnnotationLayer slotHostLayer = annotationService.findLayer(state.getProject(), slotHostFS);
        TypeAdapter slotHostAdapter = annotationService.getAdapter(slotHostLayer);
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) state.getArmedFeature().value;
        LinkWithRoleModel link = links.get(state.getArmedSlot());
        link.targetAddr = slotFillerAddr;
        link.label = selectAnnotationByAddr(aCas, slotFillerAddr).getCoveredText();
        commitFeatureStatesToFeatureStructure(aTarget, state.getDocument(),
                state.getUser().getUsername(), aCas, state.getArmedFeature().vid.getId(),
                slotHostAdapter, asList(state.getArmedFeature()));

        // NOTE: we do NOT delegate to actionCreateOrUpdate here because most of the things
        // that actionCreateOrUpdate does are not required for slot filling and we also because
        // slot filling requires special treatment. This also means, we don't delegate to
        // internalCommitAnnotation and repeat the necessary code here. However, we DO delegate
        // to internalCompleteAnnotation to save the CAS after the annotation.

        internalCompleteAnnotation(aTarget, aCas);

        // If the armed slot is located in the annotation detail editor panel (right side) update
        // the annotator state with the changes that we made to the CAS
        if (state.getSelection().getAnnotation().equals(state.getArmedFeature().vid)) {
            // Loading feature editor values from CAS
            loadFeatureEditorModels(aTarget);
        }
        // ... if the SLOT HOST annotation is NOT open in the detail panel on the right, then
        // select SLOT FILLER an open it there
        else {
            state.getSelection().selectSpan(selectAnnotationByAddr(aCas, slotFillerAddr));
            actionSelect(aTarget);
        }

        state.clearArmedSlot();
    }

    @Override
    public void actionSelect(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        // Edit existing annotation
        loadFeatureEditorModels(aTarget);

        if (aTarget != null) {
            refresh(aTarget);
            // aTarget.add(buttonContainer);
            // aTarget.add(selectedAnnotationInfoPanel, featureEditorListPanel, relationListPanel);
        }

        // Ensure we re-render and update the highlight
        onChange(aTarget);
    }

    @Override
    public void actionSelect(AjaxRequestTarget aTarget, AnnotationFS annoFs)
        throws IOException, AnnotationException
    {
        AnnotatorState state = getModelObject();

        TypeAdapter adapter = annotationService
                .getAdapter(annotationService.findLayer(state.getProject(), annoFs));

        adapter.select(getModelObject(), annoFs);
        actionSelect(aTarget);
    }

    @Override
    public void actionSelect(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        actionSelect(aTarget, selectAnnotationByAddr(editorPage.getEditorCas(), aVid.getId()));
    }

    @Override
    public void actionJump(AjaxRequestTarget aTarget, AnnotationFS aFS)
        throws IOException, AnnotationException
    {
        editorPage.actionShowSelectedDocument(aTarget, getModelObject().getDocument(),
                aFS.getBegin(), aFS.getEnd());
    }

    @Override
    public void actionJump(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        actionJump(aTarget, selectAnnotationByAddr(editorPage.getEditorCas(), aVid.getId()));
    }

    @Override
    public void actionSelectAndJump(AjaxRequestTarget aTarget, AnnotationFS annoFs)
        throws IOException, AnnotationException
    {
        actionSelect(aTarget, annoFs);
        editorPage.actionShowSelectedDocument(aTarget, getModelObject().getDocument(),
                annoFs.getBegin(), annoFs.getEnd());
    }

    @Override
    public void actionSelectAndJump(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        CAS cas = editorPage.getEditorCas();
        AnnotationFS annoFs = selectAnnotationByAddr(cas, aVid.getId());
        actionSelectAndJump(aTarget, annoFs);
    }

    @Override
    public void actionCreateOrUpdate(AjaxRequestTarget aTarget, CAS aCas)
        throws IOException, AnnotationException
    {
        LOG.trace("actionAnnotate");

        editorPage.ensureIsEditable();

        AnnotatorState state = getModelObject();

        // Note that refresh changes the selected layer if a relation is created. Then the layer
        // switches from the selected span layer to the relation layer that is attached to the span
        if (state.getSelection().isArc()) {
            LOG.trace("actionAnnotate() relation annotation - looking for attached layer");

            // FIXME REC I think this whole section which meddles around with the selected
            // annotation layer should be moved out of there to the place where we originally set
            // the annotation layer...!

            // Fetch the annotation representing the origin endpoint of the relation
            AnnotationFS originFS = selectAnnotationByAddr(aCas, state.getSelection().getOrigin());
            AnnotationFS targetFS = selectAnnotationByAddr(aCas, state.getSelection().getTarget());

            if (!originFS.getType().equals(targetFS.getType())) {
                reset(aTarget);
                onChange(aTarget);
                throw new IllegalPlacementException(
                        "Cannot create relation between spans on different layers");
            }

            // Fetch the annotation layer for the origin annotation
            AnnotationLayer originLayer = annotationService.findLayer(state.getProject(), originFS);

            AnnotationLayer previousLayer = state.getSelectedAnnotationLayer();

            // If we are creating a relation annotation, we have to set the current layer depending
            // on the type of relation that is permitted between the source/target span. This is
            // necessary because we have no separate UI control to set the relation annotation type.
            // It is possible because currently only a single relation layer is allowed to attach to
            // any given span layer.

            // If we drag an arc in a chain layer, then the arc is of the same layer as the span
            // Chain layers consist of arcs and spans
            if (originLayer.getType().equals(CHAIN_TYPE)) {
                // one layer both for the span and arc annotation
                state.setSelectedAnnotationLayer(originLayer);
            }
            // Otherwise, look up the possible relation layer(s) in the database.
            else {
                state.setSelectedAnnotationLayer(getRelationLayerFor(originLayer)
                        .orElseThrow(() -> new IllegalPlacementException(
                                "No relation annotation allowed on layer ["
                                        + state.getDefaultAnnotationLayer().getUiName() + "]")));
            }

            state.setDefaultAnnotationLayer(originLayer);

            // If we switched layers, we need to initialize the feature editors for the new layer
            if (!Objects.equals(previousLayer, state.getSelectedAnnotationLayer())) {
                LOG.trace("Layer changed from {} to {} - need to reload feature editors",
                        previousLayer, state.getSelectedAnnotationLayer());
                loadFeatureEditorModels(aTarget);
            }
        }
        else {
            // Re-set the selected layer from the drop-down since it might have changed if we
            // have previously created a relation annotation
            state.setSelectedAnnotationLayer(state.getDefaultAnnotationLayer());
        }

        internalCommitAnnotation(aTarget, aCas);

        internalCompleteAnnotation(aTarget, aCas);

        if (aTarget != null) {
            refresh(aTarget);
            // // After the annotation has been created, we need to re-render the button container
            // e.g.
            // // to make the buttons show up if previously no annotation was selected
            // aTarget.add(buttonContainer);
            // // Also update the features and selected annotation info
            // aTarget.add(selectedAnnotationInfoPanel, featureEditorListPanel, relationListPanel);
        }

        state.clearArmedSlot();
    }

    private Optional<AnnotationLayer> getRelationLayerFor(AnnotationLayer aSpanLayer)
    {
        for (AnnotationLayer l : annotationService.listAnnotationLayer(aSpanLayer.getProject())) {
            if (!RELATION_TYPE.equals(l.getType())) {
                continue;
            }

            if (aSpanLayer.equals(l.getAttachType())) {
                return Optional.of(l);
            }

            if (l.getAttachFeature() != null
                    && l.getAttachFeature().getType().equals(aSpanLayer.getName())) {
                return Optional.of(l);
            }
        }

        return Optional.empty();
    }

    public TextField<String> getForwardAnnotationTextField()
    {
        return forwardAnnotationTextField;
    }

    @Override
    public void actionCreateForward(AjaxRequestTarget aTarget, CAS aCas)
        throws IOException, AnnotationException
    {
        LOG.trace("actionCreateForward()");

        editorPage.ensureIsEditable();

        AnnotatorState state = getModelObject();

        // Re-set the selected layer from the drop-down since it might have changed if we
        // have previously created a relation annotation
        state.setSelectedAnnotationLayer(state.getDefaultAnnotationLayer());

        internalCommitAnnotation(aTarget, aCas);

        // Forward annotation mode requires that there is exactly a single feature, so we
        // can simply call `get(0)` here.
        FeatureState featureState = getModelObject().getFeatureStates().get(0);

        // If the annotation value was cleared or not filled in by the user, then we
        // remove the entire annotation.
        if (featureState.value == null) {
            TypeAdapter adapter = annotationService.getAdapter(state.getSelectedAnnotationLayer());
            AnnotationFS fs = selectAnnotationByAddr(aCas,
                    state.getSelection().getAnnotation().getId());
            deleteAnnotation(aCas, state, fs, featureState.feature.getLayer(), adapter);
        }

        // Move on to the next token
        Selection selection = state.getSelection();
        AnnotationFS nextToken = getNextToken(aCas, selection.getBegin(), selection.getEnd());
        if (nextToken != null) {
            state.getSelection().selectSpan(aCas, nextToken.getBegin(), nextToken.getEnd());

            // If the new annotation is outside the view window then move forward
            if (state.getWindowEndOffset() <= nextToken.getBegin()) {
                state.moveForward(aCas);
            }

            // Re-set the selected layer from the drop-down since it might have changed if we
            // have previously created a relation annotation
            state.setSelectedAnnotationLayer(state.getDefaultAnnotationLayer());

            // If there is already an annotation on the next token and if stacking mode is
            // disabled, then select that annotation and load the feature value of that annotation
            // into the {@link AnnotationFeatureForm#setSelectedTag(String) selected tag}.
            SpanAdapter adapter = (SpanAdapter) annotationService
                    .getAdapter(state.getDefaultAnnotationLayer());
            Type type = CasUtil.getType(aCas, adapter.getAnnotationTypeName());
            AnnotationFS annotation = selectAt(aCas, type, nextToken.getBegin(), nextToken.getEnd())
                    .stream().findFirst().orElse(null);

            // If there is no existing annotation of if stacking is allowed then we create a new one
            if (adapter.getLayer().isAllowStacking() || annotation == null) {
                internalCommitAnnotation(aTarget, aCas);
            }
            // ... if there is an existing annotation, then select it
            else {
                state.getSelection().selectSpan(annotation);

                // If the existing annotation has a feature value, then load it into the hidden
                // forward annotation text field which we use to cycle through tags
                Serializable featureValue = adapter.getFeatureValue(featureState.feature,
                        annotation);
                if (featureValue != null) {
                    Map<String, String> bindTags = buildKeySequenceToTagMap();
                    String newTag = bindTags.entrySet().stream()
                            .filter(e -> e.getValue().equals(featureValue)).map(Map.Entry::getKey)
                            .findFirst().orElse(null);
                    setForwardAnnotationKeySequence(newTag,
                            "hit existing annotation with feature value");
                }
                else {
                    setForwardAnnotationKeySequence(null,
                            "hit existing annotation without feature value");
                }
            }
        }

        LOG.trace("onAutoForward()");
        onAutoForward(aTarget);

        internalCompleteAnnotation(aTarget, aCas);

        refresh(aTarget);
    }

    /**
     * Persists the potentially modified CAS, remembers feature values, reloads the feature editors
     * using the latest info from the CAS, updates the sentence number and focus unit, performs
     * auto-scrolling.
     */
    protected void internalCompleteAnnotation(AjaxRequestTarget aTarget, CAS aCas)
        throws IOException, AnnotationException
    {
        AnnotatorState state = getModelObject();

        // Update progress information
        LOG.trace("actionAnnotate() updating progress information");
        int sentenceNumber = getSentenceNumber(aCas, state.getSelection().getBegin());
        state.setFocusUnitIndex(sentenceNumber);
        state.getDocument().setSentenceAccessed(sentenceNumber);

        // persist changes
        editorPage.writeEditorCas(aCas);

        // Remember the current feature values independently for spans and relations
        LOG.trace("actionAnnotate() remembering feature editor values");
        state.rememberFeatures();

        // Loading feature editor values from CAS
        loadFeatureEditorModels(aTarget);

        // onAnnotate callback
        onAnnotate(aTarget);

        autoScroll(aCas);

        getForwardAnnotationTextField().setModelObject(null);

        LOG.trace("onChange()");
        onChange(aTarget);

        // If we created a new annotation, then refresh the available annotation layers in the
        // detail panel.
        if (state.getSelection().getAnnotation().isNotSet()) {
            if (layerSelectionPanel.getSelectableLayers().isEmpty()) {
                state.setSelectedAnnotationLayer(new AnnotationLayer());
            }
            else if (state.getSelectedAnnotationLayer() == null) {
                if (state.getRememberedSpanLayer() == null) {
                    state.setSelectedAnnotationLayer(
                            layerSelectionPanel.getSelectableLayers().get(0));
                }
                else {
                    state.setSelectedAnnotationLayer(state.getRememberedSpanLayer());
                }
            }
        }
    }

    /**
     * Creates or updates an annotation using the information from the feature editors.
     */
    protected void internalCommitAnnotation(AjaxRequestTarget aTarget, CAS aCas)
        throws AnnotationException, IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getSelectedAnnotationLayer() == null) {
            error("No layer is selected. First select a layer.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (state.getSelectedAnnotationLayer().isReadonly()) {
            error("Layer is not editable.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        LOG.trace("actionAnnotate() selectedLayer: {}",
                state.getSelectedAnnotationLayer().getUiName());
        LOG.trace("actionAnnotate() defaultLayer: {}",
                state.getDefaultAnnotationLayer().getUiName());

        // internalCommitAnnotation is used to update an existing annotation as well as to create
        // a new one. In either case, the selectedAnnotationLayer indicates the layer type! Do not
        // use the defaultAnnotationLayer here as e.g. when creating relation annotations, it would
        // point to the span type to which the relation attaches, not to the relation type!
        TypeAdapter adapter = annotationService.getAdapter(state.getSelectedAnnotationLayer());

        // If no annotation is selected, then we assume this is an annotation creation action,
        // create the annotation
        if (state.getSelection().getAnnotation().isNotSet()) {
            // Load the feature editors with the remembered values (if any)
            loadFeatureEditorModels(aTarget);
            createNewAnnotation(aTarget, adapter, aCas);
        }

        // Update the features of the selected annotation from the values presently in the
        // feature editors
        List<FeatureState> featureStates = state.getFeatureStates();
        commitFeatureStatesToFeatureStructure(aTarget, state.getDocument(),
                state.getUser().getUsername(), aCas, state.getSelection().getAnnotation().getId(),
                adapter, featureStates);
    }

    /**
     * Commits the values from the given feature states into the annotation with the given target FS
     * address in the given target CAS using the provided type adapter.
     */
    private void commitFeatureStatesToFeatureStructure(AjaxRequestTarget aTarget,
            SourceDocument aDocment, String aUsername, CAS aTargetCas, int aTargetFsAddr,
            TypeAdapter aAdapter, List<FeatureState> aFeatureStates)
    {
        // List<AnnotationFeature> features = new ArrayList<>();
        for (FeatureState featureState : aFeatureStates) {
            try {
                // features.add(featureState.feature);

                LOG.trace("Committing feature states to CAS: {} = {}",
                        featureState.feature.getUiName(), featureState.value);
                aAdapter.setFeatureValue(aDocment, aUsername, aTargetCas, aTargetFsAddr,
                        featureState.feature, featureState.value);
            }
            catch (Exception e) {
                error("Cannot set feature [" + featureState.feature.getUiName() + "]: "
                        + e.getMessage());
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }

        // Save bandwidth by not sending trivial success messages
        // String label = TypeUtil.getUiLabelText(aAdapter, selectFsByAddr(aTargetCas,
        // aTargetFsAddr),
        // features);
        // info(generateMessage(aAdapter.getLayer(), label, false));
    }

    private AttachStatus checkAttachStatus(AjaxRequestTarget aTarget, Project aProject,
            AnnotationFS aFS)
    {
        AnnotationLayer layer = annotationService.findLayer(aProject, aFS);

        AttachStatus attachStatus = new AttachStatus();

        List<AttachedAnnotation> attachedRels = annotationService.getAttachedRels(layer, aFS);
        boolean attachedToReadOnlyRels = attachedRels.stream()
                .anyMatch(rel -> rel.getLayer().isReadonly());
        if (attachedToReadOnlyRels) {
            attachStatus.readOnlyAttached |= true;
        }
        attachStatus.attachCount += attachedRels.size();

        // We do not count these atm since they only exist for built-in layers and are not
        // visible in the UI for the user.
        // Set<AnnotationFS> attachedSpans = getAttachedSpans(aFS, layer);
        // boolean attachedToReadOnlySpans = attachedSpans.stream().anyMatch(relFS -> {
        // AnnotationLayer relLayer = annotationService.getLayer(aProject, relFS);
        // return relLayer.isReadonly();
        // });
        // if (attachedToReadOnlySpans) {
        // attachStatus.readOnlyAttached |= true;
        // }
        // attachStatus.attachCount += attachedSpans.size();

        List<AttachedAnnotation> attachedLinks = annotationService.getAttachedLinks(layer, aFS);
        boolean attachedToReadOnlyLinks = attachedLinks.stream()
                .anyMatch(rel -> rel.getLayer().isReadonly());
        if (attachedToReadOnlyLinks) {
            attachStatus.readOnlyAttached |= true;
        }
        attachStatus.attachCount += attachedLinks.size();

        return attachStatus;
    }

    @Override
    public void actionDelete(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        CAS cas = getEditorCas();

        AnnotatorState state = getModelObject();

        int addr = state.getSelection().getAnnotation().getId();
        AnnotationFS fs = selectAnnotationByAddr(cas, addr);
        AnnotationLayer layer = annotationService.findLayer(state.getProject(), fs);
        TypeAdapter adapter = annotationService.getAdapter(layer);

        if (layer.isReadonly()) {
            error("Cannot delete an annotation on a read-only layer.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        AttachStatus attachStatus = checkAttachStatus(aTarget, state.getProject(), fs);
        if (attachStatus.readOnlyAttached) {
            error("Cannot delete an annotation to which annotations on read-only layers attach.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (adapter instanceof SpanAdapter && attachStatus.attachCount > 0) {
            deleteAnnotationDialog.setContentModel(
                    new StringResourceModel("DeleteDialog.text", this, Model.of(layer))
                            .setParameters(attachStatus.attachCount));
            deleteAnnotationDialog.setConfirmAction(_target -> doDelete(_target, layer, addr));
            deleteAnnotationDialog.show(aTarget);
            return;
        }

        doDelete(aTarget, layer, addr);
    }

    private void doDelete(AjaxRequestTarget aTarget, AnnotationLayer layer, int aAddr)
        throws IOException, AnnotationException
    {
        CAS cas = getEditorCas();
        AnnotatorState state = getModelObject();
        AnnotationFS fs = selectAnnotationByAddr(cas, aAddr);
        TypeAdapter adapter = annotationService.getAdapter(layer);

        deleteAnnotation(cas, state, fs, layer, adapter);

        // Store CAS again
        editorPage.writeEditorCas(cas);

        // Update progress information
        int sentenceNumber = getSentenceNumber(cas, state.getSelection().getBegin());
        state.setFocusUnitIndex(sentenceNumber);
        state.getDocument().setSentenceAccessed(sentenceNumber);

        autoScroll(cas);

        state.rememberFeatures();

        reset(aTarget);

        onChange(aTarget);
        onDelete(aTarget, fs);
    }

    private void deleteAnnotation(CAS aCas, AnnotatorState state, AnnotationFS fs,
            AnnotationLayer layer, TypeAdapter adapter)
    {
        RequestCycle.get().find(AjaxRequestTarget.class)
                .ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));

        // == DELETE ATTACHED RELATIONS ==
        // If the deleted FS is a span, we must delete all relations that
        // point to it directly or indirectly via the attachFeature.
        if (adapter instanceof SpanAdapter) {
            for (AttachedAnnotation rel : annotationService.getAttachedRels(layer, fs)) {
                RelationAdapter relationAdapter = (RelationAdapter) annotationService
                        .findAdapter(state.getProject(), rel.getRelation());

                relationAdapter.delete(state.getDocument(), state.getUser().getUsername(), aCas,
                        new VID(rel.getRelation()));

                info(generateMessage(relationAdapter.getLayer(), null, true));
            }
        }

        // == DELETE ATTACHED SPANS ==
        // This case is currently not implemented because WebAnno currently does not allow to
        // create spans that attach to other spans. The only span type for which this is relevant
        // is the Token type which cannot be deleted.

        // == CLEAN UP LINK FEATURES ==
        // If the deleted FS is a span that is the target of a link feature, we must unset that
        // link and delete the slot if it is a multi-valued link. Here, we have to scan all
        // annotations from layers that have link features that could point to the FS
        // to be deleted: the link feature must be the type of the FS or it must be generic.
        if (adapter instanceof SpanAdapter) {
            for (AnnotationFeature linkFeature : annotationService
                    .listAttachedLinkFeatures(layer)) {
                Type linkHostType = CasUtil.getType(aCas, linkFeature.getLayer().getName());

                for (FeatureStructure linkHostFS : CasUtil.selectFS(aCas, linkHostType)) {
                    List<LinkWithRoleModel> links = adapter.getFeatureValue(linkFeature,
                            linkHostFS);
                    Iterator<LinkWithRoleModel> i = links.iterator();
                    boolean modified = false;
                    while (i.hasNext()) {
                        LinkWithRoleModel link = i.next();
                        if (link.targetAddr == getAddr(fs)) {
                            i.remove();
                            info("Cleared slot [" + link.role + "] in feature ["
                                    + linkFeature.getUiName() + "] on ["
                                    + linkFeature.getLayer().getUiName() + "]");
                            LOG.debug("Cleared slot [" + link.role + "] in feature ["
                                    + linkFeature.getName() + "] on annotation ["
                                    + getAddr(linkHostFS) + "]");
                            modified = true;
                        }
                    }
                    if (modified) {
                        setFeature(linkHostFS, linkFeature, links);

                        // If the currently armed slot is part of this link, then we disarm the slot
                        // to avoid the armed slot no longer pointing at the index which the user
                        // had selected it to point at.
                        FeatureState armedFeature = state.getArmedFeature();
                        if (armedFeature != null
                                && WebAnnoCasUtil.getAddr(linkHostFS) == armedFeature.vid.getId()
                                && armedFeature.feature.equals(linkFeature)) {
                            state.clearArmedSlot();
                        }
                    }
                }
            }
        }

        // If the deleted FS is a relation, we don't have to do anything. Nothing can point to a
        // relation.
        if (adapter instanceof RelationAdapter) {
            // Do nothing ;)
        }

        // Actually delete annotation
        adapter.delete(state.getDocument(), state.getUser().getUsername(), aCas,
                state.getSelection().getAnnotation());

        info(generateMessage(state.getSelectedAnnotationLayer(), null, true));
    }

    @Override
    public void actionReverse(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        aTarget.addChildren(getPage(), IFeedback.class);

        CAS cas = getEditorCas();

        AnnotatorState state = getModelObject();

        AnnotationFS idFs = selectAnnotationByAddr(cas,
                state.getSelection().getAnnotation().getId());

        cas.removeFsFromIndexes(idFs);

        AnnotationFS originFs = selectAnnotationByAddr(cas, state.getSelection().getOrigin());
        AnnotationFS targetFs = selectAnnotationByAddr(cas, state.getSelection().getTarget());

        List<FeatureState> featureStates = getModelObject().getFeatureStates();

        TypeAdapter adapter = annotationService.getAdapter(state.getSelectedAnnotationLayer());
        if (adapter instanceof RelationAdapter) {
            // If no features, still create arc #256
            AnnotationFS arc = ((RelationAdapter) adapter).add(state.getDocument(),
                    state.getUser().getUsername(), targetFs, originFs, cas);
            state.getSelection().setAnnotation(new VID(getAddr(arc)));

            for (FeatureState featureState : featureStates) {
                adapter.setFeatureValue(state.getDocument(), state.getUser().getUsername(), cas,
                        getAddr(arc), featureState.feature, featureState.value);
            }
        }
        else {
            error("chains cannot be reversed");
            return;
        }

        // persist changes
        editorPage.writeEditorCas(cas);
        int sentenceNumber = getSentenceNumber(cas, originFs.getBegin());
        state.setFocusUnitIndex(sentenceNumber);
        state.getDocument().setSentenceAccessed(sentenceNumber);

        autoScroll(cas);

        state.rememberFeatures();

        // in case the user re-reverse it
        state.getSelection().reverseArc();

        onChange(aTarget);
    }

    @Override
    public void actionClear(AjaxRequestTarget aTarget) throws AnnotationException
    {
        reset(aTarget);
        aTarget.addChildren(getPage(), IFeedback.class);
        onChange(aTarget);
    }

    /**
     * Scroll the window of visible annotations if auto-scrolling is enabled.
     */
    private void autoScroll(CAS aCas)
    {
        AnnotatorState state = getModelObject();
        // Perform auto-scroll if it is enabled
        if (state.getPreferences().isScrollPage()) {
            getModelObject().moveToSelection(aCas);
        }
    }

    /**
     * Loads the feature states either from the CAS (if an annotation is selected) or from the
     * remembered values (if no annotation is selected).
     */
    private void loadFeatureEditorModels(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        LOG.trace("loadFeatureEditorModels()");

        CAS aCas = getEditorCas();

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();

        List<FeatureState> featureStates = state.getFeatureStates();
        for (FeatureState featureState : featureStates) {
            if (StringUtils.isNotBlank(featureState.feature.getLinkTypeName())) {
                featureState.value = new ArrayList<>();
            }
        }

        try {
            // If we reset the layers while doing a relation, we won't be able to complete the
            // relation - so in this case, we leave the layers alone...
            if (!selection.isArc()) {
                layerSelectionPanel.refreshSelectableLayers();
            }

            if (selection.getAnnotation().isSet()) {
                // If an existing annotation was selected, take the feature editor model values from
                // there
                AnnotationFS annoFs = selectAnnotationByAddr(aCas,
                        state.getSelection().getAnnotation().getId());

                // Try obtaining the layer from the feature structure
                AnnotationLayer layer;
                try {
                    layer = annotationService.findLayer(state.getProject(), annoFs);
                    state.setSelectedAnnotationLayer(layer);
                    LOG.trace("loadFeatureEditorModels() selectedLayer set from selection: {}",
                            state.getSelectedAnnotationLayer().getUiName());
                }
                catch (NoResultException e) {
                    reset(aTarget);
                    throw new IllegalStateException(
                            "Unknown layer [" + annoFs.getType().getName() + "]", e);
                }

                // If remember layer is off, then the current layer follows the selected annotations
                // This is only relevant for span annotations because we only have these in the
                // dropdown - relation annotations are automatically determined based on the
                // selected span annotation
                if (!selection.isArc() && !state.getPreferences().isRememberLayer()) {
                    state.setSelectedAnnotationLayer(layer);
                }

                loadFeatureEditorModelsCommon(aTarget, aCas, layer, annoFs, null);
            }
            else {
                // If a new annotation is being created, populate the feature editors from the
                // remembered values (if any)

                if (selection.isArc()) {
                    // Avoid creation of arcs on locked layers
                    if (state.getSelectedAnnotationLayer() != null
                            && state.getSelectedAnnotationLayer().isReadonly()) {
                        state.setSelectedAnnotationLayer(new AnnotationLayer());
                    }
                    else {
                        loadFeatureEditorModelsCommon(aTarget, aCas,
                                state.getSelectedAnnotationLayer(), null,
                                state.getRememberedArcFeatures());
                    }
                }
                else {
                    loadFeatureEditorModelsCommon(aTarget, aCas, state.getSelectedAnnotationLayer(),
                            null, state.getRememberedSpanFeatures());
                }
            }

            updateRememberLayer();
        }
        catch (Exception e) {
            throw new AnnotationException(e);
        }
    }

    private void loadFeatureEditorModelsCommon(AjaxRequestTarget aTarget, CAS aCas,
            AnnotationLayer aLayer, FeatureStructure aFS,
            Map<AnnotationFeature, Serializable> aRemembered)
    {
        getModelObject().getFeatureStates().clear();

        AnnotatorState state = AnnotationDetailEditorPanel.this.getModelObject();

        // Populate from feature structure
        for (AnnotationFeature feature : annotationService.listSupportedFeatures(aLayer)) {
            if (!feature.isEnabled()) {
                continue;
            }

            if (aFS != null && aFS.getType().getFeatureByBaseName(feature.getName()) == null) {
                // If the feature does not exist in the given Feature Structure,
                // then the typesystem might be out of date
                error("The annotation typesystem might be out of date, "
                        + "try re-opening the document!");
                LOG.error(String.format("Unable to find %s in the current cas typesystem",
                        feature.getName()));
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            Serializable value = null;
            VID vid = null;
            if (aFS != null) {
                value = annotationService.getAdapter(aLayer).getFeatureValue(feature, aFS);
                vid = new VID(aFS);
            }
            else if (aRemembered != null) {
                value = aRemembered.get(feature);
            }

            FeatureState featureState = null;
            if (CHAIN_TYPE.equals(feature.getLayer().getType())) {
                if (state.getSelection().isArc()) {
                    if (feature.getLayer().isLinkedListBehavior()
                            && COREFERENCE_RELATION_FEATURE.equals(feature.getName())) {
                        featureState = new FeatureState(vid, feature, value);
                    }
                }
                else if (COREFERENCE_TYPE_FEATURE.equals(feature.getName())) {
                    featureState = new FeatureState(vid, feature, value);
                }
            }
            else {
                featureState = new FeatureState(vid, feature, value);
            }

            if (featureState != null) {
                state.getFeatureStates().add(featureState);

                // Populate tagsets if necessary
                if (featureState.feature.getTagset() != null) {
                    // verification to check whether constraints exist for this project or NOT
                    if (state.getConstraints() != null
                            && state.getSelection().getAnnotation().isSet()) {
                        // indicator.setRulesExist(true);
                        populateTagsBasedOnRules(aCas, featureState);
                    }
                    else {
                        // indicator.setRulesExist(false);
                        featureState.tagset = annotationService
                                .listTagsReorderable(featureState.feature.getTagset());
                    }
                }
            }
        }
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        // Only show sidebar if a document is selected
        setVisible(getModelObject() != null && getModelObject().getDocument() != null);

        // Set read only if annotation is finished or the user is viewing other's work
        setEnabled(editorPage.isEditable());
    }

    /**
     * Re-render the sidebar if the selection has changed.
     */
    @OnEvent
    public void onSelectionChangedEvent(SelectionChangedEvent aEvent)
    {
        if (aEvent.getRequestHandler() != null) {
            refresh(aEvent.getRequestHandler());
            // aEvent.getRequestHandler().add(selectedAnnotationInfoPanel, featureEditorListPanel,
            // relationListPanel);
        }
    }

    /**
     * @deprecated Use event listeners for {@link SelectionChangedEvent}, {@link AnnotationEvent},
     *             {@link FeatureValueUpdatedEvent} and/or {@link AnnotatorViewportChangedEvent}
     *             instead.
     */
    @Deprecated
    protected void onChange(AjaxRequestTarget aTarget)
    {
        // Overriden in CurationPanel
    }

    protected void onAutoForward(AjaxRequestTarget aTarget)
    {
        // Overriden in CurationPanel
    }

    /**
     * This is used only on the AutomationPage.
     * 
     * @deprecated A reasonable replacement for this should be an event listener for
     *             {@link AnnotationCreatedEvent}.
     */
    @Deprecated
    protected void onAnnotate(AjaxRequestTarget aTarget)
    {
        // Overriden in AutomationPage
    }

    protected void onDelete(AjaxRequestTarget aTarget, AnnotationFS aFs)
    {
        // Overriden in AutomationPage
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    /**
     * Adds and sorts tags based on Constraints rules
     */
    private void populateTagsBasedOnRules(CAS aCas, FeatureState aModel)
    {
        AnnotatorState state = getModelObject();

        // Add values from rules
        String restrictionFeaturePath;
        switch (aModel.feature.getLinkMode()) {
        case WITH_ROLE:
            restrictionFeaturePath = aModel.feature.getName() + "."
                    + aModel.feature.getLinkTypeRoleFeatureName();
            break;
        case NONE:
            restrictionFeaturePath = aModel.feature.getName();
            break;
        default:
            throw new IllegalArgumentException(
                    "Unsupported link mode [" + aModel.feature.getLinkMode() + "] on feature ["
                            + aModel.feature.getName() + "]");
        }

        aModel.indicator.reset();

        // Fetch possible values from the constraint rules
        List<PossibleValue> possibleValues;
        try {
            FeatureStructure featureStructure = selectFsByAddr(aCas,
                    state.getSelection().getAnnotation().getId());

            Evaluator evaluator = new ValuesGenerator();
            // Only show indicator if this feature can be affected by Constraint rules!
            aModel.indicator.setAffected(evaluator.isThisAffectedByConstraintRules(featureStructure,
                    restrictionFeaturePath, state.getConstraints()));

            possibleValues = evaluator.generatePossibleValues(featureStructure,
                    restrictionFeaturePath, state.getConstraints());

            LOG.debug("Possible values for [" + featureStructure.getType().getName() + "] ["
                    + restrictionFeaturePath + "]: " + possibleValues);
        }
        catch (Exception e) {
            error("Unable to evaluate constraints: " + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Unable to evaluate constraints: " + e.getMessage(), e);
            possibleValues = new ArrayList<>();
        }

        // Fetch actual tagset
        List<ReorderableTag> tags = annotationService
                .listTagsReorderable(aModel.feature.getTagset());

        // First add tags which are suggested by rules and exist in tagset
        List<ReorderableTag> tagset = compareSortAndAdd(possibleValues, tags, aModel.indicator);

        // Record the possible values and the (re-ordered) tagset in the feature state
        aModel.possibleValues = possibleValues;
        aModel.tagset = tagset;
    }

    /*
     * Compares existing tagset with possible values resulted from rule evaluation Adds only which
     * exist in tagset and is suggested by rules. The remaining values from tagset are added
     * afterwards.
     */
    private static List<ReorderableTag> compareSortAndAdd(List<PossibleValue> aPossibleValues,
            List<ReorderableTag> aTags, RulesIndicator aRulesIndicator)
    {
        List<ReorderableTag> returnList = new ArrayList<>();

        // if no possible values, means didn't satisfy conditions
        if (aPossibleValues.isEmpty()) {
            aRulesIndicator.didntMatchAnyRule();
            return aTags;
        }

        Map<String, ReorderableTag> tagIndex = new LinkedHashMap<>();
        for (ReorderableTag tag : aTags) {
            tagIndex.put(tag.getName(), tag);
        }

        for (PossibleValue value : aPossibleValues) {
            ReorderableTag tag = tagIndex.get(value.getValue());
            if (tag == null) {
                continue;
            }

            // Matching values found in tagset and shown in dropdown
            aRulesIndicator.rulesApplied();
            // HACK BEGIN
            tag.setReordered(true);
            // HACK END
            returnList.add(tag);
            // Avoid duplicate entries
            tagIndex.remove(value.getValue());
        }

        // If no matching tags found
        if (returnList.isEmpty()) {
            aRulesIndicator.didntMatchAnyTag();
        }

        // Add all remaining non-matching tags to the list
        returnList.addAll(tagIndex.values());

        return returnList;
    }

    /**
     * Clears the selection in the {@link AnnotatorState} and clears the feature editors. Also
     * refreshes the selectable layers dropdown.
     * 
     * @param aTarget
     *            (optional) current AJAX target
     */
    public void reset(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();

        // Clear selection and feature states
        state.getFeatureStates().clear();
        state.getSelection().clear();

        // Refresh the selectable layers dropdown
        layerSelectionPanel.refreshSelectableLayers();
        if (aTarget != null) {
            aTarget.add(layerSelectionPanel);
        }
    }

    private static Set<AnnotationFS> getAttachedSpans(AnnotationSchemaService aAS, AnnotationFS aFs,
            AnnotationLayer aLayer)
    {
        CAS cas = aFs.getCAS();
        Set<AnnotationFS> attachedSpans = new HashSet<>();
        TypeAdapter adapter = aAS.getAdapter(aLayer);
        if (adapter instanceof SpanAdapter && aLayer.getAttachType() != null) {
            Type spanType = CasUtil.getType(cas, aLayer.getAttachType().getName());
            Feature attachFeature = spanType
                    .getFeatureByBaseName(aLayer.getAttachFeature().getName());
            final Type type = spanType;

            for (AnnotationFS attachedFs : selectAt(cas, type, aFs.getBegin(), aFs.getEnd())) {
                if (isSame(attachedFs.getFeatureValue(attachFeature), aFs)) {
                    attachedSpans.add(attachedFs);
                }
            }
        }
        return attachedSpans;
    }

    protected static void handleException(Component aComponent, AjaxRequestTarget aTarget,
            Exception aException)
    {
        if (aTarget != null) {
            aTarget.addChildren(aComponent.getPage(), IFeedback.class);
        }

        try {
            throw aException;
        }
        catch (AnnotationException e) {
            aComponent.error("Error: " + e.getMessage());
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

    private static String generateMessage(AnnotationLayer aLayer, String aLabel, boolean aDeleted)
    {
        String action = aDeleted ? "deleted" : "created/updated";

        String msg = "The [" + aLayer.getUiName() + "] annotation has been " + action + ".";
        if (StringUtils.isNotBlank(aLabel)) {
            msg += " Label: [" + aLabel + "]";
        }
        return msg;
    }

    private LambdaAjaxLink createClearButton()
    {
        LambdaAjaxLink link = new LambdaAjaxLink("clear", this::actionClear);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()
                && editorPage.isEditable()));
        return link;
    }

    private Component createReverseButton()
    {
        LambdaAjaxLink link = new LambdaAjaxLink("reverse", this::actionReverse);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(LambdaBehavior.onConfigure(_this -> {
            AnnotatorState state = getModelObject();

            _this.setVisible(
                    state.getSelection().getAnnotation().isSet() && state.getSelection().isArc()
                            && RELATION_TYPE.equals(state.getSelectedAnnotationLayer().getType())
                            && editorPage.isEditable());

            // Avoid reversing in read-only layers
            _this.setEnabled(state.getSelectedAnnotationLayer() != null
                    && !state.getSelectedAnnotationLayer().isReadonly());
        }));
        return link;
    }

    private LambdaAjaxLink createDeleteButton()
    {
        LambdaAjaxLink link = new LambdaAjaxLink("delete", this::actionDelete);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()
                && editorPage.isEditable()));
        // Avoid deleting in read-only layers
        link.add(enabledWhen(() -> getModelObject().getSelectedAnnotationLayer() != null
                && !getModelObject().getSelectedAnnotationLayer().isReadonly()));
        return link;
    }

    private void actionReplace(AjaxRequestTarget aTarget) throws IOException
    {
        AnnotatorState state = getModelObject();

        AnnotationLayer newLayer = state.getDefaultAnnotationLayer();

        CAS cas = getEditorCas();
        AnnotationFS fs = selectAnnotationByAddr(cas, state.getSelection().getAnnotation().getId());
        AnnotationLayer currentLayer = annotationService.findLayer(state.getProject(), fs);

        if (currentLayer.isReadonly()) {
            error("Cannot replace an annotation on a read-only layer.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        AttachStatus attachStatus = checkAttachStatus(aTarget, state.getProject(), fs);
        if (attachStatus.readOnlyAttached) {
            error("Cannot replace an annotation to which annotations on read-only layers attach.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        replaceAnnotationDialog
                .setContentModel(new StringResourceModel("ReplaceDialog.text", this).setParameters(
                        currentLayer.getUiName(), newLayer.getUiName(), attachStatus.attachCount));
        replaceAnnotationDialog.setConfirmAction((_target) -> {
            // The delete action clears the selection, but we need it to create
            // the new annotation - so we save it.
            Selection savedSel = getModelObject().getSelection().copy();

            // Delete current annotation
            actionDelete(_target);

            // Set up the action to create the replacement annotation
            AnnotationLayer layer = state.getDefaultAnnotationLayer();
            state.getSelection().set(savedSel);
            state.getSelection().setAnnotation(VID.NONE_ID);
            state.setSelectedAnnotationLayer(layer);
            state.setDefaultAnnotationLayer(layer);
            loadFeatureEditorModels(_target);

            // Create the replacement annotation
            actionCreateOrUpdate(_target, getEditorCas());
            refresh(_target);
        });
        replaceAnnotationDialog.setCancelAction((_target) -> {
            state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
            refresh(_target);
        });
        replaceAnnotationDialog.show(aTarget);
    }

    private void updateRememberLayer()
    {
        AnnotatorState state = getModelObject();
        if (state.getPreferences().isRememberLayer()) {
            if (state.getDefaultAnnotationLayer() == null) {
                state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
            }
        }
        else if (!state.getSelection().isArc()) {
            state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
        }
    }

    public FeatureEditorListPanel getFeatureEditorListPanel()
    {
        return featureEditorListPanel;
    }

    public void refresh(AjaxRequestTarget aTarget)
    {
        aTarget.add(layerSelectionPanel, buttonContainer, selectedAnnotationInfoPanel,
                featureEditorListPanel, relationListPanel);
    }

    @OnEvent(stop = true)
    public void onLinkFeatureDeletedEvent(LinkFeatureDeletedEvent aEvent)
    {
        AjaxRequestTarget target = aEvent.getTarget();
        // Auto-commit if working on existing annotation
        if (getModelObject().getSelection().getAnnotation().isSet()) {
            try {
                actionCreateOrUpdate(target, getEditorCas());
            }
            catch (Exception e) {
                handleException(this, target, e);
            }
        }
    }

    @OnEvent
    public void onDefaultLayerChangedEvent(DefaultLayerChangedEvent aEvent)
    {
        AnnotatorState state = getModelObject();

        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class).get();

        // If "remember layer" is set, the we really just update the selected layer...
        // we do not touch the selected annotation not the annotation detail panel
        if (!state.getPreferences().isRememberLayer()) {

            // If "remember layer" is not set, then changing the layer means that we
            // want to change the type of the currently selected annotation
            if (!Objects.equals(state.getSelectedAnnotationLayer(),
                    state.getDefaultAnnotationLayer())
                    && state.getSelection().getAnnotation().isSet()) {
                try {
                    if (state.getSelection().isArc()) {
                        actionClear(target);
                    }
                    else {
                        actionReplace(target);
                    }
                }
                catch (Exception e) {
                    handleException(this, target, e);
                }
            }
            // If no annotation is selected, then prime the annotation detail panel for the new type
            else {
                state.setSelectedAnnotationLayer(state.getDefaultAnnotationLayer());
                reset(target);
            }
        }
    }

    private static class AttachStatus
    {
        boolean readOnlyAttached;
        int attachCount;
    }
}
