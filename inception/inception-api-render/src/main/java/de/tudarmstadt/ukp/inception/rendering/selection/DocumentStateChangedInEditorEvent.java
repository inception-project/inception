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
 * Indicates that the state of the document shown in an editor has changed - it was finished,
 * re-opened, or moved between curation states - while the document itself stayed put.
 * <p>
 * Components should check {@link #isFor} rather than react to any editor's transition, or a
 * transition in one pane redraws them against another pane's document.
 * <p>
 * The document is unchanged, so this is not an {@link EditorContentReplacedEvent}: consumers that
 * merely need to re-read whether the document may still be edited want this event, and consumers
 * that hold data read out of the CAS do not need to discard it.
 */
public class DocumentStateChangedInEditorEvent
    implements EditorBoundEvent
{
    private final AnnotatorViewState source;

    private final AjaxRequestTarget requestHandler;

    public DocumentStateChangedInEditorEvent(AnnotatorViewState aSource,
            AjaxRequestTarget aRequestHandler)
    {
        source = aSource;
        requestHandler = aRequestHandler;
    }

    @Override
    public AnnotatorViewState getSource()
    {
        return source;
    }

    /**
     * @return the current AJAX target, or {@code null} if the transition happened outside an AJAX
     *         request.
     */
    public AjaxRequestTarget getRequestHandler()
    {
        return requestHandler;
    }
}
