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
package de.tudarmstadt.ukp.clarin.webanno.brat.render.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.clarin.webanno.brat.message.BeanAsArraySerializer;

/**
 * Sentence-level comment.
 */
@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "anchor", "commentType", "comment" })
public class SentenceComment
    implements Comment
{
    private Object[] anchor;
    private String commentType;
    private String comment;

    public SentenceComment()
    {
        // Nothing to do
    }

    public SentenceComment(int aSentenceIndex, String aCommentType, String aComment)
    {
        anchor = new Object[] { "sent", aSentenceIndex };
        commentType = aCommentType;
        comment = aComment;
    }

    public int getSentenceIndex()
    {
        return (int) anchor[1];
    }

    public void setSentenceIndex(int aSentenceIndex)
    {
        anchor[1] = aSentenceIndex;
    }

    @Override
    public String getCommentType()
    {
        return commentType;
    }

    @Override
    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }
}
