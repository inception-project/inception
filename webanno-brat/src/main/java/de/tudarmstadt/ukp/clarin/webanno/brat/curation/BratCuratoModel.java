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
package de.tudarmstadt.ukp.clarin.webanno.brat.curation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;

import java.io.Serializable;

import org.apache.commons.lang.ObjectUtils;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Data model for the {@link BratAnnotator}
 *
 * @author Seid Muhie Yimam
 *
 */
public class BratCuratoModel
    extends BratModel
    implements Serializable
{
    private static final long serialVersionUID = 1078613192789450714L;

    /**
     * The current user annotating the document
     */
    private User user;

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

    // the begin offset of a span annotation
    private int beginOffset;

    // the end offset of a span annotation
    private int endOffset;

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

    public int getBeginOffset()
    {
        return beginOffset;
    }

    public void setBeginOffset(int beginOffset)
    {
        this.beginOffset = beginOffset;
    }

    public int getEndOffset()
    {
        return endOffset;
    }

    public void setEndOffset(int endOffset)
    {
        this.endOffset = endOffset;
    }

    public void initForDocument(JCas aJCas)
    {
        // (Re)initialize brat model after potential creating / upgrading CAS
        setSentenceAddress(BratAjaxCasUtil.getFirstSentenceAddress(aJCas));
        setFirstSentenceAddress(getSentenceAddress());
        setLastSentenceAddress(BratAjaxCasUtil.getLastSentenceAddress(aJCas));
        setWindowSize(5);

        Sentence sentence = selectByAddr(aJCas, Sentence.class, getSentenceAddress());
        setSentenceBeginOffset(sentence.getBegin());
        setSentenceEndOffset(sentence.getEnd());
    }

    private AnnotationFeature armedFeature;
    private int armedSlot = -1;

    @Override
    public void setArmedSlot(AnnotationFeature aName, int aIndex)
    {
        armedFeature = aName;
        armedSlot = aIndex;
    }

    @Override
    public boolean isArmedSlot(AnnotationFeature aName, int aIndex)
    {
        return ObjectUtils.equals(aName, armedFeature) && aIndex == armedSlot;
    }

    @Override
    public void clearArmedSlot()
    {
        armedFeature = null;
        armedSlot = -1;
    }

    @Override
    public boolean isSlotArmed()
    {
        return armedFeature != null;
    }

    @Override
    public AnnotationFeature getArmedFeature()
    {
        return armedFeature;
    }

    @Override
    public int getArmedSlot()
    {
        return armedSlot;
    }
}
