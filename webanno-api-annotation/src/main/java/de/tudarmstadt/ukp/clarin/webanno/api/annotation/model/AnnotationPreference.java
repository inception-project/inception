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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy.ColoringStrategyType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy.ReadonlyColoringBehaviour;

/**
 * This is a class representing the bean objects to store users preference of annotation settings
 * such as annotation layers, number of sentence to display at a time, visibility of lemma and
 * whether to allow auto page scrolling.
 *
 */
public class AnnotationPreference
    implements Serializable
{
    private static final long serialVersionUID = 2202236699782758271L;

    public static final int FONT_ZOOM_MIN = 10;
    public static final int FONT_ZOOM_MAX = 1000;
    public static final int FONT_ZOOM_DEFAULT = 100;
    
    public static final int SIDEBAR_SIZE_MIN = 10;
    public static final int SIDEBAR_SIZE_MAX = 50;
    public static final int SIDEBAR_SIZE_DEFAULT = 20;
    
    // Id of annotation layers, to be stored in the properties file comma separated: 12, 34,....
    @Deprecated
    private List<Long> annotationLayers;
    
    // Id of annotation layers, to be stored in the properties file comma separated: 12, 34,....
    private Set<Long> hiddenAnnotationLayerIds = new HashSet<>();

    private long defaultLayer = -1;
    
    private int windowSize;

    private int curationWindowSize = 10;

    private boolean scrollPage = true;
    
    // if a default layer is to be set
    private boolean rememberLayer;

    // // determine if static color for annotations will be used or we shall
    // // dynamically generate one
    @Deprecated
    private boolean staticColor = true; // this is only here to not break previous user settings,
                                        // its not an option that can be set anymore

    private Map<Long, ColoringStrategyType> colorPerLayer = new HashMap<>();

    private ReadonlyColoringBehaviour readonlyLayerColoringBehaviour = 
            ReadonlyColoringBehaviour.LEGACY;

    private int sidebarSize;
    private int fontZoom;
    
    private String editor;

    /**
     * working with preferred layers is deprecated, use hidden layers instead
     * @return
     */
    @Deprecated
    public List<Long> getAnnotationLayers()
    {
        return annotationLayers;
    }
    
    /**
     * working with preferred layers is deprecated, use hidden layers instead 
     * 
     * @param aAnnotationLayers
     */
    @Deprecated()
    public void setAnnotationLayers(List<Long> aAnnotationLayers)
    {
        annotationLayers = aAnnotationLayers;
    }
    
    public Set<Long> getHiddenAnnotationLayerIds()
    {
        return hiddenAnnotationLayerIds;
    }
    
    public void setHiddenAnnotationLayerIds(Set<Long> aAnnotationLayerIds)
    {
        hiddenAnnotationLayerIds = aAnnotationLayerIds;
    }

    /**
     * The number of sentences to be displayed at a time
     */
    public int getWindowSize()
    {
        return Math.max(1, windowSize);
    }

    /**
     * The number of sentences to be displayed at a time
     */
    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
    }

    /**
     * Get the number of sentences curation window display at the left side.
     */
    public int getCurationWindowSize()
    {
        return curationWindowSize;
    }

    /**
     * set the number of sentences curation window display at the left side
     *
     */
    public void setCurationWindowSize(int curationWindowSize)
    {
        this.curationWindowSize = curationWindowSize;
    }

    /**
     * Used to enable/disable auto-scrolling while annotation
     */
    public boolean isScrollPage()
    {
        return scrollPage;
    }

    /**
     * Used to enable/disable auto-scrolling while annotation
     */
    public void setScrollPage(boolean aScrollPage)
    {
        scrollPage = aScrollPage;
    }

    public boolean isRememberLayer()
    {
        return rememberLayer;
    }

    public void setRememberLayer(boolean aRememberLayer)
    {
        rememberLayer = aRememberLayer;
    }

    public Map<Long, ColoringStrategyType> getColorPerLayer()
    {
        return colorPerLayer;
    }

    public void setColorPerLayer(Map<Long, ColoringStrategyType> colorPerLayer)
    {
        this.colorPerLayer = colorPerLayer;
    }

    public ReadonlyColoringBehaviour getReadonlyLayerColoringBehaviour()
    {
        return readonlyLayerColoringBehaviour;
    }

    public void setReadonlyLayerColoringBehaviour(
            ReadonlyColoringBehaviour readonlyLayerColoringBehaviour)
    {
        this.readonlyLayerColoringBehaviour = readonlyLayerColoringBehaviour;
    }

    @Deprecated
    public boolean isStaticColor()
    {
        return staticColor;
    }

    public int getSidebarSize()
    {
        if (sidebarSize < SIDEBAR_SIZE_MIN || sidebarSize > SIDEBAR_SIZE_MAX) {
            return SIDEBAR_SIZE_DEFAULT;
        }
        else {
            return sidebarSize;
        }
    }

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
}
