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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model;

/**
 * An ennumeration to differentiate sentences in a document with different colors so as to easily
 * identify
 * 
 * @author Andreas Straninger
 */
public enum SentenceState
{
    /**
     * No conflicts of annotation in this sentence, no color - null- white
     */
    AGREE(false, null),
    /**
     * Conflicts of annotation found in this sentence, mark background in red
     */
    DISAGREE(true, "#FF9999"),
    /**
     * Curator resolved conflicts - mark background in yellow
     */
    RESOLVED(true, "#FFFF99"),
    /**
     *
     */
    CONFIRMED(true, "#99FF99");

    private boolean hasDiff;
    private String colorCode;

    private SentenceState(boolean aHasDiff, String aColorCode)
    {
        hasDiff = aHasDiff;
        colorCode = aColorCode;
    }

    public boolean hasDiff()
    {
        return hasDiff;
    }

    public String getColorCode()
    {
        return colorCode;
    }
}
