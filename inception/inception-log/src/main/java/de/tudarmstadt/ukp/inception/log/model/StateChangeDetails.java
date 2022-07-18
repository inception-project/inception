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
package de.tudarmstadt.ukp.inception.log.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class StateChangeDetails
{
    private String state;
    private String previousState;
    private String annotatorState;
    private String annotatorComment;

    public StateChangeDetails()
    {
        // Nothing to do
    }

    public StateChangeDetails(String aState, String aPreviousState)
    {
        state = aState;
        previousState = aPreviousState;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String aState)
    {
        state = aState;
    }

    public String getPreviousState()
    {
        return previousState;
    }

    public void setPreviousState(String aPreviousState)
    {
        previousState = aPreviousState;
    }

    public void setAnnotatorComment(String aAnnotatorComment)
    {
        annotatorComment = aAnnotatorComment;
    }

    public String getAnnotatorComment()
    {
        return annotatorComment;
    }

    public void setAnnotatorState(String aAnnotatorState)
    {
        annotatorState = aAnnotatorState;
    }

    public String getAnnotatorState()
    {
        return annotatorState;
    }
}
