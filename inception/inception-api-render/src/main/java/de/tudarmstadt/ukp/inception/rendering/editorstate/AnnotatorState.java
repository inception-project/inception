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
package de.tudarmstadt.ukp.inception.rendering.editorstate;

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
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

/**
 * Covers information about the state of the annotation editor component that is relevant across
 * cycles.
 */
public interface AnnotatorState
    extends Serializable, AnnotationSelectionState, AnnotatorViewState, AnnotatorDocumentNavigation,
    LayerSelectionState
{
    void reset();

    /**
     * @return the timestamp when the annotation document was last changed on this.
     * 
     * @see #setAnnotationDocumentTimestamp(long)
     */
    Optional<Long> getAnnotationDocumentTimestamp();

    /**
     * @param aTimeStamp
     *            the timestamp when the annotation document was last changed on this. This value
     *            must be set explicitly whenever the annotation document is loaded by the editor.
     *            It can be used to detect modifications to the file on disk which might make it
     *            incompatible with the current state of the annotation editor (in particular it
     *            might invalidate the VIDs).
     */
    void setAnnotationDocumentTimestamp(long aTimeStamp);

    // ---------------------------------------------------------------------------------------------
    // Annotation behavior
    //
    // Control which kinds of annotations are created when an annotation creation action is
    // triggered and also what happens after the annotation has been created (e.g. auto-forward)
    // ---------------------------------------------------------------------------------------------

    // REC: would be very nice if we didn't need the mode - the behaviors specific to annotation,
    // curation, automation, correction, etc. should be local to the respective modules / pages
    @Deprecated
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

    Map<AnnotationFeature, Serializable> getRememberedSpanFeatures();

    Map<AnnotationFeature, Serializable> getRememberedArcFeatures();

    void clearRememberedFeatures();

    // ---------------------------------------------------------------------------------------------
    // User
    // ---------------------------------------------------------------------------------------------
    User getUser();

    void setUser(User aUser);

    /**
     * @param aCurrentUserName
     *            name of the current user
     * @return if user is viewing other people's work (read-only), but not as Curation User
     */
    boolean isUserViewingOthersWork(String aCurrentUserName);

    // ---------------------------------------------------------------------------------------------
    // Project
    // ---------------------------------------------------------------------------------------------
    @Override
    Project getProject();

    void clearProject();

    void setProject(Project aProject);

    void refreshProject(ProjectService aProjectService);

    // ---------------------------------------------------------------------------------------------
    // Constraints
    // REC: we cache the constraints when a document is opened because parsing them takes some time
    // ---------------------------------------------------------------------------------------------

    void setConstraints(ParsedConstraints aConstraints);

    ParsedConstraints getConstraints();

    // ---------------------------------------------------------------------------------------------
    // User preferences
    // ---------------------------------------------------------------------------------------------
    @Override
    AnnotationPreference getPreferences();

    void setPreferences(AnnotationPreference aPreferences);

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
