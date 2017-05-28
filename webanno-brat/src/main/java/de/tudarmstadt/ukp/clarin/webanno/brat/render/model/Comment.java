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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.BeanAsArraySerializer;

/**
 * Use this "comments" to highlight "yield" of relation nodes
 *
 */
@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "vid", "commentType", "comment" })
public class Comment
{
    public static final String ANNOTATION_ERROR = "AnnotationError";
    public static final String ANNOTATION_INCOMPLETE = "AnnotationIncomplete";
    public static final String ANNOTATION_UNCONFIRMED = "AnnotationUnconfirmed";
    public static final String ANNOTATION_WARNING = "AnnotationWarning";
    public static final String ANNOTATOR_NOTES = "AnnotatorNotes";
    public static final String MISSING_ANNOTATION = "MissingAnnotation";
    public static final String CHANGED_ANNOTATION = "ChangedAnnotation";
    public static final String NORMALIZED = "Normalized";
    public static final String TRUE_POSITIVE = "True_positive";
    public static final String FALSE_POSITIVE = "False_positive";
    public static final String FALSE_NEGATIVE = "False_negative";
    
    private VID vid;
    private String commentType;
    private String comment;

    public Comment()
    {
        // Nothing to do
    }

    public Comment(int aId, String aCommentType, String aComment)
    {
        this(new VID(aId), aCommentType, aComment);
    }

    public Comment(VID aVid, String aCommentType, String aComment)
    {
        vid = aVid;
        commentType = aCommentType;
        comment = aComment;
    }

    @Deprecated
    public int getId()
    {
        return vid.getId();
    }

    @Deprecated
    public void setId(int aId)
    {
        vid = new VID(aId);
    }

    public VID getVid()
    {
        return vid;
    }

    public void setVid(VID aVid)
    {
        vid = aVid;
    }

    public String getCommentType()
    {
        return commentType;
    }

    public void setCommentType(String commentType)
    {
        this.commentType = commentType;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }
}
