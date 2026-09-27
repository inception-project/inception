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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.url;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.wicket.util.string.StringValue;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

/**
 * Decides what the URL fragment says about the editors on a page.
 *
 * @see SingleDocumentEditorUrlParameterStrategy
 */
public interface EditorUrlParameterStrategy
    extends Serializable
{
    /**
     * @param aStates
     *            the states of the editors the URL should describe, <b>in display order</b>. An
     *            empty list means nothing is open.
     * @return the parameters the URL fragment should carry. Parameters mapped to {@code null} are
     *         removed from the fragment. An empty map leaves the fragment alone.
     */
    Map<String, Object> getUrlFragmentParameters(List<AnnotatorState> aStates);

    /**
     * Convenience for the common single-editor case.
     *
     * @param aState
     *            the state of the editor the URL should describe.
     * @return as {@link #getUrlFragmentParameters(List)}.
     */
    default Map<String, Object> getUrlFragmentParameters(AnnotatorState aState)
    {
        return getUrlFragmentParameters(aState == null ? List.of() : List.of(aState));
    }

    /**
     * The inverse of {@link #getUrlFragmentParameters}: read what the fragment asks for.
     * <p>
     * This only <b>parses</b>. It does not resolve an editor, open a document or check access - the
     * URL names a document, not an editor, so choosing one is the workspace's job and guarding it
     * is the page's. A strategy that omits a parameter when writing ignores it when reading, so
     * that one class states the scheme in both directions.
     *
     * @param aDocument
     *            the raw {@code d} parameter.
     * @param aFocus
     *            the raw {@code f} parameter.
     * @param aDataOwner
     *            the raw {@code u} parameter.
     * @return what was asked for, for the FIRST editor only. Kept for callers that can only act on
     *         one document; prefer {@link #parseUrlFragmentTargets}.
     */
    default UrlFragmentTarget parseUrlFragmentParameters(StringValue aDocument, StringValue aFocus,
            StringValue aDataOwner)
    {
        var targets = parseUrlFragmentTargets(aDocument, aFocus, aDataOwner);
        return targets.isEmpty()
                ? new UrlFragmentTarget(Optional.empty(), Optional.empty(), Optional.empty())
                : targets.get(0);
    }

    /**
     * The inverse of {@link #getUrlFragmentParameters(List)}: read what the fragment asks for, one
     * entry per editor, in display order.
     *
     * @param aDocument
     *            the raw {@code d} parameter.
     * @param aFocus
     *            the raw {@code f} parameter.
     * @param aDataOwner
     *            the raw {@code u} parameter.
     * @return what was asked for. Empty if the fragment names no document at all.
     */
    List<UrlFragmentTarget> parseUrlFragmentTargets(StringValue aDocument, StringValue aFocus,
            StringValue aDataOwner);

    /**
     * Read one editor's focus out of the raw {@code f} parameter, without needing a document.
     *
     * @param aFocus
     *            the raw {@code f} parameter.
     * @param aIndex
     *            which slot, counting from zero.
     * @return the focus asked for, or {@code 0} if the slot is absent or empty.
     */
    default int parseUrlFragmentFocus(StringValue aFocus, int aIndex)
    {
        return parseUrlFragmentFocusIfPresent(aFocus, aIndex).orElse(0);
    }

    /**
     * As {@link #parseUrlFragmentFocus}, but distinguishing "no focus requested" from "focus 0".
     *
     * @return the focus asked for, or empty if the slot is absent or empty.
     */
    Optional<Integer> parseUrlFragmentFocusIfPresent(StringValue aFocus, int aIndex);
}
