/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model;

// {"start":"","startOffset":2855,"end":"","endOffset":2863}
public class Range
{
    private String start;
    private int startOffset;
    private String end;
    private int endOffset;

    public Range()
    {
        // For Jackson deserialization
    }

    public Range(int aStartOffset, int aEndOffset)
    {
        startOffset = aStartOffset;
        endOffset = aEndOffset;
        start = "";
        end = "";
    }

    public String getStart()
    {
        return start;
    }

    public void setStart(String aStart)
    {
        start = aStart;
    }

    public int getStartOffset()
    {
        return startOffset;
    }

    public void setStartOffset(int aStartOffset)
    {
        startOffset = aStartOffset;
    }

    public String getEnd()
    {
        return end;
    }

    public void setEnd(String aEnd)
    {
        end = aEnd;
    }

    public int getEndOffset()
    {
        return endOffset;
    }

    public void setEndOffset(int aEndOffset)
    {
        endOffset = aEndOffset;
    }
}
