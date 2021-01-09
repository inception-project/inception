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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A model comprises of Curation Segments comprising of the begin and end of the sentences,
 * {@link SentenceState} Sentence number
 */
public class SourceListView
    implements Serializable
{
    private static final long serialVersionUID = 9219600871129699568L;
    // begin offset of sentences list, default is the begin of the document
    private Integer begin;
    // end offset of sentences list, default is end of the document
    private Integer end;

    // begin of the curation/suggestion sentences list
    private int curationBegin;
    // end of the curation/suggestion sentences list
    private int curationEnd;

    private SentenceState sentenceState;
    private Integer sentenceNumber;
    private boolean isCurrentSentence;

    private Map<String, Integer> sentenceAddress = new LinkedHashMap<>();

    public SourceListView()
    {

    }

    public Integer getBegin()
    {
        return begin;
    }

    public void setBegin(Integer begin)
    {
        this.begin = begin;
    }

    public Integer getEnd()
    {
        return end;
    }

    public void setEnd(Integer end)
    {
        this.end = end;
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

    public Map<String, Integer> getSentenceAddress()
    {
        return sentenceAddress;
    }

    public Boolean getHasDiff()
    {
        return sentenceState.hasDiff();
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

    public void setSentenceNumber(Integer sentenceNumber)
    {
        this.sentenceNumber = sentenceNumber;
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
