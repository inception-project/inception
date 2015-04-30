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
import java.util.List;

/**
 * This is a class representing the bean objects to store users preference of annotation settings
 * such as annotation layers, number of sentence to display at a time, visibility of lemma and
 * whether to allow auto page scrolling.
 *
 * @author Seid Muhie Yimam
 */
public class AnnotationPreference
    implements Serializable
{
    private static final long serialVersionUID = 2202236699782758271L;
    
    // Id of annotation layers, to be stored in the properties file comma separated: 12, 34,....
    private List<Long> annotationLayers;

    private int windowSize = 5;
    
    private boolean scrollPage = true;

    // determine if static color for annotations will be used or we shall
    // dynamically generate one
    private boolean staticColor = true;

    public List<Long> getAnnotationLayers()
    {
        return annotationLayers;
    }

    public void setAnnotationLayers(List<Long> aAnnotationLayers)
    {
        annotationLayers = aAnnotationLayers;
    }

    /**
     * The number of sentences to be displayed at a time
     */
    public int getWindowSize()
    {
        return windowSize;
    }

    /**
     * The number of sentences to be displayed at a time
     */
    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
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

    public boolean isStaticColor()
    {
        return staticColor;
    }

    public void setStaticColor(boolean staticColor)
    {
        this.staticColor = staticColor;
    }

}
