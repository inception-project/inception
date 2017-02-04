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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation.action;

import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getLastSentenceInDisplayWindow;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel.FeatureModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil;
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
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Data model for the {@link BratAnnotator}
 */
public class ActionContext
    implements Serializable, AnnotatorState, TransientActionContext
{
    private static final long serialVersionUID = 1078613192789450714L;

    /**
     * The Project the annotator working on
     */
    private Project project;

    /**
     * The source document the to be annotated
     */
    private SourceDocument document;

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
     * the sentence number where an action occured (selection, modification, clicking)
     */
    private int focusSentenceNumber;
    /**
     * The first sentence number in the display window
     */
    private int fSN;
    /**
     * The last sentence number in the display window
     */
    private int lSN;

    /**
     * Constraints object from rule file
     */
    private ParsedConstraints constraints;

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

    // Annotation preferences, to be saved in a file system
    /**
     * The annotation layers available in the current project.
     */
    private List<AnnotationLayer> annotationLayers = new ArrayList<AnnotationLayer>();

    // /**
    // * The number of sentences to be displayed at a time
    // */
    // private int windowSize = 5;
    //
    // /**
    // * Used to enable/disable auto-scrolling while annotation
    // */
    // private boolean scrollPage = true;
    //
    // // determine if static color for annotations will be used or we shall
    // // dynamically generate one
    // private boolean staticColor = true;

    private AnnotationPreference preferences = new AnnotationPreference();

    /**
     * If the document is opened through the next/previous buttons on the annotation page, not with
     * the open dialog method, used to change {@link #document}
     */
    private String documentName;

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

    // Text field to capture key-bindings for forward annoatations
    private String forwardAnno;
    // the default annotation layer
    private AnnotationLayer defaultAnnotationLayer;

    // the name of the default annotation layer
    private String layerName;

    // enable automatic forward annotations
    private boolean forwardAnnotation;

    // User action while annotating on document
    private String userAction;

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
    public ScriptDirection getScriptDirection()
    {
        return scriptDirection;
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
    public void setDocument(SourceDocument aDocument)
    {
        document = aDocument;
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

        displayWindowStartSentenceAddress = aSentence.getAddress();
        this.sentenceBeginOffset = aSentence.getBegin();
        this.sentenceEndOffset = aSentence.getEnd();

        Sentence lastVisibleSentence = getLastSentenceInDisplayWindow(jcas, getAddr(aSentence),
                getPreferences().getWindowSize());
        this.fSN = BratAjaxCasUtil.getSentenceNumber(jcas, aSentence.getBegin());
        this.lSN = BratAjaxCasUtil.getSentenceNumber(jcas, lastVisibleSentence.getBegin());
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
    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    @Override
    public AnnotationLayer getRememberedSpanLayer()
    {
        return rememberedSpanLayer;
    }

    @Override
    public void setRememberedSpanLayer(AnnotationLayer rememberedSpanLayer)
    {
        this.rememberedSpanLayer = rememberedSpanLayer;
    }

    @Override
    public AnnotationLayer getRememberedArcLayer()
    {
        return rememberedArcLayer;
    }

    @Override
    public void setRememberedArcLayer(AnnotationLayer rememberedArcLayer)
    {
        this.rememberedArcLayer = rememberedArcLayer;
    }

    @Override
    public Map<AnnotationFeature, Serializable> getRememberedSpanFeatures()
    {
        return rememberedSpanFeatures;
    }

    @Override
    public void setRememberedSpanFeatures(List<FeatureModel> aModels)
    {
        rememberedSpanFeatures = new HashMap<>();
        if (aModels != null) {
            for (FeatureModel fm : aModels) {
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

    @Override
    public void setRememberedArcFeatures(List<FeatureModel> aModels)
    {
        rememberedArcFeatures = new HashMap<>();
        if (aModels != null) {
            for (FeatureModel fm : aModels) {
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
    public int getSentenceBeginOffset()
    {
        return sentenceBeginOffset;
    }

    @Override
    public int getSentenceEndOffset()
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
        return fSN;
    }

    @Override
    public int getLastVisibleSentenceNumber()
    {
        return lSN;
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
    public void clearRememberedFeatures()
    {
        setRememberedArcFeatures(null);
        setRememberedArcLayer(null);
        setRememberedSpanFeatures(null);
        setRememberedSpanLayer(null);
    }

    @Override
    public void initForDocument(JCas aJCas, RepositoryService aRepository)
    {
        getSelection().clear();
        clearArmedSlot();

        // (Re)initialize brat model after potential creating / upgrading CAS
        setFirstVisibleSentence(BratAjaxCasUtil.getFirstSentence(aJCas));
        getPreferences().setWindowSize(aRepository.getNumberOfSentences());
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
}
