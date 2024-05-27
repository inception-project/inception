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
package de.tudarmstadt.ukp.inception.rendering.vmodel;

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.uima.cas.FeatureStructure;

public class VComment
    implements Serializable
{
    private static final long serialVersionUID = -1327907345192547101L;

    private final VID vid;
    private final VCommentType commentType;
    private final String comment;

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
        return new ToStringBuilder(this).append("vid", vid).append("commentType", commentType)
                .append("comment", comment).toString();
    }
}
