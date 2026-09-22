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
import java.util.Optional;

/**
 * Represents the URL names a document, optionally a data owner and a focus.
 * <p>
 * Output from {@link EditorUrlParameterStrategy}.
 *
 * @param document
 *            the requested document <b>as written in the URL</b>, or empty if the fragment does not
 *            name one. {@code d} may be a document id <i>or</i> a document name, and which it is
 *            can only be decided against a project.
 * @param dataOwner
 *            the requested data owner, or empty if the fragment does not name one or the strategy
 *            does not accept one. Empty means "the session owner" to the caller - a strategy that
 *            pins the data owner reports empty here and lets the page apply its own pin.
 * @param focus
 *            the requested focus unit index, or empty if the fragment does not name one.
 */
public record UrlFragmentTarget(Optional<String> document, Optional<String> dataOwner,
        Optional<Integer> focus)
    implements Serializable
{
    private static final long serialVersionUID = 4085148982817822265L;

    /**
     * @return whether this fragment asks for anything at all. An empty target is what a fragment
     *         that names neither a document nor a focus parses to, and is the signal to leave the
     *         page as it is.
     */
    public boolean isEmpty()
    {
        return document.isEmpty() && focus.isEmpty();
    }
}
