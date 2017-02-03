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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation.action;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel.FeatureModel;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Covers information about the state of the annotation editor component that is relevant across
 * cycles.
 */
public interface AnnotatorState
{
    public void initForDocument(JCas aJCas, RepositoryService aRepository);

    // ---------------------------------------------------------------------------------------------
    // Window of visible annotations
    // ---------------------------------------------------------------------------------------------
    
    // REC: sentenceNumber/sentenceAddress can probably be dropped in favor of
    // firstSentenceNumber/firstSentenceAddress?
    
    /**
     * Get the number of the sentence in focus (curation view)
     */
    public int getFocusSentenceNumber();
    
    /**
     * Set the number of the sentence in focus (curation view)
     */
    public void setFocusSentenceNumber(int sentenceNumber);
    
    public void setFirstVisibleSentence(Sentence aSentence);
    
    public int getFirstVisibleSentenceAddress();
    @Deprecated
    public void setFirstVisibleSentenceAddress(int aSentenceAddress);
    public int getSentenceBeginOffset();
    public int getSentenceEndOffset();

    public int getFirstVisibleSentenceNumber();
    @Deprecated
    public void setFirstVisibleSentenceNumber(int fSN);
    public int getLastVisibleSentenceNumber();
    @Deprecated
    public void setLastVisibleSentenceNumber(int lSN);
    
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
    void setMode(Mode mode);
    
    // ---------------------------------------------------------------------------------------------
    // Remembered feature values
    //
    // These are optionally used when a new annotation is created to pre-fill feature values using
    // those of the last annotation of the same type. This can be useful when many annotations of
    // the same type with similar feature values need to be created.
    // ---------------------------------------------------------------------------------------------
    AnnotationLayer getRememberedSpanLayer();
    void setRememberedSpanLayer(AnnotationLayer rememberedSpanLayer);
    AnnotationLayer getRememberedArcLayer();
    void setRememberedArcLayer(AnnotationLayer rememberedArcLayer);
    Map<AnnotationFeature, Serializable> getRememberedSpanFeatures();
    void setRememberedSpanFeatures(List<FeatureModel> aModels);
    Map<AnnotationFeature, Serializable> getRememberedArcFeatures();
    void setRememberedArcFeatures(List<FeatureModel> aModels);
    void clearRememberedFeatures();
    
    // ---------------------------------------------------------------------------------------------
    // User
    // ---------------------------------------------------------------------------------------------
    
    // REC not sure if we need these really... we can fetch the user from the security context.
    // Might be interesting to have if we allow an admin to open another users annotation though.
    public User getUser();
    public void setUser(User aUser);
    
    // ---------------------------------------------------------------------------------------------
    // Document
    // ---------------------------------------------------------------------------------------------
    SourceDocument getDocument();
    void setDocument(SourceDocument aDocument);
    
    // ---------------------------------------------------------------------------------------------
    // Project
    // ---------------------------------------------------------------------------------------------
    Project getProject();
    void setProject(Project aProject);
    
    // REC: we cache the constraints when a document is opened because parsing them takes some time
    ParsedConstraints getConstraints();
    void setConstraints(ParsedConstraints aConstraints);
    
    // ---------------------------------------------------------------------------------------------
    // Selection
    // ---------------------------------------------------------------------------------------------
    Selection getSelection();
    void setArmedSlot(AnnotationFeature aName, int aIndex);
    boolean isArmedSlot(AnnotationFeature aName, int aIndex);
    void clearArmedSlot();
    boolean isSlotArmed();
    AnnotationFeature getArmedFeature();
    int getArmedSlot();
    
    // ---------------------------------------------------------------------------------------------
    // Rendering
    // - script direction can be changed by the user at will - it defaults to the direction
    //   configured in the project
    // ---------------------------------------------------------------------------------------------
    ScriptDirection getScriptDirection();
    void setScriptDirection(ScriptDirection aScriptDirection);
    
    // ---------------------------------------------------------------------------------------------
    // User preferences
    // ---------------------------------------------------------------------------------------------
    public AnnotationPreference getPreferences();
    public void setPreferences(AnnotationPreference aPreferences);
    List<AnnotationLayer> getAnnotationLayers();
    void setAnnotationLayers(List<AnnotationLayer> aAnnotationLayers);
}
