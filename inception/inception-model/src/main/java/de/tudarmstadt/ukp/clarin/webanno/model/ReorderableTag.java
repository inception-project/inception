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
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.io.Serializable;
import java.util.Objects;

public class ReorderableTag
    implements Serializable
{
    private static final long serialVersionUID = -4720695709156570355L;

    private final ImmutableTag tag;
    private boolean reordered;
    private String score;

    public ReorderableTag(ImmutableTag aTag)
    {
        tag = aTag;
    }

    public ReorderableTag(String aName, String aDescription)
    {
        tag = new ImmutableTag(aName, aDescription);
    }

    public ReorderableTag(String aName, boolean aReordered)
    {
        this(aName, null);
        setReordered(aReordered);
    }

    public long getId()
    {
        return tag.getId();
    }

    public String getName()
    {
        return tag.getName();
    }

    public String getDescription()
    {
        return tag.getDescription();
    }

    public void setReordered(boolean aB)
    {
        reordered = aB;
    }

    public boolean getReordered()
    {
        return reordered;
    }

    @Override
    public String toString()
    {
        return tag.getName();
    }

    public String getScore()
    {
        return score;
    }

    public void setScore(String aScore)
    {
        score = aScore;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ReorderableTag)) {
            return false;
        }
        ReorderableTag castOther = (ReorderableTag) other;
        return Objects.equals(tag, castOther.tag);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(tag);
    }
}
