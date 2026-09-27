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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;

/**
 * Implemented by an {@link ActionBar} item that finishes or re-opens the document in its editor.
 * The page-wide keyboard shortcut in {@link ActionBarKeyBindingsPanel} is routed to the
 * implementation in the action bar of the active editor.
 */
public interface DocumentStateHandler
{
    /**
     * Finish the document or, if it is already finished, re-open it - whatever the workflow the
     * item belongs to understands by that. Does nothing if the item would not currently offer the
     * action.
     *
     * @param aTarget
     *            the AJAX request target
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionFinishOrReopenDocument(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException;
}
