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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

/**
 * Covers information about the state of the annotation editor component that is relevant across
 * cycles.
 */
public interface AnnotatorState
    extends Serializable, AnnotatorViewState, AnnotatorDocumentNavigation
{
    void reset();

    /**
     * Get the timestamp when the annotation document was last changed on this.
     * 
     * @see #setAnnotationDocumentTimestamp(long)
     */
    Optional<Long> getAnnotationDocumentTimestamp();

    /**
     * Set the timestamp when the annotation document was last changed on this. This value must be
     * set explicitly whenever the annotation document is loaded by the editor. It can be used to
     * detect modifications to the file on disk which might make it incompatible with the current
     * state of the annotation editor (in particular it might invalidate the VIDs).
     */
    void setAnnotationDocumentTimestamp(long aTimeStamp);

    // ---------------------------------------------------------------------------------------------
    // Annotation behavior
    //
    // Control which kinds of annotations are created when an annotation creation action is
    // triggered and also what happens after the annotation has been created (e.g. auto-forward)
    // ---------------------------------------------------------------------------------------------
    boolean isForwardAnnotation();

    void setForwardAnnotation(boolean forwardAnnotation);

    /**
     * Get annotation layer of currently selected annotation.
     */
    AnnotationLayer getSelectedAnnotationLayer();

    /**
     * Set annotation layer of currently selected annotation.
     */
    void setSelectedAnnotationLayer(AnnotationLayer aLayer);

    /**
     * Get annotation layer used for newly created annotations.
     */
    AnnotationLayer getDefaultAnnotationLayer();

    /**
     * Set annotation layer used for newly created annotations.
     */
    void setDefaultAnnotationLayer(AnnotationLayer aLayer);

    // REC: would be very nice if we didn't need the mode - the behaviors specific to annotation,
    // curation, automation, correction, etc. should be local to the respective modules / pages
    Mode getMode();

    // ---------------------------------------------------------------------------------------------
    // Remembered feature values
    //
    // These are optionally used when a new annotation is created to pre-fill feature values using
    // those of the last annotation of the same type. This can be useful when many annotations of
    // the same type with similar feature values need to be created.
    // ---------------------------------------------------------------------------------------------
    void rememberFeatures();

    AnnotationLayer getRememberedSpanLayer();

    AnnotationLayer getRememberedArcLayer();

    Map<AnnotationFeature, Serializable> getRememberedSpanFeatures();

    Map<AnnotationFeature, Serializable> getRememberedArcFeatures();

    void clearRememberedFeatures();

    // ---------------------------------------------------------------------------------------------
    // User
    // ---------------------------------------------------------------------------------------------

    // REC not sure if we need these really... we can fetch the user from the security context.
    // Might be interesting to have if we allow an admin to open another users annotation though.
    User getUser();

    void setUser(User aUser);

    /**
     * User is viewing other people's work (read-only), but not as Curation User
     */
    boolean isUserViewingOthersWork(String aCurrentUserName);

    // ---------------------------------------------------------------------------------------------
    // Project
    // ---------------------------------------------------------------------------------------------
    @Override
    Project getProject();

    void setProject(Project aProject);

    /**
     * Set whether the user should be allowed to switch projects in the annotation editor "open
     * documents" dialog.
     * 
     * @deprecated Project selection will be removed from the open document dialog. This option here
     *             will also go away.
     */
    @Deprecated
    void setProjectLocked(boolean aFlag);

    /**
     * @deprecated Project selection will be removed from the open document dialog. This option here
     *             will also go away.
     */
    @Deprecated
    boolean isProjectLocked();

    // REC: we cache the constraints when a document is opened because parsing them takes some time
    ParsedConstraints getConstraints();

    void setConstraints(ParsedConstraints aConstraints);

    // ---------------------------------------------------------------------------------------------
    // Selection
    // ---------------------------------------------------------------------------------------------
    @Override
    Selection getSelection();

    /**
     * Mark a slot in the given feature state as armed. Note that this feature state does not
     * necessarily belong to the feature states for the annotation detail panel (cf.
     * {@link #getFeatureStates()}) but may belong to some other feature editor elsewhere in the UI.
     */
    void setArmedSlot(FeatureState aState, int aIndex);

    boolean isArmedSlot(FeatureState aState, int aIndex);

    void clearArmedSlot();

    boolean isSlotArmed();

    FeatureState getArmedFeature();

    int getArmedSlot();

    // ---------------------------------------------------------------------------------------------
    // User preferences
    // ---------------------------------------------------------------------------------------------
    @Override
    AnnotationPreference getPreferences();

    void setPreferences(AnnotationPreference aPreferences);

    /**
     * Set all layers in the project, including hidden layers, non-enabled layers, etc. This is just
     * a convenience method to quickly access layer information and to avoid having to access the
     * database every time during rendering to get this information.
     * 
     * @param aLayers
     *            all layers.
     */
    void setAllAnnotationLayers(List<AnnotationLayer> aLayers);

    List<AnnotationLayer> getAllAnnotationLayers();

    /**
     * Get the annotation layers which are usable by the annotator (i.e. enabled, visible according
     * to the user preferences , etc.)
     * 
     * @return usable layers
     */
    List<AnnotationLayer> getAnnotationLayers();

    /**
     * Set the annotation layers which are usable by the annotator (i.e. enabled, visible according
     * to the user preferences , etc.)
     * 
     * @param aAnnotationLayers
     *            usable layers
     */
    void setAnnotationLayers(List<AnnotationLayer> aAnnotationLayers);

    // ---------------------------------------------------------------------------------------------
    // Feature value models
    // ---------------------------------------------------------------------------------------------
    List<FeatureState> getFeatureStates();

    FeatureState getFeatureState(AnnotationFeature aFeature);

    // ---------------------------------------------------------------------------------------------
    // Meta data
    // ---------------------------------------------------------------------------------------------

    <M extends Serializable> M getMetaData(final AnnotatorStateMetaDataKey<M> aKey);

    <M extends Serializable> void setMetaData(final AnnotatorStateMetaDataKey<M> aKey, M aMetadata);
}
