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
package de.tudarmstadt.ukp.inception.rendering.editorstate;

/**
 * An event that carries the {@link AnnotatorViewState editor state} it originated from.
 * <p>
 * A page may host more than one editor over the same document (e.g. the main editor plus a
 * read-only reference-document viewer, #6146). Consumers that are bound to a particular editor must
 * therefore ignore events originating from another editor's state. Use {@link #isFor} at the top of
 * an {@code @OnEvent} handler to fail fast on foreign events rather than hand-comparing
 * {@link #getSource()}.
 */
public interface EditorBoundEvent
{
    /**
     * @return the editor state this event originated from, or {@code null} if unknown.
     */
    AnnotatorViewState getSource();

    /**
     * @param aState
     *            the editor state to test against.
     * @return whether this event originated from the given editor state.
     */
    default boolean isFor(AnnotatorViewState aState)
    {
        return getSource() == aState;
    }
}
