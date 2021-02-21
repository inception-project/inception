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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model;

import java.io.Serializable;

/**
 * A model comprises of Curation Segments comprising of the begin and end of the sentences,
 * {@link SentenceState} Sentence number
 */
public class SentenceInfo
    implements Serializable
{
    private static final long serialVersionUID = 9219600871129699568L;

    // begin/end offset of sentences list, default is the begin of the document
    private final Integer begin;
    private final Integer end;
    private final Integer sentenceNumber;

    // begin of the curation/suggestion sentences list
    private int curationBegin;
    // end of the curation/suggestion sentences list
    private int curationEnd;

    private SentenceState sentenceState;

    private boolean isCurrentSentence;

    public SentenceInfo()
    {
        begin = null;
        end = null;
        sentenceNumber = null;
    }

    public SentenceInfo(int aBegin, int aEnd, int aUnitIndex)
    {
        begin = aBegin;
        end = aEnd;
        sentenceNumber = aUnitIndex;
    }

    public Integer getBegin()
    {
        return begin;
    }

    public Integer getEnd()
    {
        return end;
    }

    public int getCurationBegin()
    {
        return curationBegin;
    }

    public void setCurationBegin(int curationBegin)
    {
        this.curationBegin = curationBegin;
    }

    public int getCurationEnd()
    {
        return curationEnd;
    }

    public void setCurationEnd(int curationEnd)
    {
        this.curationEnd = curationEnd;
    }

    public SentenceState getSentenceState()
    {
        return sentenceState;
    }

    public void setSentenceState(SentenceState sentenceState)
    {
        this.sentenceState = sentenceState;
    }

    public Integer getSentenceNumber()
    {
        return sentenceNumber;
    }

    public boolean isCurrentSentence()
    {
        return isCurrentSentence;
    }

    public void setCurrentSentence(boolean isCurrentSentence)
    {
        this.isCurrentSentence = isCurrentSentence;
    }
}
