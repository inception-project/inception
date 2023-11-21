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
package de.tudarmstadt.ukp.clarin.webanno.brat.render.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

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

    public SentenceComment(VID aVid, int aSentenceIndex, String aCommentType, String aComment)
    {
        anchor = new Object[] { "sent", aSentenceIndex, aVid };
        commentType = aCommentType;
        comment = aComment;
    }

    public int getVid()
    {
        return (int) anchor[0];
    }

    public void getVid(VID aVid)
    {
        anchor[2] = aVid;
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
