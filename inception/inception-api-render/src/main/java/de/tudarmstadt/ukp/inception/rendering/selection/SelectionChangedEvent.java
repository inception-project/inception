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

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorViewState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.EditorBoundEvent;

/**
 * Fired when the selection of an editor changes. Carries the originating editor state as
 * {@link #getSource() source} so consumers can react only to their own editor (cf. #6146).
 */
public class SelectionChangedEvent
    implements EditorBoundEvent
{
    private final AnnotatorViewState source;

    private final AjaxRequestTarget requestHandler;

    public SelectionChangedEvent(AnnotatorViewState aSource, AjaxRequestTarget aRequestHandler)
    {
        source = aSource;
        requestHandler = aRequestHandler;
    }

    /**
     * @return the editor state whose selection changed, or {@code null} if unknown.
     */
    public AnnotatorViewState getSource()
    {
        return source;
    }

    public AjaxRequestTarget getRequestHandler()
    {
        return requestHandler;
    }
}
