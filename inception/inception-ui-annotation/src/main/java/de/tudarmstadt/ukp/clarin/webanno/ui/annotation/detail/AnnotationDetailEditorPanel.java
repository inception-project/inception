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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorManagerPrefs.KEY_ANNOTATION_EDITOR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnchoringModePrefs.KEY_ANCHORING_MODE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenModelIsNotNull;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.isSame;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.selectAt;
import static wicket.contrib.input.events.EventType.click;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyBindingsProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyBindingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.events.BulkAnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.diam.editing.AnnotationEditingService;
import de.tudarmstadt.ukp.inception.diam.editing.PartialDeleteException;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.editorstate.EditorBoundEvent;
import de.tudarmstadt.ukp.inception.rendering.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.inception.rendering.selection.ActiveEditorChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.DocumentStateChangedInEditorEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.EditorContentReplacedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.selection.SelectionChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import jakarta.persistence.NoResultException;

/**
 * Annotation Detail Editor Panel.
 */
public class AnnotationDetailEditorPanel
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = 7324241992353693848L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean KeyBindingsProperties keyBindings;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaProperties schemaProperties;
    private @SpringBean UserDao userService;
    private @SpringBean PreferencesService preferencesService;
    private @SpringBean AnnotationEditingService annotationEditingService;
    private @SpringBean DocumentService documentService;

    // Top-level containers
    private final LayerSelectionPanel layerSelectionPanel;
    private final FeatureEditorListPanel featureEditorListPanel;
    private final WebMarkupContainer buttonContainer;
    private final WebMarkupContainer navContainer;
    private final AttachedAnnotationListPanel relationListPanel;

    // Components
    private final BootstrapModalDialog confirmationDialog;
    private final DocumentEditorManager editorPage;
    private final DetailStateModel stateModel;

    public AnnotationDetailEditorPanel(String id, DocumentEditorManager aManager)
    {
        this(id, aManager, new DetailStateModel(aManager));
    }

    private AnnotationDetailEditorPanel(String id, DocumentEditorManager aManager,
            DetailStateModel aStateModel)
    {
        super(id, new CompoundPropertyModel<>(aStateModel));

        editorPage = aManager;
        stateModel = aStateModel;

        setOutputMarkupPlaceholderTag(true);
        setMarkupId("annotationDetailEditorPanel");
        add(visibleWhenModelIsNotNull(this));

        queue(new WebMarkupContainer("header")
                .add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet())));
        queue(new Label("layerType", getModel().map(m -> m.getSelectedAnnotationLayer())
                .map(l -> l.getType()).map(t -> getString(t))).setOutputMarkupId(true));

        var floatingInfo = new WebMarkupContainer("floatingInfo");
        floatingInfo.add(visibleWhen(this::isFloating));
        queue(floatingInfo);
        queue(new Label("floatingOwner", new StringResourceModel("floatingOwner")
                .setParameters(LoadableDetachableModel.of(this::floatingOwnerName))));

        confirmationDialog = new BootstrapModalDialog("deleteAnnotationDialog");
        confirmationDialog.trapFocus();
        queue(confirmationDialog);

        // The default layer belongs to the active editor, so the layer selection stays bound to it
        // while the panel shows an annotation read-only
        IModel<AnnotatorState> activeEditorStateModel = this::activeEditorState;
        queue(layerSelectionPanel = new LayerSelectionPanel("layerContainer",
                activeEditorStateModel));
        layerSelectionPanel.add(visibleWhen(this::hasActiveEditor));
        queue(new AnnotationInfoPanel("infoContainer", getModel(), this));
        queue(featureEditorListPanel = new FeatureEditorListPanel("featureEditorListPanel",
                getModel(), this));
        featureEditorListPanel.add(enabledWhen(this::isSelectedAnnotationEditable));
        queue(relationListPanel = new AttachedAnnotationListPanel("relationListContainer",
                getModel()));
        relationListPanel.setOutputMarkupPlaceholderTag(true);

        buttonContainer = new WebMarkupContainer("buttonContainer");
        buttonContainer.add(enabledWhen(this::isSelectedAnnotationEditable));
        buttonContainer.setOutputMarkupPlaceholderTag(true);
        queue(createDeleteButton());
        queue(createReverseButton());
        queue(createClearButton());
        queue(buttonContainer);

        navContainer = new WebMarkupContainer("navContainer");
        // Navigating would select the next annotation in the active editor, using addresses from
        // the floating annotation's CAS
        navContainer.add(visibleWhen(
                () -> getModelObject().getSelection().getAnnotation().isSet() && !isFloating()));
        navContainer.setOutputMarkupPlaceholderTag(true);
        queue(createNextAnnotationButton());
        queue(createPreviousAnnotationButton());
        queue(navContainer);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(
                JavaScriptHeaderItem.forReference(AnnotationDetailEditorJSResourceReference.get()));
    }

    private LambdaAjaxLink createNextAnnotationButton()
    {
        var link = new LambdaAjaxLink("nextAnnotation", this::actionNextAnnotation);
        link.add(keyBindings.getNavigation().getNextAnnotation().toInputBehavior(click));
        link.add(
                AttributeModifier
                        .append("title",
                                () -> " ("
                                        + KeyBindingsUtil.formatShortcut(
                                                keyBindings.getNavigation().getNextAnnotation())
                                        + ")"));
        return link;
    }

    private LambdaAjaxLink createPreviousAnnotationButton()
    {
        var link = new LambdaAjaxLink("previousAnnotation", this::actionPreviousAnnotation);
        link.add(keyBindings.getNavigation().getPreviousAnnotation().toInputBehavior(click));
        link.add(
                AttributeModifier.append("title",
                        () -> " ("
                                + KeyBindingsUtil.formatShortcut(
                                        keyBindings.getNavigation().getPreviousAnnotation())
                                + ")"));
        return link;
    }

    private void actionNextAnnotation(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        var sel = getModelObject().getSelection();
        if (!sel.isSet()) {
            return;
        }

        var cas = detailCas();
        var cur = selectByAddr(cas, AnnotationFS.class, sel.getAnnotation().getId());
        var next = WebAnnoCasUtil.getNext(cur);

        if (next != null) {
            actionSelectAndJump(aTarget, next);
        }
        else {
            info("There is no next annotation");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private void actionPreviousAnnotation(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        var sel = getModelObject().getSelection();
        if (!sel.isSet()) {
            return;
        }

        var cas = detailCas();
        var cur = selectByAddr(cas, AnnotationFS.class, sel.getAnnotation().getId());
        var prev = WebAnnoCasUtil.getPrev(cur);

        if (prev != null) {
            actionSelectAndJump(aTarget, prev);
        }
        else {
            info("There is no previous annotation");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    public void actionFillSlot(AjaxRequestTarget aTarget, int aSlotFillerBegin, int aSlotFillerEnd)
        throws AnnotationException, IOException
    {
        var state = getModelObject();

        ensureDetailIsEditable();

        var slotFillerVid = annotationEditingService.createSlotFiller(state.getDocument(),
                state.getUser().getUsername(), detailCas(), state, aSlotFillerBegin,
                aSlotFillerEnd);

        actionFillSlot(aTarget, slotFillerVid);
    }

    public void actionFillSlot(AjaxRequestTarget aTarget, VID aExistingSlotFillerId)
        throws AnnotationException, IOException
    {
        var state = getModelObject();

        // Guard the CAS we are actually going to write to (see writeDetailCas)
        ensureDetailIsEditable();

        var slotFillerAddr = aExistingSlotFillerId.getId();

        var cas = detailCas();

        // Remember which slot was armed - filling it does not clear it, but the branch below
        // needs to know whether the slot host is the annotation currently open in this panel.
        var armedFeature = state.getArmedFeature();

        reportMessages(aTarget, annotationEditingService.fillSlot(state.getDocument(),
                state.getUser().getUsername(), cas, state, slotFillerAddr));

        // NOTE: we do NOT delegate to internalCommitAnnotation here because most of what it does
        // is not required for slot filling and because slot filling requires special treatment -
        // hence the necessary code is in the service. However, we DO delegate to
        // internalCompleteAnnotation to save the CAS after the annotation.

        internalCompleteAnnotation(aTarget);

        // If the armed slot is located in the annotation detail editor panel (right side) update
        // the annotator state with the changes that we made to the CAS
        if (state.getSelection().getAnnotation().equals(armedFeature.vid)) {
            // Loading feature editor values from CAS
            loadFeatureEditorModels(aTarget);
        }
        // ... if the SLOT HOST annotation is NOT open in the detail panel on the right, then
        // select SLOT FILLER an open it there
        else {
            state.setSelection(Selection.span(selectAnnotationByAddr(cas, slotFillerAddr)));
            actionLoadSelectionDetails(aTarget);
        }

        state.clearArmedSlot();
    }

    void ensureDetailIsEditable() throws AnnotationException
    {
        detailActionHandler().ensureIsEditable();
    }

    private boolean isSelectedAnnotationEditable()
    {
        var selectedLayerIsReadOnly = getModel() //
                .map(AnnotatorState::getSelectedAnnotationLayer) //
                .map(AnnotationLayer::isReadonly) //
                .orElse(false) //
                .getObject();

        return isDetailEditable() && !selectedLayerIsReadOnly;
    }

    boolean hasActiveEditor()
    {
        return editorPage.getActiveEditor().isPresent();
    }

    /**
     * @return whether the panel shows an annotation that is not selected in any editor (see
     *         {@link #actionShowReadOnly}).
     */
    boolean isFloating()
    {
        return stateModel.getFloatingSource() != null;
    }

    boolean isDetailEditable()
    {
        if (isFloating()) {
            return false;
        }

        return editorPage.getActiveEditor() //
                .map(DiamContext::getActionHandler) //
                .map(AnnotationActionHandler::isEditable) //
                .orElse(false);
    }

    /**
     * @return the action handler for the annotation shown in the panel - the active editor's, or a
     *         read-only one while the panel is floating.
     */
    AnnotationActionHandler detailActionHandler()
    {
        var floatingSource = stateModel.getFloatingSource();
        if (floatingSource != null) {
            return floatingSource.getActionHandler();
        }

        return editorPage.getActiveEditor().orElseThrow().getActionHandler();
    }

    /**
     * @return the CAS the annotation shown in the panel comes from - the active editor's, or the
     *         floating source's while the panel is floating.
     */
    CAS detailCas() throws IOException
    {
        var floatingSource = stateModel.getFloatingSource();
        if (floatingSource != null) {
            return floatingSource.getActionHandler().getEditorCas();
        }

        return editorPage.getActiveEditor().orElseThrow().getEditorCas();
    }

    private AnnotatorState activeEditorState()
    {
        return editorPage.getActiveEditor() //
                .map(DiamContext::getAnnotatorState) //
                .orElse(null);
    }

    /**
     * Persist the CAS of the editor the user is currently working in. Must be paired with
     * {@link #detailCas()} - writing through the page while having read through the active context
     * would persist the active editor's CAS as the main editor's annotations. While the panel is
     * floating, this fails because the floating source is read-only.
     */
    private void writeDetailCas() throws IOException, AnnotationException
    {
        detailActionHandler().writeEditorCas();
    }

    /**
     * Shows the given annotation read-only without changing the active editor or its selection. The
     * panel keeps showing it until an editor asks the panel to show its selection again, the active
     * editor's selection or content changes, or the active editor changes.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document the annotation belongs to
     * @param aDataOwner
     *            the user whose annotations contain the annotation
     * @param aVid
     *            the annotation, as resolved in the data owner's CAS
     * @param aOtherDataOwners
     *            other users who have the same annotation - named in the header alongside the data
     *            owner
     * @throws IOException
     *             if the CAS cannot be read
     * @throws AnnotationException
     *             if the feature editors cannot be loaded
     */
    public void actionShowReadOnly(AjaxRequestTarget aTarget, SourceDocument aDocument,
            User aDataOwner, VID aVid, List<User> aOtherDataOwners)
        throws IOException, AnnotationException
    {
        var state = new AnnotatorStateImpl();
        state.setUser(aDataOwner);
        state.setProject(aDocument.getProject());
        state.setDocument(aDocument, emptyList());
        state.setPagingStrategy(new NoPagingStrategy());

        // SHARED_READ_ONLY_ACCESS never writes the CAS file, so looking at the annotation does not
        // bump the data owner's CAS timestamp
        var dataOwnerSet = AnnotationSet.forUser(aDataOwner);
        CasProvider casProvider = () -> documentService.readAnnotationCas(aDocument, dataOwnerSet,
                AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);

        var annoFs = selectAnnotationByAddr(casProvider.get(), aVid.getId());
        var adapter = annotationService
                .getAdapter(annotationService.findLayer(state.getProject(), annoFs));
        // The state is not bound to any editor, so the selection event it fires is ignored here
        // (see isForActiveEditor)
        state.setSelection(adapter.select(aVid, annoFs));

        var ownerNames = new ArrayList<String>();
        ownerNames.add(aDataOwner.getUiName());
        aOtherDataOwners.forEach(user -> ownerNames.add(user.getUiName()));

        stateModel.setFloatingSource(new FloatingDetailSource(state, casProvider, ownerNames));

        loadFeatureEditorModels(aTarget);
        refresh(aTarget);
    }

    /**
     * Stops showing an annotation read-only (see {@link #actionShowReadOnly}). The panel shows the
     * active editor's selection again.
     */
    private void stopFloating()
    {
        stateModel.setFloatingSource(null);
    }

    /**
     * Stops showing an annotation read-only and shows the active editor's selection again. Clear
     * does this while the panel is floating.
     */
    private void actionCloseFloating(AjaxRequestTarget aTarget)
    {
        stopFloating();

        try {
            if (hasActiveEditor()) {
                loadFeatureEditorModels(aTarget);
            }
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }

        refresh(aTarget);
    }

    private String floatingOwnerName()
    {
        var floatingSource = stateModel.getFloatingSource();
        return floatingSource != null ? String.join(", ", floatingSource.getOwnerNames()) : null;
    }

    @OnEvent
    public void onShowAnnotationReadOnlyEvent(ShowAnnotationReadOnlyEvent aEvent)
    {
        try {
            actionShowReadOnly(aEvent.getRequestHandler(), aEvent.getDocument(),
                    aEvent.getDataOwner(), aEvent.getVid(), aEvent.getOtherDataOwners());
        }
        catch (Exception e) {
            handleException(this, aEvent.getRequestHandler(), e);
        }
    }

    /**
     * Shows the active editor's selection. An editor calls this to have its selection shown, so
     * this also ends showing an annotation read-only. The selection event alone would not be enough
     * because it does not fire if the selection has not changed.
     */
    public void actionLoadSelectionDetails(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        stopFloating();

        loadFeatureEditorModels(aTarget);

        if (aTarget != null) {
            refresh(aTarget);
        }
    }

    public void actionSelect(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        var annoFs = selectAnnotationByAddr(detailCas(), aVid.getId());
        var state = getModelObject();

        var adapter = annotationService
                .getAdapter(annotationService.findLayer(state.getProject(), annoFs));
        state.setSelection(adapter.select(aVid, annoFs));

        actionLoadSelectionDetails(aTarget);
    }

    private void actionSelectAndJump(AjaxRequestTarget aTarget, AnnotationFS annoFs)
        throws IOException, AnnotationException
    {
        actionSelect(aTarget, new VID(annoFs));

        if (isFloating()) {
            // There is no editor to jump in
            return;
        }

        var state = getModelObject();

        var context = editorPage.getActiveEditor().orElseThrow();

        // Resolve the ping ranges in the active context's CAS - that is where the selection's
        // origin/target addresses come from.
        var pingRanges = state.getSelection().pingRanges(detailCas());

        context.actionJumpTo(aTarget, annoFs.getBegin(), annoFs.getEnd(), pingRanges);
    }

    public void actionSelectAndJump(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        var targetFs = selectFsByAddr(detailCas(), aVid.getId());
        if (targetFs instanceof AnnotationFS) {
            actionSelectAndJump(aTarget, (AnnotationFS) targetFs);
        }
    }

    /**
     * Persists the potentially modified CAS, remembers feature values, reloads the feature editors
     * using the latest info from the CAS, updates the sentence number and focus unit, performs
     * auto-scrolling.
     */
    void internalCompleteAnnotation(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        var state = getModelObject();

        // persist changes - through the editor the user is working in, not necessarily the main
        // editor: writing via the page would persist it as the main document's annotations.
        writeDetailCas();

        // Remember the current feature values independently for spans and relations
        state.rememberFeatures();

        // Loading feature editor values from CAS
        loadFeatureEditorModels(aTarget);

        autoScroll();

        state.clearArmedSlot();
    }

    /**
     * Updates the selected annotation with the values presently in the feature editors. Creating
     * new annotations goes through the DIAM create handlers, not through here - the only caller
     * bails out unless an annotation is already selected.
     */
    private void internalCommitAnnotation(AjaxRequestTarget aTarget, CAS aCas)
        throws AnnotationException, IOException
    {
        var state = getModelObject();

        // The selectedAnnotationLayer indicates the layer type! Do not use the
        // defaultAnnotationLayer here as e.g. for relation annotations, it would point to the span
        // type to which the relation attaches, not to the relation type!
        var adapter = annotationService.getAdapter(state.getSelectedAnnotationLayer());

        reportMessages(aTarget, annotationEditingService.commitFeatureStates(state.getDocument(),
                state.getUser().getUsername(), aCas, state.getSelection().getAnnotation().getId(),
                adapter, state.getFeatureStates()));
    }

    public void actionDelete(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        var state = getModelObject();

        ensureDetailIsEditable();

        if (state.getSelection().getAnnotation().isNotSet()) {
            error("No annotation selected.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var cas = detailCas();

        var vid = state.getSelection().getAnnotation();
        var fs = selectAnnotationByAddr(cas, vid.getId());
        var layer = annotationService.findLayer(state.getProject(), fs);
        var adapter = annotationService.getAdapter(layer);

        if (layer.isReadonly()) {
            error("Cannot delete an annotation on a read-only layer.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var attachStatus = annotationEditingService.checkAttachStatus(state.getProject(), fs);
        if (attachStatus.isReadOnlyAttached()) {
            error("Cannot delete an annotation to which annotations on read-only layers attach.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (adapter instanceof SpanAdapter && attachStatus.getAttachCount() > 0) {
            var sessionOwner = userService.getCurrentUser();
            var confirmationPrefs = preferencesService.loadTraitsForUserAndProject(
                    KEY_ANNOTATION_EDITOR_MANAGER_PREFS, sessionOwner, state.getProject());

            if (confirmationPrefs.isShowDeleteAnnotationConfirmation()) {
                var dialogContent = new DeleteAnnotationConfirmationDialogPanel(
                        BootstrapModalDialog.CONTENT_ID, Model.of(layer), Model.of(attachStatus));
                dialogContent.setConfirmAction(_target -> doDelete(_target, layer, vid));
                confirmationDialog.open(dialogContent, aTarget);
                return;
            }
        }

        doDelete(aTarget, layer, vid);
    }

    private void doDelete(AjaxRequestTarget aTarget, AnnotationLayer layer, VID aVid)
        throws IOException, AnnotationException
    {
        CAS cas = detailCas();
        AnnotatorState state = getModelObject();
        TypeAdapter adapter = annotationService.getAdapter(layer);

        List<LogMessage> messages;
        try {
            messages = annotationEditingService.deleteAnnotation(cas, state, aVid, layer, adapter);
        }
        catch (PartialDeleteException e) {
            // FIXME: The CAS was already modified before the delete failed, so we do not write it
            // here. But it remains in the CAS storage session cache in its half-modified state, so
            // subsequent actions in this session still see the inconsistent CAS and may end up
            // persisting it. Properly recovering requires invalidating that cache entry and
            // re-reading the CAS from storage.
            LOG.error("Partial delete of [{}] on layer [{}] - CAS is now inconsistent", aVid,
                    layer.getUiName(), e);
            reportMessages(aTarget, e.getMessages());
            error("Could not delete annotation: " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        reportMessages(aTarget, messages);

        // Store CAS again
        writeDetailCas();

        // Update progress information
        int sentenceNumber = getSentenceNumber(cas, state.getSelection().getBegin());
        state.setFocusUnitIndex(sentenceNumber);

        autoScroll();

        state.rememberFeatures();

        reset(aTarget);
    }

    /**
     * Presents messages returned by {@link AnnotationEditingService}. The service does not talk to
     * Wicket, so reporting its messages is the caller's job.
     *
     * @param aTarget
     *            (optional) current AJAX target - if none is given, the messages are still added to
     *            the feedback panel, but it is not scheduled for repainting.
     */
    void reportMessages(AjaxRequestTarget aTarget, List<LogMessage> aMessages)
    {
        if (aMessages.isEmpty()) {
            return;
        }

        aMessages.forEach(message -> message.toWicket(this));

        if (aTarget != null) {
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    public void actionReverse(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        var state = getModelObject();

        ensureDetailIsEditable();

        var adapter = annotationService.getAdapter(state.getSelectedAnnotationLayer());
        var cas = detailCas();

        var messages = new ArrayList<LogMessage>();
        var newRelation = annotationEditingService.reverseRelation(state.getDocument(),
                state.getUser().getUsername(), cas, state.getSelection().getAnnotation().getId(),
                adapter, state.getFeatureStates(), messages);
        reportMessages(aTarget, messages);

        internalCompleteAnnotation(aTarget);

        state.setSelection(Selection.arc(newRelation));
    }

    public void actionClear(AjaxRequestTarget aTarget)
    {
        if (isFloating()) {
            actionCloseFloating(aTarget);
            return;
        }

        reset(aTarget);
        aTarget.add(this);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    /**
     * Scroll the window of visible annotations if auto-scrolling is enabled.
     */
    private void autoScroll() throws IOException
    {
        var state = getModelObject();
        if (state.getPreferences().isScrollPage()) {
            state.moveToSelection(detailCas());
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

        var cas = detailCas();
        var state = getModelObject();
        var selection = state.getSelection();

        try {
            // If we reset the layers while doing a relation, we won't be able to complete the
            // relation - so in this case, we leave the layers alone... The layers are only needed
            // to create annotations, which cannot be done on a floating annotation.
            if (!selection.isArc() && !isFloating()) {
                state.refreshSelectableLayers(schemaProperties::isLayerBlocked);

                if (state.getDefaultAnnotationLayer() != null) {
                    var sessionOwner = userService.getCurrentUser();
                    var anchoringPrefs = preferencesService.loadTraitsForUserAndProject(
                            KEY_ANCHORING_MODE, sessionOwner, state.getProject());
                    state.syncAnchoringModeToDefaultLayer(anchoringPrefs);
                }
            }

            // Nothing to load if no annotation is selected - the feature editors are not rendered
            // in that case anyway (see FeatureEditorListPanel.layerIsSelectedAndHasFeatures), and
            // pre-filling them from the remembered values is no longer this panel's job: creation
            // moved to the DIAM handlers, which apply the remembered values themselves (see
            // CreateSpanAnnotationHandler and CreateRelationAnnotationHandler).
            if (selection.getAnnotation().isNotSet()) {
                return;
            }

            // Updating existing annotation - load feature states from it
            var anno = selectAnnotationByAddr(cas, selection.getAnnotation().getId());

            // Try obtaining the layer from the feature structure
            AnnotationLayer layer;
            try {
                layer = annotationService.findLayer(state.getProject(), anno);
                state.setSelectedAnnotationLayer(layer);
                LOG.trace("loadFeatureEditorModels() selectedLayer set from selection: {}",
                        state.getSelectedAnnotationLayer().getUiName());
            }
            catch (NoResultException e) {
                reset(aTarget);
                throw new IllegalStateException("Unknown layer [" + anno.getType().getName() + "]",
                        e);
            }

            loadFeatureStates(aTarget, cas, layer, anno, null);
        }
        catch (Exception e) {
            throw new AnnotationException(e);
        }
    }

    /**
     * Loads the feature states from the service and installs them in the {@link AnnotatorState}.
     * The previous feature states are only replaced once the new ones have been loaded
     * successfully.
     */
    private void loadFeatureStates(AjaxRequestTarget aTarget, CAS aCas, AnnotationLayer aLayer,
            FeatureStructure aFS, Map<AnnotationFeature, Serializable> aRemembered)
        throws AnnotationException
    {
        var state = getModelObject();

        var messages = new ArrayList<LogMessage>();
        var featureStates = annotationEditingService.loadFeatureStates(aCas, state, aLayer, aFS,
                aRemembered, messages);
        reportMessages(aTarget, messages);

        state.getFeatureStates().clear();
        state.getFeatureStates().addAll(featureStates);
    }

    @Override
    public void onEvent(IEvent<?> aEvent)
    {
        super.onEvent(aEvent);

        var payload = aEvent.getPayload();

        if (payload instanceof SelectionChangedEvent selectionChanged) {
            onSelectionChangedEvent(selectionChanged);
        }
        else if (payload instanceof ActiveEditorChangedEvent activeEditorChanged) {
            onActiveEditorChangedEvent(activeEditorChanged);
        }
        else if (payload instanceof EditorContentReplacedEvent contentReplaced) {
            onEditorContentReplacedEvent(contentReplaced);
        }
        else if (payload instanceof DocumentStateChangedInEditorEvent stateChanged) {
            onDocumentStateChangedInEditorEvent(stateChanged);
        }
    }

    /**
     * The panel follows the active editor. While it is floating, {@link #getModelObject()} returns
     * the floating state, so the events must be matched against the active editor's state.
     */
    private boolean isForActiveEditor(EditorBoundEvent aEvent)
    {
        return aEvent.isFor(activeEditorState());
    }

    void onDocumentStateChangedInEditorEvent(DocumentStateChangedInEditorEvent aEvent)
    {
        if (!isForActiveEditor(aEvent)) {
            return;
        }

        if (aEvent.getRequestHandler() != null) {
            refresh(aEvent.getRequestHandler());
        }
    }

    void onEditorContentReplacedEvent(EditorContentReplacedEvent aEvent)
    {
        if (!isForActiveEditor(aEvent)) {
            return;
        }

        stopFloating();

        reset(aEvent.getRequestHandler());
    }

    void onSelectionChangedEvent(SelectionChangedEvent aEvent)
    {
        if (!isForActiveEditor(aEvent)) {
            return;
        }

        stopFloating();

        if (aEvent.getRequestHandler() != null) {
            try {
                loadFeatureEditorModels(aEvent.getRequestHandler());
                refresh(aEvent.getRequestHandler());
            }
            catch (Exception e) {
                handleException(this, aEvent.getRequestHandler(), e);
            }
        }
    }

    void onActiveEditorChangedEvent(ActiveEditorChangedEvent aEvent)
    {
        stopFloating();

        var target = aEvent.getRequestHandler();
        if (target == null) {
            return;
        }

        if (!isVisibleInHierarchy()) {
            return;
        }

        var context = aEvent.getContext();
        if (context == null) {
            // No active editor - re-render panel to hide it
            refresh(target);
            return;
        }

        try {
            if (context.getAnnotatorState().getSelection().getAnnotation().isNotSet()) {
                reset(target);
            }
            else {
                loadFeatureEditorModels(target);
            }

            refresh(target);
        }
        catch (Exception e) {
            handleException(this, target, e);
        }
    }

    private boolean annotationEventAffectsSelectedAnnotation(AnnotationEvent aEvent)
    {
        var state = getModelObject();
        var selection = state.getSelection();
        if (selection.getAnnotation().isNotSet()) {
            return false;
        }

        if (!state.getUser().getUsername().equals(aEvent.getDocumentOwner())) {
            return false;
        }

        return true;
    }

    @OnEvent
    public void onBulkAnnotationEvent(BulkAnnotationEvent aEvent)
    {
        if (!annotationEventAffectsSelectedAnnotation(aEvent)) {
            return;
        }

        try {
            var selection = getModelObject().getSelection();
            var id = selection.getAnnotation().getId();
            var annotationStillExists = detailCas().select(Annotation.class) //
                    .at(selection.getBegin(), selection.getEnd()) //
                    .anyMatch(ann -> ann._id() == id);

            if (!annotationStillExists) {
                if (isFloating()) {
                    stopFloating();
                }
                else {
                    getModelObject().clearSelection();
                }
                aEvent.getRequestTarget().ifPresent(this::refresh);
            }
        }
        catch (Exception e) {
            handleException(this, aEvent.getRequestTarget().orElse(null), e);
        }
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
        var state = getModelObject();

        // Clear selection and feature states
        state.getFeatureStates().clear();
        state.clearSelection();

        // Refresh the selectable layers dropdown
        state.refreshSelectableLayers(schemaProperties::isLayerBlocked);

        if (state.getDefaultAnnotationLayer() != null) {
            var sessionOwner = userService.getCurrentUser();
            var anchoringPrefs = preferencesService.loadTraitsForUserAndProject(KEY_ANCHORING_MODE,
                    sessionOwner, state.getProject());
            state.syncAnchoringModeToDefaultLayer(anchoringPrefs);
        }

        if (aTarget != null) {
            aTarget.add(layerSelectionPanel);
        }
    }

    // Used in commented-out code that we might want to comment back in again later
    @SuppressWarnings("unused")
    private static Set<AnnotationFS> getAttachedSpans(AnnotationSchemaService aAS, AnnotationFS aFs,
            AnnotationLayer aLayer)
    {
        var cas = aFs.getCAS();
        var attachedSpans = new HashSet<AnnotationFS>();
        var adapter = aAS.getAdapter(aLayer);
        if (adapter instanceof SpanAdapter && aLayer.getAttachType() != null) {
            var spanType = CasUtil.getType(cas, aLayer.getAttachType().getName());
            var attachFeature = spanType.getFeatureByBaseName(aLayer.getAttachFeature().getName());
            var type = spanType;

            for (var attachedFs : selectAt(cas, type, aFs.getBegin(), aFs.getEnd())) {
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

    private LambdaAjaxLink createClearButton()
    {
        var link = new LambdaAjaxLink("clear", this::actionClear);
        link.setOutputMarkupPlaceholderTag(true);
        link.setAlwaysEnabled(true); // Not to be disabled when document is read-only
        link.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()));
        link.add(keyBindings.getEditing().getClearSelection().toInputBehavior(click));
        link.add(
                AttributeModifier
                        .append("title",
                                () -> " ("
                                        + KeyBindingsUtil.formatShortcut(
                                                keyBindings.getEditing().getClearSelection())
                                        + ")"));
        return link;
    }

    private Component createReverseButton()
    {
        var link = new LambdaAjaxLink("reverse", this::actionReverse);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(LambdaBehavior.onConfigure(_this -> {
            var state = getModelObject();

            _this.setVisible(
                    state.getSelection().getAnnotation().isSet() && state.getSelection().isArc()
                            && RelationLayerSupport.TYPE
                                    .equals(state.getSelectedAnnotationLayer().getType())
                            && isDetailEditable());
        }));
        link.add(keyBindings.getEditing().getToggleSelection().toInputBehavior(click));
        link.add(
                AttributeModifier
                        .append("title",
                                () -> " ("
                                        + KeyBindingsUtil.formatShortcut(
                                                keyBindings.getEditing().getToggleSelection())
                                        + ")"));
        return link;
    }

    private LambdaAjaxLink createDeleteButton()
    {
        var link = new LambdaAjaxLink("delete", this::actionDelete);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()
                && isDetailEditable()));
        link.add(keyBindings.getEditing().getDeleteAnnotation().toInputBehavior(click));
        link.add(
                AttributeModifier
                        .append("title",
                                () -> " ("
                                        + KeyBindingsUtil.formatShortcut(
                                                keyBindings.getEditing().getDeleteAnnotation())
                                        + ")"));
        return link;
    }

    public FeatureEditorListPanel getFeatureEditorListPanel()
    {
        return featureEditorListPanel;
    }

    public void refresh(AjaxRequestTarget aTarget)
    {
        // We need to add the entire ADEP because we set the enabled state of the whole ADEP
        // hierarchy in its configure method for read-only layers or documents
        aTarget.add(this);
    }

    @OnEvent(stop = true)
    public void onLinkFeatureDeletedEvent(LinkFeatureDeletedEvent aEvent)
    {
        if (getModelObject().getSelection().getAnnotation().isNotSet()) {
            return;
        }

        // Auto-commit if working on existing annotation
        var target = aEvent.getTarget();
        try {
            ensureDetailIsEditable();

            internalCommitAnnotation(target, detailCas());
            internalCompleteAnnotation(target);
            refresh(target);
        }
        catch (Exception e) {
            handleException(this, target, e);
        }
    }

    /**
     * The state of the annotation shown in the panel - the active editor's, or the floating
     * source's while the panel is floating.
     */
    static class DetailStateModel
        implements IModel<AnnotatorState>
    {
        private static final long serialVersionUID = -7069428645365760907L;

        private final DocumentEditorManager manager;
        private FloatingDetailSource floatingSource;

        DetailStateModel(DocumentEditorManager aManager)
        {
            manager = aManager;
        }

        FloatingDetailSource getFloatingSource()
        {
            return floatingSource;
        }

        void setFloatingSource(FloatingDetailSource aFloatingSource)
        {
            floatingSource = aFloatingSource;
        }

        @Override
        public AnnotatorState getObject()
        {
            if (floatingSource != null) {
                return floatingSource.getState();
            }

            return manager.getActiveEditor() //
                    .map(DiamContext::getAnnotatorState) //
                    .orElse(null);
        }

        @Override
        public void detach()
        {
            // Detach only the active editor's model the detail panel is bound to
            manager.getActiveEditor() //
                    .map(DiamContext::getStateModel) //
                    .ifPresent(IModel::detach);
        }
    }
}
