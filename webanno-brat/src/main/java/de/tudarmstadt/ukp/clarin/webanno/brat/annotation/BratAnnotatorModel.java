/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectSentenceAt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.command.Selection;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel.FeatureModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
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
 *
 *
 */
public class BratAnnotatorModel
    implements Serializable
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
     * The very last sentence address in its UIMA annotation
     */
    private int lastSentenceAddress;

    /**
     * The very first sentence address in its UIMA annotation
     */
    private int firstSentenceAddress;

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
    private int sentenceNumber;
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

    public ParsedConstraints getConstraints()
    {
        return constraints;
    }

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
    
    //User action while annotating on document
    private String userAction;

    public String getUserAction()
    {
        return userAction;
    }

    public void setUserAction(String aUserAction)
    {
        userAction = aUserAction;
    }

    public void clearUserAction()
    {
        userAction = null;
    }

    private final Selection selection = new Selection();

    public Selection getSelection()
    {
        return selection;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
        setScriptDirection(project.getScriptDirection());
    }

    public ScriptDirection getScriptDirection()
    {
        return scriptDirection;
    }

    public void setScriptDirection(ScriptDirection aScriptDirection)
    {
        scriptDirection = aScriptDirection;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public void setDocument(SourceDocument aDocument)
    {
        document = aDocument;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser(User aUser)
    {
        user = aUser;
    }

    public int getSentenceAddress()
    {
        return displayWindowStartSentenceAddress;
    }

    public void setSentenceAddress(int aSentenceAddress)
    {
        displayWindowStartSentenceAddress = aSentenceAddress;
    }

    public int getLastSentenceAddress()
    {
        return lastSentenceAddress;
    }

    public void setLastSentenceAddress(int aLastSentenceAddress)
    {
        lastSentenceAddress = aLastSentenceAddress;
    }

    public int getFirstSentenceAddress()
    {
        return firstSentenceAddress;
    }

    public void setFirstSentenceAddress(int aFirstSentenceAddress)
    {
        firstSentenceAddress = aFirstSentenceAddress;
    }

    public List<AnnotationLayer> getAnnotationLayers()
    {
        return annotationLayers;
    }

    public void setAnnotationLayers(List<AnnotationLayer> aAnnotationLayers)
    {
        annotationLayers = aAnnotationLayers;
    }

    public AnnotationPreference getPreferences()
    {
        return preferences;
    }

    public void setPreferences(AnnotationPreference aPreferences)
    {
        preferences = aPreferences;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public void setDocumentName(String documentName)
    {
        this.documentName = documentName;
    }

    public Mode getMode()
    {
        return mode;
    }

    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    public AnnotationLayer getRememberedSpanLayer()
    {
        return rememberedSpanLayer;
    }

    public void setRememberedSpanLayer(AnnotationLayer rememberedSpanLayer)
    {
        this.rememberedSpanLayer = rememberedSpanLayer;
    }

    public AnnotationLayer getRememberedArcLayer()
    {
        return rememberedArcLayer;
    }

    public void setRememberedArcLayer(AnnotationLayer rememberedArcLayer)
    {
        this.rememberedArcLayer = rememberedArcLayer;
    }

    public Map<AnnotationFeature, Serializable> getRememberedSpanFeatures()
    {
        return rememberedSpanFeatures;
    }

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

    public Map<AnnotationFeature, Serializable> getRememberedArcFeatures()
    {
        return rememberedArcFeatures;
    }

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

    public int getSentenceBeginOffset()
    {
        return sentenceBeginOffset;
    }

    public void setSentenceBeginOffset(int sentenceBeginOffset)
    {
        this.sentenceBeginOffset = sentenceBeginOffset;
    }

    public int getSentenceEndOffset()
    {
        return sentenceEndOffset;
    }

    public void setSentenceEndOffset(int sentenceEndOffset)
    {
        this.sentenceEndOffset = sentenceEndOffset;
    }

    public int getSentenceNumber()
    {
        return sentenceNumber;
    }

    public void setSentenceNumber(int sentenceNumber)
    {
        this.sentenceNumber = sentenceNumber;
    }

    public int getFSN()
    {
        return fSN;
    }

    public void setFSN(int fSN)
    {
        this.fSN = fSN;
    }

    public int getLSN()
    {
        return lSN;
    }

    public void setLSN(int lSN)
    {
        this.lSN = lSN;
    }

    public AnnotationLayer getSelectedAnnotationLayer()
    {
        return selectedAnnotationLayer;
    }

    public void setSelectedAnnotationLayer(AnnotationLayer selectedAnnotationLayer)
    {
        this.selectedAnnotationLayer = selectedAnnotationLayer;
    }

    public AnnotationLayer getDefaultAnnotationLayer()
    {
        return defaultAnnotationLayer;
    }

    public void setDefaultAnnotationLayer(AnnotationLayer defaultAnnotationLayer)
    {
        this.defaultAnnotationLayer = defaultAnnotationLayer;
    }

    public boolean isForwardAnnotation()
    {
        return forwardAnnotation;
    }

    public void setForwardAnnotation(boolean forwardAnnotation)
    {
        this.forwardAnnotation = forwardAnnotation;
    }

    public void initForProject()
    {
        setRememberedArcFeatures(null);
        setRememberedArcLayer(null);
        setRememberedSpanFeatures(null);
        setRememberedSpanLayer(null);
    }

    public void initForDocument(JCas aJCas, RepositoryService aRepository)
    {
        getSelection().clear();
        clearArmedSlot();
        
        // (Re)initialize brat model after potential creating / upgrading CAS
        setSentenceAddress(BratAjaxCasUtil.getFirstSentenceAddress(aJCas));
        setFirstSentenceAddress(BratAjaxCasUtil.getFirstSentenceAddress(aJCas));
        setLastSentenceAddress(BratAjaxCasUtil.getLastSentenceAddress(aJCas));
        getPreferences().setWindowSize(aRepository.getNumberOfSentences());

        Sentence sentence = selectByAddr(aJCas, Sentence.class, getSentenceAddress());
        setSentenceBeginOffset(sentence.getBegin());
        setSentenceEndOffset(sentence.getEnd());

        Sentence firstSentence = selectSentenceAt(aJCas, getSentenceBeginOffset(),
                getSentenceEndOffset());
        int lastAddressInPage = getLastSentenceAddressInDisplayWindow(aJCas,
                getAddr(firstSentence), getPreferences().getWindowSize());
        // the last sentence address in the display window
        Sentence lastSentenceInPage = (Sentence) selectByAddr(aJCas, FeatureStructure.class,
                lastAddressInPage);
        setFSN(BratAjaxCasUtil.getSentenceNumber(aJCas, firstSentence.getBegin()));
        setLSN(BratAjaxCasUtil.getSentenceNumber(aJCas, lastSentenceInPage.getBegin()));

        // LOG.debug("Configured BratAnnotatorModel for user [" + username + "] f:["
        // + getFirstSentenceAddress() + "] l:["
        // + getLastSentenceAddress() + "] s:["
        // + getSentenceAddress() + "]");
    }

    private AnnotationFeature armedFeature;
    private int armedSlot = -1;

    public void setArmedSlot(AnnotationFeature aName, int aIndex)
    {
        armedFeature = aName;
        armedSlot = aIndex;
    }

    public boolean isArmedSlot(AnnotationFeature aName, int aIndex)
    {
        return ObjectUtils.equals(aName, armedFeature) && aIndex == armedSlot;
    }

    public void clearArmedSlot()
    {
        armedFeature = null;
        armedSlot = -1;
    }

    public boolean isSlotArmed()
    {
        return armedFeature != null;
    }

    public AnnotationFeature getArmedFeature()
    {
        return armedFeature;
    }

    public int getArmedSlot()
    {
        return armedSlot;
    }
}
