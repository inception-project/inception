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
package de.tudarmstadt.ukp.inception.recogitojseditor.model;

public class WebAnnotationTextPositionSelector
    extends WebAnnotationSelector
{
    private int start;
    private int end;

    public WebAnnotationTextPositionSelector()
    {
        // Default
    }

    public WebAnnotationTextPositionSelector(int aStart, int aEnd)
    {
        start = aStart;
        end = aEnd;
    }

    public int getStart()
    {
        return start;
    }

    public void setStart(int aBegin)
    {
        start = aBegin;
    }

    public int getEnd()
    {
        return end;
    }

    public void setEnd(int aEnd)
    {
        end = aEnd;
    }
}
