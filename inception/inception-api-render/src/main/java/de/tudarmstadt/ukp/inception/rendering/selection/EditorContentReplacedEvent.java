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
 * Indicates when the content of an editor changes (i.e. a document is opened or the current
 * document is unloaded leaving it empty.
 * <p>
 * Note that related events like {@link DocumentOpenedEvent} do not indicate when a document is
 * unloaded, only when a document is opened.
 */
public class EditorContentReplacedEvent
    implements EditorBoundEvent
{
    private final AnnotatorViewState source;

    private final AjaxRequestTarget requestHandler;

    public EditorContentReplacedEvent(AnnotatorViewState aSource, AjaxRequestTarget aRequestHandler)
    {
        source = aSource;
        requestHandler = aRequestHandler;
    }

    /**
     * @return the editor state whose content was replaced, or {@code null} if unknown.
     */
    @Override
    public AnnotatorViewState getSource()
    {
        return source;
    }

    /**
     * @return the current AJAX target, or {@code null} if the replacement happened outside an AJAX
     *         request (e.g. during initial page rendering).
     */
    public AjaxRequestTarget getRequestHandler()
    {
        return requestHandler;
    }
}
