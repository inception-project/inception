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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState;

/**
 * A model comprises of Curation Segments comprising of the begin and end of the unit,
 * {@link CasDiffSummaryState} unit number
 */
public class CurationUnit
    implements Serializable
{
    private static final long serialVersionUID = 9219600871129699568L;

    // begin/end offset of unit list, default is the begin of the document
    private final int begin;
    private final int end;
    private final int unitIndex;

    private CasDiffSummaryState state;

    private boolean isCurrentUnit;

    public CurationUnit(int aBegin, int aEnd, int aUnitIndex)
    {
        begin = aBegin;
        end = aEnd;
        unitIndex = aUnitIndex;
    }

    public Integer getBegin()
    {
        return begin;
    }

    public Integer getEnd()
    {
        return end;
    }

    public CasDiffSummaryState getState()
    {
        return state;
    }

    public void setState(CasDiffSummaryState sentenceState)
    {
        this.state = sentenceState;
    }

    public Integer getUnitIndex()
    {
        return unitIndex;
    }

    public boolean isCurrentUnit()
    {
        return isCurrentUnit;
    }

    public void setCurrentUnit(boolean isCurrentSentence)
    {
        this.isCurrentUnit = isCurrentSentence;
    }
}
