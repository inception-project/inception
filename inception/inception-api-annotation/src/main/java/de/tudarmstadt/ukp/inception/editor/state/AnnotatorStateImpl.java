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
package de.tudarmstadt.ukp.inception.editor.state;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnchoringModePrefs;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorStateMetaDataKey;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.paging.PagingStrategy;
import de.tudarmstadt.ukp.inception.rendering.paging.Unit;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderSlotsEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.AnnotatorViewportChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerTypes;

/**
 * Data model for annotation editors
 */
public class AnnotatorStateImpl
    implements Serializable, AnnotatorState
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = 1078613192789450714L;

    /**
     * The Project the annotator working on
     */
    private Project project;

    /**
     * The source document the to be annotated
     */
    private SourceDocument document;

    // This is being used in the action bar paging area to indicate the maximum number of units
    private int documentIndex = -1;

    // This is being used in the action bar paging area to indicate the maximum number of units
    private int numberOfDocuments = -1;

    /**
     * The current user annotating the document
     */
    private User user;

    private String editorFactoryId;

    private ScriptDirection scriptDirection;

    /**
     * The begin offset of the first visible sentence.
     */
    private int windowBeginOffset;

    /**
     * The end offset of the last visible sentence.
     */
    private int windowEndOffset;

    /**
     * The index of the unit an action occurred (selection, modification, clicking).
     */
    private int focusUnitIndex;

    /**
     * The index of the first visible unit in the display window.
     */
    // This is being used in the action bar paging area to indicate the maximum number of units
    private int firstVisibleUnitIndex;

    /**
     * The index of the last visible unit in the display window.
     */
    // This is being used in the action bar paging area to indicate the maximum number of units
    private int lastVisibleUnitIndex;

    /**
     * The total number of units in the document.
     */
    // This is being used in the action bar paging area to indicate the maximum number of units
    private int unitCount;

    private final List<FeatureState> featureModels = new ArrayList<>();

    /**
     * Constraints object from rule file
     */
    private ParsedConstraints constraints;

    /**
     * The project annotation layers available for annotation.
     */
    private List<AnnotationLayer> annotationLayers = new ArrayList<>();

    /**
     * All project annotation layers.
     */
    private List<AnnotationLayer> allAnnotationLayers = new ArrayList<>();

    /**
     * Selectable annotation layers.
     */
    private List<AnnotationLayer> selectableLayers = new ArrayList<>();

    private AnnotationPreference preferences = new AnnotationPreference();

    /**
     * The Mode of the current operations as either {@link Mode#ANNOTATION} or as
     * {@link Mode#CURATION}
     */
    private Mode mode;

    /**
     * The previously selected {@link TagSet} and {@link Tag} for a span/Arc annotation so as to
     * pre-fill the type in the span/arc annotation dialog (only for new span/arc annotations)
     */
    private AnnotationLayer rememberedSpanLayer;
    private AnnotationLayer rememberedArcLayer;

    private Map<AnnotationFeature, Serializable> rememberedSpanFeatures = new HashMap<>();
    private Map<AnnotationFeature, Serializable> rememberedArcFeatures = new HashMap<>();

    // the selected annotation layer
    private AnnotationLayer selectedAnnotationLayer;

    // Text field to capture key-bindings for forward annotations
    @Deprecated
    @SuppressWarnings("unused")
    private String forwardAnno;

    // the default annotation layer
    private AnnotationLayer defaultAnnotationLayer;

    // the name of the default annotation layer
    @SuppressWarnings("unused")
    private String layerName;

    // User action while annotating on document
    @Deprecated
    @SuppressWarnings("unused")
    private String userAction;

    private Long annotationDocumentTimestamp;

    private PagingStrategy pagingStrategy;

    private List<Unit> visibleUnits = emptyList();

    private Map<AnnotatorStateMetaDataKey<?>, Object> metaData = new HashMap<>();

    private AnchoringMode anchoringMode;

    public AnnotatorStateImpl(Mode aMode)
    {
        mode = aMode;
    }

    @Override
    public ParsedConstraints getConstraints()
    {
        return constraints;
    }

    @Override
    public void setConstraints(ParsedConstraints aConstraints)
    {
        constraints = aConstraints;
    }

    private final Selection selection = new Selection();

    @Override
    public Selection getSelection()
    {
        return selection;
    }

    @Override
    public Project getProject()
    {
        return project;
    }

    @Override
    public void setProject(Project aProject)
    {
        if (project == null || !project.equals(aProject)) {
            setScriptDirection(aProject.getScriptDirection());
        }

        project = aProject;
    }

    @Override
    public void clearProject()
    {
        project = null;
        clearDocument();
    }

    @Override
    public ScriptDirection getScriptDirection()
    {
        return scriptDirection;
    }

    @Override
    public void toggleScriptDirection()
    {
        if (ScriptDirection.LTR.equals(getScriptDirection())) {
            setScriptDirection(ScriptDirection.RTL);
        }
        else {
            setScriptDirection(ScriptDirection.LTR);
        }
    }

    @Override
    public void setScriptDirection(ScriptDirection aScriptDirection)
    {
        scriptDirection = aScriptDirection;
    }

    @Override
    public SourceDocument getDocument()
    {
        return document;
    }

    @Override
    public int getDocumentIndex()
    {
        return documentIndex;
    }

    @Override
    public int getNumberOfDocuments()
    {
        return numberOfDocuments;
    }

    @Override
    public void clearDocument()
    {
        setDocument(null, null);
    }

    @Override
    public void setDocument(SourceDocument aDocument, List<SourceDocument> aDocuments)
    {
        document = aDocument;
        if (aDocument != null) {
            documentIndex = aDocuments.indexOf(aDocument);
            numberOfDocuments = aDocuments.size();
        }
        else {
            documentIndex = -1;
            numberOfDocuments = -1;
        }
    }

    @Override
    public void refreshDocument(DocumentService aDocumentService)
    {
        if (document != null) {
            document = aDocumentService.getSourceDocument(document.getProject().getId(),
                    document.getId());
        }
    }

    @Override
    public void refreshProject(ProjectService aProjectService)
    {
        if (project != null) {
            project = aProjectService.getProject(project.getId());
        }
    }

    @Override
    public User getUser()
    {
        return user;
    }

    @Override
    public void setUser(User aUser)
    {
        user = aUser;
    }

    /**
     * @deprecated use {@link #setPageBegin(CAS, int)} instead.
     */
    @Deprecated
    @Override
    public void setFirstVisibleUnit(AnnotationFS aFirstVisibleUnit)
    {
        setPageBegin(aFirstVisibleUnit.getCAS(), aFirstVisibleUnit.getBegin());
    }

    @Override
    public void setPageBegin(CAS aCas, int aOffset)
    {
        var ps = getPagingStrategy();
        var units = ps.unitsStartingAtOffset(aCas, aOffset, getPreferences().getWindowSize());
        setVisibleUnits(units, ps.unitCount(aCas));
    }

    @Override
    public void setVisibleUnits(List<Unit> aUnits, int aTotalUnitCount)
    {
        if (aUnits.isEmpty()) {
            unitCount = 0;
            visibleUnits = aUnits;
            firstVisibleUnitIndex = 0;
            lastVisibleUnitIndex = 0;
            focusUnitIndex = 0;
            windowBeginOffset = 0;
            windowEndOffset = 0;
            fireViewStateChanged();
            return;
        }

        unitCount = aTotalUnitCount;
        visibleUnits = aUnits;
        firstVisibleUnitIndex = aUnits.get(0).getIndex();
        lastVisibleUnitIndex = aUnits.get(aUnits.size() - 1).getIndex();
        focusUnitIndex = firstVisibleUnitIndex;

        int newWindowBeginOffset = aUnits.get(0).getBegin();
        int newWindowEndOffset = aUnits.get(aUnits.size() - 1).getEnd();
        if (windowBeginOffset != newWindowBeginOffset || windowEndOffset != newWindowEndOffset) {
            windowBeginOffset = newWindowBeginOffset;
            windowEndOffset = newWindowEndOffset;
            fireViewStateChanged();
        }
    }

    @Override
    public List<Unit> getVisibleUnits()
    {
        return visibleUnits;
    }

    @Override
    public int getWindowBeginOffset()
    {
        return windowBeginOffset;
    }

    @Override
    public int getWindowEndOffset()
    {
        return windowEndOffset;
    }

    @Override
    public List<AnnotationLayer> getAllAnnotationLayers()
    {
        return allAnnotationLayers;
    }

    @Override
    public void setAllAnnotationLayers(List<AnnotationLayer> aLayers)
    {
        allAnnotationLayers = unmodifiableList(new ArrayList<>(aLayers));
    }

    @Override
    public List<AnnotationLayer> getAnnotationLayers()
    {
        return annotationLayers;
    }

    @Override
    public void setAnnotationLayers(List<AnnotationLayer> aAnnotationLayers)
    {
        annotationLayers = unmodifiableList(new ArrayList<>(aAnnotationLayers));

        // Make sure the currently selected layer is actually visible/exists
        if (!annotationLayers.contains(selectedAnnotationLayer)) {
            selectedAnnotationLayer = annotationLayers.stream() //
                    .filter(layer -> layer.getType().equals(LayerTypes.SPAN_LAYER_TYPE)) //
                    .findFirst() //
                    .orElse(null);
            defaultAnnotationLayer = selectedAnnotationLayer;
        }
    }

    public void setSelectableLayers(List<AnnotationLayer> aSelectableLayers)
    {
        selectableLayers = aSelectableLayers;
    }

    @Override
    public List<AnnotationLayer> getSelectableLayers()
    {
        return selectableLayers;
    }

    @Override
    public void refreshSelectableLayers(Predicate<AnnotationLayer> isLayerBlocked)
    {
        selectableLayers.clear();

        for (var layer : getAnnotationLayers()) {
            if (!layer.isEnabled() || layer.isReadonly() || isLayerBlocked.test(layer)) {
                continue;
            }

            if (layer.getType().equals(LayerTypes.SPAN_LAYER_TYPE)
                    || layer.getType().equals(LayerTypes.CHAIN_LAYER_TYPE)) {
                selectableLayers.add(layer);
            }
        }

        // Is the selected layer selectable?
        if (getSelectedAnnotationLayer() == null
                || !selectableLayers.contains(getSelectedAnnotationLayer())) {
            // if there is only one layer, we use it to create new annotations
            if (selectableLayers.size() == 1) {
                setDefaultAnnotationLayer(selectableLayers.get(0));
            }

            if (getDefaultAnnotationLayer() != null) {
                setSelectedAnnotationLayer(getDefaultAnnotationLayer());
            }
            else if (!selectableLayers.isEmpty()) {
                setSelectedAnnotationLayer(selectableLayers.get(0));
            }
        }
    }

    @Override
    public AnnotationPreference getPreferences()
    {
        return preferences;
    }

    @Override
    public void setPreferences(AnnotationPreference aPreferences)
    {
        preferences = aPreferences;
    }

    @Deprecated
    @Override
    public Mode getMode()
    {
        return mode;
    }

    @Override
    public AnnotationLayer getRememberedSpanLayer()
    {
        return rememberedSpanLayer;
    }

    @Override
    public Map<AnnotationFeature, Serializable> getRememberedSpanFeatures()
    {
        return rememberedSpanFeatures;
    }

    private void setRememberedSpanFeatures(List<FeatureState> aModels)
    {
        rememberedSpanFeatures = new HashMap<>();
        if (aModels != null) {
            for (var fm : aModels) {
                // Do not remember values unless this feature is enabled
                if (!fm.feature.isRemember()) {
                    continue;
                }

                // Do not remember link features.
                if (!LinkMode.NONE.equals(fm.feature.getLinkMode())) {
                    continue;
                }
                rememberedSpanFeatures.put(fm.feature, fm.value);
            }
        }
    }

    @Override
    public Map<AnnotationFeature, Serializable> getRememberedArcFeatures()
    {
        return rememberedArcFeatures;
    }

    private void setRememberedArcFeatures(List<FeatureState> aModels)
    {
        rememberedArcFeatures = new HashMap<>();
        if (aModels != null) {
            for (FeatureState fm : aModels) {
                // Do not remember values unless this feature is enabled
                if (!fm.feature.isRemember()) {
                    continue;
                }

                // Do not remember link features.
                if (!LinkMode.NONE.equals(fm.feature.getLinkMode())) {
                    continue;
                }
                rememberedArcFeatures.put(fm.feature, fm.value);
            }
        }
    }

    @Override
    public int getFocusUnitIndex()
    {
        return focusUnitIndex;
    }

    @Override
    public void setFocusUnitIndex(int aUnitIndex)
    {
        focusUnitIndex = aUnitIndex;
    }

    @Override
    public int getFirstVisibleUnitIndex()
    {
        return firstVisibleUnitIndex;
    }

    @Override
    public int getLastVisibleUnitIndex()
    {
        return lastVisibleUnitIndex;
    }

    @Override
    public int getUnitCount()
    {
        return unitCount;
    }

    @Override
    public AnnotationLayer getSelectedAnnotationLayer()
    {
        return selectedAnnotationLayer;
    }

    @Override
    public void setSelectedAnnotationLayer(AnnotationLayer selectedAnnotationLayer)
    {
        this.selectedAnnotationLayer = selectedAnnotationLayer;
    }

    @Override
    public AnnotationLayer getDefaultAnnotationLayer()
    {
        return defaultAnnotationLayer;
    }

    @Override
    public void setDefaultAnnotationLayer(AnnotationLayer defaultAnnotationLayer)
    {
        this.defaultAnnotationLayer = defaultAnnotationLayer;
    }

    @Override
    public void rememberFeatures()
    {
        LOG.trace("Remembering feature editor values");
        if (getSelection().isArc()) {
            rememberedArcLayer = getSelectedAnnotationLayer();
            setRememberedArcFeatures(featureModels);
        }
        else {
            rememberedSpanLayer = getSelectedAnnotationLayer();
            setRememberedSpanFeatures(featureModels);
        }
    }

    @Override
    public void clearRememberedFeatures()
    {
        setRememberedArcFeatures(null);
        rememberedArcLayer = null;
        setRememberedSpanFeatures(null);
        rememberedSpanLayer = null;
    }

    @Override
    public void reset()
    {
        getSelection().clear();
        clearArmedSlot();
        clearRememberedFeatures();
        focusUnitIndex = 0;
        firstVisibleUnitIndex = 0;
        lastVisibleUnitIndex = 0;
        unitCount = 0;
        windowBeginOffset = 0;
        windowEndOffset = 0;
        annotationDocumentTimestamp = null;

        fireViewStateChanged();
    }

    private FeatureState armedFeatureState;
    private int armedSlot = -1;

    @Override
    public void setArmedSlot(FeatureState aState, int aIndex)
    {
        boolean needRerender = armedFeatureState != aState || armedSlot != aIndex;
        armedFeatureState = aState;
        armedSlot = aIndex;
        if (needRerender) {
            rerenderSlots();
        }
    }

    @Override
    public boolean isArmedSlot(FeatureState aState, int aIndex)
    {
        if (armedFeatureState == null) {
            return false;
        }

        return Objects.equals(aState.vid, armedFeatureState.vid)
                && Objects.equals(aState.feature, armedFeatureState.feature) && aIndex == armedSlot;
    }

    /**
     * Re-render all slots to de-select all slots that are not armed anymore
     */
    private void rerenderSlots()
    {
        RequestCycle requestCycle = RequestCycle.get();

        if (requestCycle == null) {
            return;
        }

        Optional<IPageRequestHandler> handler = requestCycle.find(IPageRequestHandler.class);
        if (handler.isPresent() && handler.get().isPageInstanceCreated()) {
            Page page = (Page) handler.get().getPage();
            page.send(page, BREADTH, new RenderSlotsEvent(
                    requestCycle.find(IPartialPageRequestHandler.class).orElse(null)));
        }
    }

    @Override
    public void clearArmedSlot()
    {
        boolean needRerender = armedFeatureState != null || armedSlot != -1;
        armedFeatureState = null;
        armedSlot = -1;
        if (needRerender) {
            rerenderSlots();
        }
    }

    @Override
    public boolean isSlotArmed()
    {
        return armedFeatureState != null;
    }

    @Override
    public FeatureState getArmedFeature()
    {
        return armedFeatureState;
    }

    @Override
    public int getArmedSlot()
    {
        return armedSlot;
    }

    @Override
    public List<FeatureState> getFeatureStates()
    {
        return featureModels;
    }

    @Override
    public FeatureState getFeatureState(AnnotationFeature aFeature)
    {
        for (FeatureState f : featureModels) {
            if (Objects.equals(f.feature.getId(), aFeature.getId())) {
                return f;
            }
        }
        return null;
    }

    @Override
    public Optional<Long> getAnnotationDocumentTimestamp()
    {
        return Optional.ofNullable(annotationDocumentTimestamp);
    }

    @Override
    public void setAnnotationDocumentTimestamp(long aAnnotationDocumentTimestamp)
    {
        annotationDocumentTimestamp = aAnnotationDocumentTimestamp;
    }

    @Override
    public PagingStrategy getPagingStrategy()
    {
        return pagingStrategy;
    }

    @Override
    public void setPagingStrategy(PagingStrategy aPagingStrategy)
    {
        pagingStrategy = aPagingStrategy;
    }

    @Override
    public boolean isUserViewingOthersWork(String aCurrentUserName)
    {
        return !user.getUsername().equals(aCurrentUserName);
    }

    @Override
    public void setEditorFactoryId(String aId)
    {
        editorFactoryId = aId;
    }

    @Override
    public String getEditorFactoryId()
    {
        return editorFactoryId;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <M extends Serializable> M getMetaData(AnnotatorStateMetaDataKey<M> aKey)
    {
        if (metaData.containsKey(aKey)) {
            return (M) metaData.get(aKey);
        }
        return null;
    }

    @Override
    public <M extends Serializable> void setMetaData(AnnotatorStateMetaDataKey<M> aKey, M aMetadata)
    {
        metaData.put(aKey, aMetadata);
    }

    private void fireViewStateChanged()
    {
        RequestCycle requestCycle = RequestCycle.get();

        if (requestCycle == null) {
            return;
        }

        Optional<IPageRequestHandler> handler = requestCycle.find(IPageRequestHandler.class);
        if (handler.isPresent() && handler.get().isPageInstanceCreated()) {
            Page page = (Page) handler.get().getPage();
            page.send(page, BREADTH, new AnnotatorViewportChangedEvent(
                    requestCycle.find(AjaxRequestTarget.class).orElse(null)));
        }
    }

    @Override
    public AnchoringMode getAnchoringMode()
    {
        return anchoringMode;
    }

    @Override
    public void setAnchoringMode(AnchoringMode aAnchoringMode)
    {
        anchoringMode = aAnchoringMode;
    }

    @Override
    public void syncAnchoringModeToDefaultLayer(AnchoringModePrefs aAnchoringPrefs)
    {
        var defaultLayer = getDefaultAnnotationLayer();
        if (defaultLayer == null) {
            setAnchoringMode(null);
            return;
        }

        var prefAnchoringMode = aAnchoringPrefs.getAnchoringMode(defaultLayer);
        if (prefAnchoringMode.map(defaultLayer.getAnchoringMode()::allows).orElse(false)) {
            prefAnchoringMode.ifPresent(this::setAnchoringMode);
        }
        else {
            setAnchoringMode(defaultLayer.getAnchoringMode());
        }
    }
}
