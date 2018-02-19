/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

    // ---------------------------------------------------------------------------------------------
    // Annotation behavior
    //
    // Control which kinds of annotations are created when an annotation creation action is
    // triggered and also what happens after the annotation has been created (e.g. auto-forward)
    // ---------------------------------------------------------------------------------------------
    boolean isForwardAnnotation();

    void setForwardAnnotation(boolean forwardAnnotation);

    AnnotationLayer getSelectedAnnotationLayer();

    void setSelectedAnnotationLayer(AnnotationLayer selectedAnnotationLayer);

    AnnotationLayer getDefaultAnnotationLayer();

    void setDefaultAnnotationLayer(AnnotationLayer defaultAnnotationLayer);

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

    // ---------------------------------------------------------------------------------------------
    // Project
    // ---------------------------------------------------------------------------------------------
    @Override
    Project getProject();

    void setProject(Project aProject);
    
    /**
     * Set whether the user should be allowed to switch projects in the annotation editor
     * "open documents" dialog.
     */
    void setProjectLocked(boolean aFlag);
    
    boolean isProjectLocked();

    // REC: we cache the constraints when a document is opened because parsing them takes some time
    ParsedConstraints getConstraints();

    void setConstraints(ParsedConstraints aConstraints);

    // ---------------------------------------------------------------------------------------------
    // Selection
    // ---------------------------------------------------------------------------------------------
    @Override
    Selection getSelection();

    void setArmedSlot(AnnotationFeature aName, int aIndex);

    boolean isArmedSlot(AnnotationFeature aName, int aIndex);

    void clearArmedSlot();

    boolean isSlotArmed();

    AnnotationFeature getArmedFeature();

    int getArmedSlot();

    // ---------------------------------------------------------------------------------------------
    // User preferences
    // ---------------------------------------------------------------------------------------------
    @Override
    AnnotationPreference getPreferences();

    void setPreferences(AnnotationPreference aPreferences);

    List<AnnotationLayer> getAnnotationLayers();

    void setAnnotationLayers(List<AnnotationLayer> aAnnotationLayers);

    // ---------------------------------------------------------------------------------------------
    // Feature value models
    // ---------------------------------------------------------------------------------------------
    List<FeatureState> getFeatureStates();

    FeatureState getFeatureState(AnnotationFeature aFeature);
}
