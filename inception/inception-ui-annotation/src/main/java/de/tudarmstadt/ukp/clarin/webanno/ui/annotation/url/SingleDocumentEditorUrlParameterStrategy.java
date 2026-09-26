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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.PAGE_PARAM_DATA_OWNER;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.PAGE_PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.PAGE_PARAM_FOCUS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Collections.emptyMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.wicket.util.string.StringValue;
import org.danekja.java.util.function.serializable.SerializableSupplier;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

/**
 * The {@code #!d=<doc>&f=<focus>&u=<owner>} scheme, extended to several editors: {@code d},
 * {@code f} and {@code u} are parallel bar-separated lists, one slot per editor in display order.
 * <p>
 * A single editor produces exactly the classic URL. Empty slots are omitted and an all-empty list
 * is dropped, so one document at focus 0 owned by the session owner is still {@code #!d=42} -
 * existing links and bookmarks keep working. That compatibility is the reason for the empty-slot
 * encoding rather than something tidier like repeated keys.
 * <p>
 * Reading and writing are stated in this one class so the two cannot drift: a value the writer
 * omits is a value the reader treats as absent.
 * <p>
 * Examples:
 *
 * <pre>
 * d=42                 one editor, document 42, focus 0, session owner
 * d=42&amp;f=5             one editor at unit 5
 * d=42|7               two editors, documents 42 and 7
 * d=42|7&amp;f=5           unit 5 in the first, unbound in the second
 * d=42|7&amp;f=|5          unbound in the first, unit 5 in the second
 * d=42|7&amp;u=|bob        the second editor shows bob's annotations
 * </pre>
 */
