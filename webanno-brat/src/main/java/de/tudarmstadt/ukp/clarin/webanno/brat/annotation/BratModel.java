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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel.FeatureModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * Data model for the {@link BratAnnotator}
 *
 * @author Seid Muhie Yimam
 *
 */
public class BratModel
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

    // Annotation preferences, to be saved in a file system
    /**
     * The annotation layers available in the current project.
     */
    private List<AnnotationLayer> annotationLayers = new ArrayList<AnnotationLayer>();

    /**
     * The number of sentences to be displayed at a time
     */
    private int windowSize = 5;

    /**
     * Used to enable/disable auto-scrolling while annotation
     */
    private boolean scrollPage = true;

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

    private boolean annotationCleared = false;

    // determine if static color for annotations will be used or we shall
    // dynamically generate one
    private boolean staticColor = true;

    // if it is annotation or delete operation
    private boolean isAnnotate;

    // the span id of the dependent in arc annotation
    private int originSpanId;

    // The type of the dependent in the arc annotation
    private String originSpanType;

    // The type of the governor in the arc annotation
    private String targetSpanType;

    // The span id of the governor in arc annotation
    private int targetSpanId;

    // selected span text
    private String selectedText;

    //id of the select annotation layer
    private VID selectedAnnotationId = VID.NONE_ID;

    // the selected annotation layer
    private AnnotationLayer selectedAnnotationLayer;

    // is the annotation span or arc annotation
    private boolean isRelationAnno;

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
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

    public void setUser(User user)
    {
        this.user = user;
    }

    public List<AnnotationLayer> getAnnotationLayers()
    {
        return annotationLayers;
    }

    public void setAnnotationLayers(List<AnnotationLayer> aAnnotationLayers)
    {
        annotationLayers = aAnnotationLayers;
    }

    public int getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
    }

    public boolean isScrollPage()
    {
        return scrollPage;
    }

    public void setScrollPage(boolean aScrollPage)
    {
        scrollPage = aScrollPage;
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
                rememberedArcFeatures.put(fm.feature, fm.value);
            }
        }
    }

    public boolean isAnnotationCleared()
    {
        return annotationCleared;
    }

    public void setAnnotationCleared(boolean annotationCleared)
    {
        this.annotationCleared = annotationCleared;
    }

    public boolean isStaticColor()
    {
        return staticColor;
    }

    public void setStaticColor(boolean staticColor)
    {
        this.staticColor = staticColor;
    }

    public boolean isAnnotate()
    {
        return isAnnotate;
    }

    public void setAnnotate(boolean isAnnotate)
    {
        this.isAnnotate = isAnnotate;
    }

    public int getOriginSpanId()
    {
        return originSpanId;
    }

    public void setOriginSpanId(int originSpanId)
    {
        this.originSpanId = originSpanId;
    }

    public String getOriginSpanType()
    {
        return originSpanType;
    }

    public void setOriginSpanType(String originSpanType)
    {
        this.originSpanType = originSpanType;
    }

    public String getTargetSpanType()
    {
        return targetSpanType;
    }

    public void setTargetSpanType(String targetSpanType)
    {
        this.targetSpanType = targetSpanType;
    }

    public int getTargetSpanId()
    {
        return targetSpanId;
    }

    public void setTargetSpanId(int targetSpanId)
    {
        this.targetSpanId = targetSpanId;
    }

    public String getSelectedText()
    {
        return selectedText;
    }

    public void setSelectedText(String selectedText)
    {
        this.selectedText = selectedText;
    }

    public VID getSelectedAnnotationId()
    {
        return selectedAnnotationId;
    }

    public void setSelectedAnnotationId(VID selectedAnnotationId)
    {
        this.selectedAnnotationId = selectedAnnotationId;
    }

    public AnnotationLayer getSelectedAnnotationLayer()
    {
        return selectedAnnotationLayer;
    }

    public void setSelectedAnnotationLayer(AnnotationLayer selectedAnnotationLayer)
    {
        this.selectedAnnotationLayer = selectedAnnotationLayer;
    }

    public boolean isRelationAnno()
    {
        return isRelationAnno;
    }

    public void setRelationAnno(boolean isRelationAnno)
    {
        this.isRelationAnno = isRelationAnno;
    }

    public void initForProject()
    {
        setRememberedArcFeatures(null);
        setRememberedArcLayer(null);
        setRememberedSpanFeatures(null);
        setRememberedSpanLayer(null);
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
