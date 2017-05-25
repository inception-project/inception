/*
 * Copyright 2012
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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getLastSentenceInDisplayWindow;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
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
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Data model for annotation editors
 */
public class AnnotatorStateImpl
    implements Serializable, AnnotatorState, TransientActionContext
{
    private static final long serialVersionUID = 1078613192789450714L;

    /**
     * The Project the annotator working on
     */
    private Project project;
    private boolean projectLocked = false;

    /**
     * The source document the to be annotated
     */
    private SourceDocument document;
    private int documentIndex = -1;
    private int numberOfDocuments = -1;

    /**
     * The current user annotating the document
     */
    private User user;

    private ScriptDirection scriptDirection;

    /**
     * The sentence address where the display window starts with, in its UIMA annotation
     */
    private int displayWindowStartSentenceAddress = -1;

    /**
     * The begin offset of a sentence
     */
    private int sentenceBeginOffset;

    /**
     * The end offset of a sentence
     */
    private int sentenceEndOffset;

    /**
     * The begin offset of the first visible sentence.
     */
    private int windowBeginOffset;

    /**
     * The end offset of the last visible sentence.
     */
    private int windowEndOffset;

    /**
     * the sentence number where an action occured (selection, modification, clicking)
     */
    private int focusSentenceNumber;
    
    /**
     * The first sentence number in the display window
     */
    private int firstVisibleSentenceNumber;
    
    /**
     * The last sentence number in the display window
     */
    private int lastVisibleSentenceNumber;

    /**
     * The total number of sentences in the document
     */
    private int numberOfSentences;

    private List<FeatureState> featureModels = new ArrayList<>();          
    
    /**
     * Constraints object from rule file
     */
    private ParsedConstraints constraints;

    /**
     * The annotation layers available in the current project.
     */
    private List<AnnotationLayer> annotationLayers = new ArrayList<AnnotationLayer>();

    private AnnotationPreference preferences = new AnnotationPreference();

    /**
     * The Mode of the current operations as either {@link Mode#ANNOTATION} or as
     * {@link Mode#CURATION}
     */
    private Mode mode;

    /**
     * The previously selected {@link TagSet} and {@link Tag} for a span/Arc annotation so as toz
     * pre-fill the type in the span/arc annotation dialog (only for new span/arc annotations)
     */
    private AnnotationLayer rememberedSpanLayer;
    private AnnotationLayer rememberedArcLayer;

    private Map<AnnotationFeature, Serializable> rememberedSpanFeatures = new HashMap<AnnotationFeature, Serializable>();
    private Map<AnnotationFeature, Serializable> rememberedArcFeatures = new HashMap<AnnotationFeature, Serializable>();

    // the selected annotation layer
    private AnnotationLayer selectedAnnotationLayer;

    // Text field to capture key-bindings for forward annotations
    private String forwardAnno;
    
    // the default annotation layer
    private AnnotationLayer defaultAnnotationLayer;

    // the name of the default annotation layer
    private String layerName;

    // enable automatic forward annotations
    private boolean forwardAnnotation;

    // User action while annotating on document
    private String userAction;

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

    @Override
    public String getUserAction()
    {
        return userAction;
    }

    @Override
    public void setUserAction(String aUserAction)
    {
        userAction = aUserAction;
    }

    @Override
    public void clearUserAction()
    {
        userAction = null;
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
        project = aProject;
        setScriptDirection(project.getScriptDirection());
    }

    @Override
    public void setProjectLocked(boolean aFlag)
    {
        projectLocked = aFlag;
    }
    
    @Override
    public boolean isProjectLocked()
    {
        return projectLocked;
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
    public User getUser()
    {
        return user;
    }

    @Override
    public void setUser(User aUser)
    {
        user = aUser;
    }

    @Override
    public void setFirstVisibleSentence(Sentence aSentence)
    {
        JCas jcas;
        try {
            jcas = aSentence.getCAS().getJCas();
        }
        catch (CASException e) {
            throw new IllegalStateException("Unable to fetch JCas from CAS", e);
        }

        displayWindowStartSentenceAddress = WebAnnoCasUtil.getAddr(aSentence);
        sentenceBeginOffset = aSentence.getBegin();
        sentenceEndOffset = aSentence.getEnd();

        Sentence lastVisibleSentence = getLastSentenceInDisplayWindow(jcas, getAddr(aSentence),
                getPreferences().getWindowSize());
        firstVisibleSentenceNumber = WebAnnoCasUtil.getSentenceNumber(jcas,
                aSentence.getBegin());
        lastVisibleSentenceNumber = WebAnnoCasUtil.getSentenceNumber(jcas,
                lastVisibleSentence.getBegin());
        numberOfSentences = select(jcas, Sentence.class).size();
        
        windowBeginOffset = aSentence.getBegin();
        windowEndOffset = lastVisibleSentence.getEnd();
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
    public int getFirstVisibleSentenceAddress()
    {
        return displayWindowStartSentenceAddress;
    }

    @Override
    public List<AnnotationLayer> getAnnotationLayers()
    {
        return annotationLayers;
    }

    @Override
    public void setAnnotationLayers(List<AnnotationLayer> aAnnotationLayers)
    {
        annotationLayers = aAnnotationLayers;
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
    public AnnotationLayer getRememberedArcLayer()
    {
        return rememberedArcLayer;
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
            for (FeatureState fm : aModels) {
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
    public int getFirstVisibleSentenceBegin()
    {
        return sentenceBeginOffset;
    }

    @Override
    public int getFirstVisibleSentenceEnd()
    {
        return sentenceEndOffset;
    }

    @Override
    public int getFocusSentenceNumber()
    {
        return focusSentenceNumber;
    }

    @Override
    public void setFocusSentenceNumber(int aSentenceNumber)
    {
        focusSentenceNumber = aSentenceNumber;
    }

    @Override
    public int getFirstVisibleSentenceNumber()
    {
        return firstVisibleSentenceNumber;
    }

    @Override
    public int getLastVisibleSentenceNumber()
    {
        return lastVisibleSentenceNumber;
    }
    
    @Override
    public int getNumberOfSentences()
    {
        return numberOfSentences;
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
    public boolean isForwardAnnotation()
    {
        return forwardAnnotation;
    }

    @Override
    public void setForwardAnnotation(boolean forwardAnnotation)
    {
        this.forwardAnnotation = forwardAnnotation;
    }

    @Override
    public void rememberFeatures()
    {
        if (getSelection().isRelationAnno()) {
            this.rememberedArcLayer = getSelectedAnnotationLayer();
            setRememberedArcFeatures(featureModels);
        }
        else {
            this.rememberedSpanLayer = getSelectedAnnotationLayer();
            setRememberedSpanFeatures(featureModels);
        }
    }
    
    @Override
    public void clearRememberedFeatures()
    {
        setRememberedArcFeatures(null);
        this.rememberedArcLayer = null;
        setRememberedSpanFeatures(null);
        this.rememberedSpanLayer = null;
    }

    @Override
    public void clearAllSelections()
    {
        getSelection().clear();
        clearArmedSlot();
    }

    private AnnotationFeature armedFeature;
    private int armedSlot = -1;

    @Override
    public void setArmedSlot(AnnotationFeature aName, int aIndex)
    {
        armedFeature = aName;
        armedSlot = aIndex;
    }

    @Override
    public boolean isArmedSlot(AnnotationFeature aName, int aIndex)
    {
        return ObjectUtils.equals(aName, armedFeature) && aIndex == armedSlot;
    }

    @Override
    public void clearArmedSlot()
    {
        armedFeature = null;
        armedSlot = -1;
    }

    @Override
    public boolean isSlotArmed()
    {
        return armedFeature != null;
    }

    @Override
    public AnnotationFeature getArmedFeature()
    {
        return armedFeature;
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
            if (f.feature.getId() == aFeature.getId()) {
                return f;
            }
        }
        return null;
    }
    
    @Override
    public TransientActionContext getAction()
    {
        return this;
    }
    
    // if it is annotation or delete operation
    private boolean isAnnotate = true;

    @Override
    public boolean isAnnotate()
    {
        return isAnnotate;
    }

    @Override
    public void setAnnotate(boolean isAnnotate)
    {
        this.isAnnotate = isAnnotate;
    }
}
