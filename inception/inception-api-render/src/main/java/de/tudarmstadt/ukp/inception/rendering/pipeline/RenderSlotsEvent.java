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
package de.tudarmstadt.ukp.inception.rendering.pipeline;

import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorViewState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.EditorBoundEvent;

public class RenderSlotsEvent
    implements EditorBoundEvent
{
    private final AnnotatorViewState source;

    private final IPartialPageRequestHandler requestHandler;

    public RenderSlotsEvent(AnnotatorViewState aSource, IPartialPageRequestHandler aRequestHandler)
    {
        source = aSource;
        requestHandler = aRequestHandler;
    }

    /**
     * @return the editor state whose slots are being (re-)rendered, or {@code null} if unknown.
     */
    public AnnotatorViewState getSource()
    {
        return source;
    }

    public IPartialPageRequestHandler getRequestHandler()
    {
        return requestHandler;
    }
}