public class SingleDocumentEditorUrlParameterStrategy
    implements EditorUrlParameterStrategy
{
    private static final long serialVersionUID = -8177088139647249429L;

    /**
     * Separates the per-editor slots of {@code d}, {@code f} and {@code u}.
     * <p>
     * A pipe symbol legal and unescaped in a URL fragment, and cannot occur in any slot value, so
     * it needs no escaping here. That is enforced, not assumed: {@code d} and {@code f} are
     * numeric, and a bar is in {@code ValidationUtils#FILESYSTEM_RESERVED_CHARACTERS}, which
     * {@code UserDao#isValidUsername} rejects unconditionally - unlike
     * {@code SecurityProperties#getUsernamePattern}, which an instance may relax to anything.
     */
    private static final String LIST_SEPARATOR = "|";

    /**
     * {@link #LIST_SEPARATOR} as a regex, for {@link String#split}. The bar is a regex alternation
     * operator, so splitting on it raw would split on every character.
     */
    private static final String LIST_SEPARATOR_PATTERN = "\\|";

    private final SerializableSupplier<String> sessionOwnerSupplier;
    private final boolean dataOwnerPinned;

    /**
     * @param aSessionOwnerSupplier
     *            supplies the current session owner's username.
     * @param aDataOwnerPinned
     *            whether the hosting page fixes the data owner rather than taking it from the URL.
     */
    public SingleDocumentEditorUrlParameterStrategy(
            SerializableSupplier<String> aSessionOwnerSupplier, boolean aDataOwnerPinned)
    {
        sessionOwnerSupplier = aSessionOwnerSupplier;
        dataOwnerPinned = aDataOwnerPinned;

        if (!aDataOwnerPinned && aSessionOwnerSupplier == null) {
            throw new IllegalArgumentException("A scheme that names the data owner needs to know "
                    + "the session owner, so that a plain link stays plain");
        }
    }

    private boolean isDataOwnerPinned()
    {
        return dataOwnerPinned;
    }

    @Override
    public Map<String, Object> getUrlFragmentParameters(List<AnnotatorState> aStates)
    {
        var states = aStates == null ? List.<AnnotatorState> of()
                : aStates.stream().filter(s -> s != null && s.getDocument() != null).toList();

        if (states.isEmpty()) {
            return emptyMap();
        }

        var parameters = new LinkedHashMap<String, Object>();

        parameters.put(PAGE_PARAM_DOCUMENT,
                join(states, state -> String.valueOf(state.getDocument().getId())));

        // A focus of 0 is the default, so it is written as an EMPTY SLOT rather than a zero - that
        // keeps a single-editor URL free of `f` entirely, exactly as before. The slot still has to
        // be there when a later editor does have a focus, or the lists would not line up.
        parameters.put(PAGE_PARAM_FOCUS,
                join(states,
                        state -> state.getFocusUnitIndex() > 0
                                ? String.valueOf(state.getFocusUnitIndex())
                                : ""));

        if (isDataOwnerPinned()) {
            parameters.put(PAGE_PARAM_DATA_OWNER, null);
        }
        else {
            // REC: We currently do not want that one can switch to the CURATION_USER directly via
            // the URL without having to activate sidebar curation mode as well, so we do not handle
            // the CURATION_USER here.
            parameters.put(PAGE_PARAM_DATA_OWNER, join(states, state -> {
                var dataOwner = state.getUser().getUsername();
                return Set.of(sessionOwnerSupplier.get(), CURATION_USER).contains(dataOwner) ? ""
                        : dataOwner;
            }));
        }

        return parameters;
    }

    /**
     * Render one slot per editor, bar-separated - but collapse to {@code null} when every slot is
     * empty, so the parameter is dropped from the fragment altogether.
     */
    private static String join(List<AnnotatorState> aStates,
            Function<AnnotatorState, String> aRenderer)
    {
        var slots = aStates.stream().map(aRenderer).toList();

        if (slots.stream().allMatch(String::isEmpty)) {
            return null;
        }

        // Trailing empties carry no information - "5|" and "5" mean the same thing - so drop them
        // and keep the common cases short.
        var end = slots.size();
        while (end > 0 && slots.get(end - 1).isEmpty()) {
            end--;
        }

        return String.join(LIST_SEPARATOR, slots.subList(0, end));
    }

    /**
     * Where the data owner is pinned, {@code u} is ignored on the way in just as it is omitted on
     * the way out, so that one class states the scheme in both directions.
     */
    @Override
    public List<UrlFragmentTarget> parseUrlFragmentTargets(StringValue aDocument,
            StringValue aFocus, StringValue aDataOwner)
    {
        // The DOCUMENT list decides how many editors the fragment describes. A focus or an owner
        // without a document names nothing, so a longer f/u list is surplus and ignored rather
        // than conjuring editors out of nothing.
        var documents = split(aDocument);
        if (documents.isEmpty()) {
            return List.of();
        }

        var foci = split(aFocus);
        var dataOwners = isDataOwnerPinned() ? List.<String> of() : split(aDataOwner);

        var targets = new ArrayList<UrlFragmentTarget>();
        for (var i = 0; i < documents.size(); i++) {
            targets.add(new UrlFragmentTarget( //
                    optional(documents.get(i)), //
                    optional(slot(dataOwners, i)), //
                    optionalInt(slot(foci, i))));
        }

        return targets;
    }

    @Override
    public Optional<Integer> parseUrlFragmentFocusIfPresent(StringValue aFocus, int aIndex)
    {
        return optionalInt(slot(split(aFocus), aIndex));
    }

    private static String slot(List<String> aSlots, int aIndex)
    {
        return aIndex < aSlots.size() ? aSlots.get(aIndex) : null;
    }

    private static List<String> split(StringValue aValue)
    {
        if (aValue == null || aValue.isEmpty()) {
            return List.of();
        }

        var value = aValue.toString();
        if (value.isBlank()) {
            return List.of();
        }

        // -1 keeps trailing empty slots, which matter: "1|" is two editors, the second unnamed.
        return List.of(value.split(LIST_SEPARATOR_PATTERN, -1));
    }

    private static Optional<String> optional(String aValue)
    {
        return aValue == null || aValue.isBlank() ? Optional.empty() : Optional.of(aValue);
    }

    private static Optional<Integer> optionalInt(String aValue)
    {
        if (aValue == null || aValue.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.valueOf(aValue.trim()));
        }
        catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
