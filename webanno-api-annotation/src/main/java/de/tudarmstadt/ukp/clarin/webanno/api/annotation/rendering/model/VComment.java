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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.uima.cas.FeatureStructure;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class VComment
{
    private VID vid;
    private VCommentType commentType;
    private String comment;

    public VComment()
    {
        // Nothing to do
    }

    public VComment(FeatureStructure aFS, VCommentType aCommentType, String aComment)
    {
        this(new VID(getAddr(aFS)), aCommentType, aComment);
    }

    public VComment(VID aVid, VCommentType aCommentType, String aComment)
    {
        vid = aVid;
        commentType = aCommentType;
        comment = aComment;
    }

    public VID getVid()
    {
        return vid;
    }

    public VCommentType getCommentType()
    {
        return commentType;
    }

    public String getComment()
    {
        return comment;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
                .append("vid", vid)
                .append("commentType", commentType)
                .append("comment", comment)
                .toString();
    }
}
