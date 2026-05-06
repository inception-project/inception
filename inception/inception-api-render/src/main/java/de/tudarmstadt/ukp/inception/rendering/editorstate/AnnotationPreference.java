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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringStrategyType;
import de.tudarmstadt.ukp.inception.rendering.coloring.ReadonlyColoringStrategy;

/**
 * This is a class representing the bean objects to store users preference of annotation settings
 * such as annotation layers, number of sentence to display at a time, visibility of lemma and
 * whether to allow auto page scrolling.
 */
public class AnnotationPreference
    implements Serializable, ColoringPreferences
{
    private static final long serialVersionUID = 2202236699782758271L;

    // BEGIN: Settings related to editor choice
    private String editor;
    // END: Settings related to editor choice

    // BEGIN: Settings specific to the brat editor
    public static final int FONT_ZOOM_MIN = 10;
    public static final int FONT_ZOOM_MAX = 1000;
    public static final int FONT_ZOOM_DEFAULT = 100;
    private int fontZoom;
    private int windowSize;
    private boolean scrollPage = true;
    private boolean collapseArcs = false;
    // END: Settings specific to the brat editor

    // BEGIN: Settings specific to the AnnotationPage layout
    public static final int SIDEBAR_SIZE_MIN = 5;
    public static final int SIDEBAR_SIZE_MAX = 50;
    public static final int SIDEBAR_SIZE_DEFAULT = 20;
    private @Deprecated int sidebarSize;
    private int sidebarSizeLeft;
    private int sidebarSizeRight;
    // END: Settings specific to the AnnotationPage layout

    // BEGIN: Settings specific to the AnnotationDetailPanel
    private long defaultLayer = -1;
    // END: Settings specific to the AnnotationDetailPanel

    // BEGIN: Settings specific to layer visibility and coloring
    private @Deprecated List<Long> annotationLayers;
    private Set<Long> hiddenAnnotationLayerIds = new HashSet<>();
    private Set<Long> hiddenAnnotationFeatureIds = new HashSet<>();
    private Map<Long, Set<String>> hiddenTags = new HashMap<>();
    private Map<Long, ColoringStrategyType> colorPerLayer = new HashMap<>();
    private ReadonlyColoringStrategy readonlyLayerColoringBehaviour = ReadonlyColoringStrategy.LEGACY;
    private @Deprecated boolean staticColor = true;
    // END: Settings specific to layer visibility and coloring

    /**
     * @return the preferred annotation layers
     * 
     * @deprecated working with preferred layers is deprecated, use hidden layers instead
     */
    @Deprecated
    public List<Long> getAnnotationLayers()
    {
        return annotationLayers;
    }

    /**
     * @deprecated working with preferred layers is deprecated, use hidden layers instead
     * 
     * @param aAnnotationLayers
     *            the preferred annotation layers
     */
    @Deprecated()
    public void setAnnotationLayers(List<Long> aAnnotationLayers)
    {
        annotationLayers = aAnnotationLayers;
    }

    public void setLayerVisible(AnnotationLayer aLayer, boolean aVisible)
    {
        if (aVisible) {
            hiddenAnnotationLayerIds.remove(aLayer.getId());
        }
        else {
            hiddenAnnotationLayerIds.add(aLayer.getId());
        }
    }

    public Set<Long> getHiddenAnnotationLayerIds()
    {
        return hiddenAnnotationLayerIds;
    }

    public void setHiddenAnnotationLayerIds(Set<Long> aAnnotationLayerIds)
    {
        hiddenAnnotationLayerIds = aAnnotationLayerIds;
    }

    public void setFeatureVisible(AnnotationFeature aFeature, boolean aVisible)
    {
        if (aVisible) {
            hiddenAnnotationFeatureIds.remove(aFeature.getId());
        }
        else {
            hiddenAnnotationFeatureIds.add(aFeature.getId());
        }
    }

    public Set<Long> getHiddenAnnotationFeatureIds()
    {
        return hiddenAnnotationFeatureIds;
    }

    public void setHiddenAnnotationFeatureIds(Set<Long> aAnnotationFeatureIds)
    {
        hiddenAnnotationFeatureIds = aAnnotationFeatureIds;
    }

    public void setTagVisible(AnnotationFeature aFeature, Tag aTag, boolean aVisible)
    {
        var tags = hiddenTags.get(aFeature.getId());
        if (tags == null) {
            if (!aVisible) {
                tags = new HashSet<String>();
                tags.add(aTag.getName());
                hiddenTags.put(aFeature.getId(), tags);
            }
        }
        else {
            if (!aVisible) {
                tags.add(aTag.getName());
            }
            else {
                tags.remove(aTag.getName());
                if (tags.isEmpty()) {
                    hiddenTags.remove(aFeature.getId());
                }
            }
        }
    }

    public Map<Long, Set<String>> getHiddenTags()
    {
        return hiddenTags;
    }

    public void setHiddenTags(Map<Long, Set<String>> aTags)
    {
        hiddenTags = aTags;
    }

    /**
     * @return number of sentences to be displayed at a time
     */
    public int getWindowSize()
    {
        return Math.max(1, windowSize);
    }

    /**
     * @param aWindowSize
     *            number of sentences to be displayed at a time
     */
    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
    }

    /**
     * @return if auto-scrolling is enabled while annotating
     */
    public boolean isScrollPage()
    {
        return scrollPage;
    }

    /**
     * @param aScrollPage
     *            enable/disable auto-scrolling while annotation
     */
    public void setScrollPage(boolean aScrollPage)
    {
        scrollPage = aScrollPage;
    }

    @Override
    public Map<Long, ColoringStrategyType> getColorPerLayer()
    {
        return colorPerLayer;
    }

    public void setColorPerLayer(Map<Long, ColoringStrategyType> colorPerLayer)
    {
        this.colorPerLayer = colorPerLayer;
    }

    @Override
    public ReadonlyColoringStrategy getReadonlyLayerColoringBehaviour()
    {
        return readonlyLayerColoringBehaviour;
    }

    public void setReadonlyLayerColoringBehaviour(
            ReadonlyColoringStrategy readonlyLayerColoringBehaviour)
    {
        this.readonlyLayerColoringBehaviour = readonlyLayerColoringBehaviour;
    }

    /**
     * @deprecated this is only here to not break previous user settings, its not an option that can
     *             be set anymore and is also no longer used anywhere
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public boolean isStaticColor()
    {
        return staticColor;
    }

    @Deprecated
    public int getSidebarSize()
    {
        if (sidebarSize < SIDEBAR_SIZE_MIN || sidebarSize > SIDEBAR_SIZE_MAX) {
            return SIDEBAR_SIZE_DEFAULT;
        }
        else {
            return sidebarSize;
        }
    }

    @Deprecated
    public void setSidebarSize(int aSidebarSize)
    {
        if (aSidebarSize > SIDEBAR_SIZE_MAX) {
            sidebarSize = SIDEBAR_SIZE_MAX;
        }
        else if (aSidebarSize < SIDEBAR_SIZE_MIN) {
            sidebarSize = SIDEBAR_SIZE_MIN;
        }
        else {
            sidebarSize = aSidebarSize;
        }
    }

    public int getSidebarSizeLeft()
    {
        if (sidebarSizeLeft < SIDEBAR_SIZE_MIN || sidebarSizeLeft > SIDEBAR_SIZE_MAX) {
            return SIDEBAR_SIZE_DEFAULT;
        }
        else {
            return sidebarSizeLeft;
        }
    }

    public void setSidebarSizeLeft(int aSidebarSize)
    {
        if (aSidebarSize > SIDEBAR_SIZE_MAX) {
            sidebarSizeLeft = SIDEBAR_SIZE_MAX;
        }
        else if (aSidebarSize < SIDEBAR_SIZE_MIN) {
            sidebarSizeLeft = SIDEBAR_SIZE_MIN;
        }
        else {
            sidebarSizeLeft = aSidebarSize;
        }
    }

    public int getSidebarSizeRight()
    {
        if (sidebarSizeRight < SIDEBAR_SIZE_MIN || sidebarSizeRight > SIDEBAR_SIZE_MAX) {
            return SIDEBAR_SIZE_DEFAULT;
        }
        else {
            return sidebarSizeRight;
        }
    }

    public void setSidebarSizeRight(int aSidebarSize)
    {
        if (aSidebarSize > SIDEBAR_SIZE_MAX) {
            sidebarSizeRight = SIDEBAR_SIZE_MAX;
        }
        else if (aSidebarSize < SIDEBAR_SIZE_MIN) {
            sidebarSizeRight = SIDEBAR_SIZE_MIN;
        }
        else {
            sidebarSizeRight = aSidebarSize;
        }
    }

    public int getFontZoom()
    {
        if (fontZoom < FONT_ZOOM_MIN || fontZoom > FONT_ZOOM_MAX) {
            return FONT_ZOOM_DEFAULT;
        }
        else {
            return fontZoom;
        }
    }

    public void setFontZoom(int aFontZoom)
    {
        if (aFontZoom > FONT_ZOOM_MAX) {
            fontZoom = FONT_ZOOM_MAX;
        }
        else if (aFontZoom < FONT_ZOOM_MIN) {
            fontZoom = FONT_ZOOM_MIN;
        }
        else {
            fontZoom = aFontZoom;
        }
    }

    public String getEditor()
    {
        return editor;
    }

    public void setEditor(String aEditor)
    {
        editor = aEditor;
    }

    public void setDefaultLayer(long aLayerId)
    {
        defaultLayer = aLayerId;
    }

    public long getDefaultLayer()
    {
        return defaultLayer;
    }

    public boolean isCollapseArcs()
    {
        return collapseArcs;
    }

    public void setCollapseArcs(boolean aCollapseArcs)
    {
        collapseArcs = aCollapseArcs;
    }
}
