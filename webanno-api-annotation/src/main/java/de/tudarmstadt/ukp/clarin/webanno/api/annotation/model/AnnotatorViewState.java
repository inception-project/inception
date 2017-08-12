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

import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public interface AnnotatorViewState
    extends Serializable
{
    // ---------------------------------------------------------------------------------------------
    // Window of visible annotations
    // ---------------------------------------------------------------------------------------------

    // REC: sentenceNumber/sentenceAddress can probably be dropped in favor of
    // firstSentenceNumber/firstSentenceAddress?

    /**
     * @param aUnit
     *            the first unit in the display window.
     */
    void setFirstVisibleUnit(Sentence aUnit);

    /**
     * @param aIndex
     *            the 1-based index of the focus unit
     */
    void setFocusUnitIndex(int aIndex);

    /**
     * @return the 1-based index of the focus unit
     */
    int getFocusUnitIndex();

    /**
     * @return the UIMA address of the first unit in the display window.
     */
    int getFirstVisibleUnitAddress();

    /**
     * @return the begin character offset of the first unit in the display window.
     */
    int getFirstVisibleUnitBegin();

    /**
     * @return the end character offset of the first unit in the display window.
     */
    int getFirstVisibleUnitEnd();

    /**
     * @return the index of the first unit in the display window.
     */
    int getFirstVisibleUnitIndex();

    /**
     * @return the index of the last unit in the display window.
     */
    int getLastVisibleUnitIndex();

    /**
     * @return the total number of units in the document.
     */
    int getUnitCount();

    /**
     * @return the begin character offset of the first unit in the display window.
     */
    int getWindowBeginOffset();

    /**
     * @return the end character offset of the last unit in the display window.
     */
    int getWindowEndOffset();
    
    // ---------------------------------------------------------------------------------------------
    // Rendering
    // - script direction can be changed by the user at will - it defaults to the direction
    // configured in the project
    // ---------------------------------------------------------------------------------------------
    ScriptDirection getScriptDirection();

    void setScriptDirection(ScriptDirection aScriptDirection);

    void toggleScriptDirection();
}
