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
package de.tudarmstadt.ukp.inception.rendering.selection;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

/**
 * Fired by {@link AnnotatorState} if the parameters controlling the viewport have changed.
 * <p>
 * NOTE: This event is not fired when basic configurations affecting the viewport are changed, e.g.
 * the {@link AnnotatorState#getPagingStrategy() paging strategy}. It is also not called when the
 * active document or active project changes.
 *
 * @see AnnotatorState#setPageBegin
 * @see AnnotatorState#setVisibleUnits
 * @see AnnotatorState#setFirstVisibleUnit
 * @see AnnotatorState#getFirstVisibleUnitIndex()
 * @see AnnotatorState#getLastVisibleUnitIndex()
 * @see AnnotatorState#getUnitCount()
 * @see AnnotatorState#getWindowBeginOffset()
 * @see AnnotatorState#getWindowEndOffset()
 */
public class AnnotatorViewportChangedEvent
{

    private final AjaxRequestTarget requestHandler;

    public AnnotatorViewportChangedEvent(AjaxRequestTarget aRequestHandler)
    {
        requestHandler = aRequestHandler;
    }

    public AjaxRequestTarget getRequestHandler()
    {
        return requestHandler;
    }
}
