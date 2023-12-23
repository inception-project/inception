/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.inception.recommendation.api.event;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public class AjaxRecommendationRejectedEvent
{
    protected AjaxRequestTarget target;
    private AnnotatorState annotatorState;
    private VID vid;

    public AjaxRecommendationRejectedEvent(AjaxRequestTarget aTarget,
            AnnotatorState aAnnotatorState, VID aVid)
    {
        this.target = aTarget;
        this.annotatorState = aAnnotatorState;
        this.vid = aVid;
    }

    public AjaxRequestTarget getTarget()
    {
        return target;
    }

    public AnnotatorState getAnnotatorState()
    {
        return annotatorState;
    }

    public VID getVid()
    {
        return vid;
    }
}
