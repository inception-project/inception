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
package de.tudarmstadt.ukp.inception.revieweditor.event;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public class SelectAnnotationEvent
{

    private final VID vid;

    private final AjaxRequestTarget target;

    private final int begin;

    private final int end;

    public SelectAnnotationEvent(VID aVid, int aBegin, int aEnd, AjaxRequestTarget aTarget)
    {
        vid = aVid;
        begin = aBegin;
        end = aEnd;
        target = aTarget;
    }

    public VID getVid()
    {
        return vid;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public AjaxRequestTarget getTarget()
    {
        return target;
    }
}
