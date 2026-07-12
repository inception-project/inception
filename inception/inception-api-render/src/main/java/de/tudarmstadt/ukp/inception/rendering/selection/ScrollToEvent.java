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

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorViewState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.EditorBoundEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;

public class ScrollToEvent
    implements EditorBoundEvent
{
    private final AnnotatorViewState source;
    private final AjaxRequestTarget requestHandler;
    private final int offset;
    private final List<VRange> pingRanges;
    private final FocusPosition position;

    public ScrollToEvent(AnnotatorViewState aSource, AjaxRequestTarget aRequestHandler, int aOffset,
            VRange aPingRange, FocusPosition aPos)
    {
        this(aSource, aRequestHandler, aOffset, aPingRange != null ? asList(aPingRange) : null,
                aPos);
    }

    public ScrollToEvent(AnnotatorViewState aSource, AjaxRequestTarget aRequestHandler, int aOffset,
            List<VRange> aPingRanges, FocusPosition aPos)
    {
        source = aSource;
        requestHandler = aRequestHandler;
        offset = aOffset;
        position = aPos;
        pingRanges = aPingRanges;
    }

    /**
     * @return the annotator state that originated this scroll request. An editor should only react
     *         to events originating from its own state so that paging one editor does not scroll
     *         other editors sharing the same page (e.g. a reference-document sidebar viewer).
     */
    public AnnotatorViewState getSource()
    {
        return source;
    }

    public AjaxRequestTarget getRequestHandler()
    {
        return requestHandler;
    }

    public int getOffset()
    {
        return offset;
    }

    /**
     * @deprecated Use {@link #getPingRanges()} instead.
     */
    @Deprecated
    public VRange getPingRange()
    {
        return pingRanges != null && !pingRanges.isEmpty() ? pingRanges.get(0) : null;
    }

    public List<VRange> getPingRanges()
    {
        return pingRanges;
    }

    public FocusPosition getPosition()
    {
        return position;
    }
}
