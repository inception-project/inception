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

import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.Range.rangeClippedToDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.inception.support.uima.Range;
import de.tudarmstadt.ukp.inception.rendering.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;

public interface DocumentEditor
    extends AnnotationActionHandler, DiamContext
{
    /**
     * @param aVid
     *            the VID of the annotation to select
     * @param aAnnotation
     *            the annotation to select
     * @return the selection for the given annotation.
     */
    Selection selectionFor(VID aVid, AnnotationFS aAnnotation);

    @Override
    default void actionSelect(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        if (!(selectFsByAddr(getEditorCas(), aVid.getId()) instanceof AnnotationFS annoFs)) {
            return;
        }

        getAnnotatorState().setSelection(selectionFor(aVid, annoFs));
        actionLoadSelectedAnnotationDetails(aTarget);
    }

    @Override
    default void actionSelectAndJump(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        actionSelect(aTarget, aVid);

        var cas = getEditorCas();

        if (!(selectFsByAddr(cas, aVid.getId()) instanceof AnnotationFS annoFs)) {
            return;
        }

        actionJump(aTarget, annoFs.getBegin(), annoFs.getEnd(),
                getAnnotatorState().getSelection().pingRanges(cas));
    }

    @Override
    default void actionJump(AjaxRequestTarget aTarget, int aBegin, int aEnd) throws IOException
    {
        actionJump(aTarget, aBegin, aEnd, null);
    }

    /**
     * Scroll to the given location. Optionally highlight additional ranges during the scroll.
     *
     * @param aTarget
     *            the AJAX target
     * @param aRange
     *            where to scroll to, or {@link Range#UNDEFINED} to leave the editor wherever
     *            opening the document placed it.
     * @param aAdditionalPingRanges
     *            additional ranges that should ideally be visible. May be {@code null} or empty.
     * @throws IOException
     *             if there was an I/O-level problem
     */
    @Override
    default void actionJump(AjaxRequestTarget aTarget, int aBegin, int aEnd,
            List<VRange> aAdditionalPingRanges)
        throws IOException
    {
        actionJump(aTarget, aBegin, aEnd, aAdditionalPingRanges, false);
    }

    /**
     * Scroll to the given location, mirroring
     * {@code AnnotationPageBase#actionShowSelectedDocument}.
     *
     * @param aForceRefresh
     *            re-render even in a non-paged editor. Set when the document was just switched: the
     *            editor then shows different content, so the client cannot simply scroll.
     */
    private void actionJump(AjaxRequestTarget aTarget, int aBegin, int aEnd,
            List<VRange> aAdditionalPingRanges, boolean aForceRefresh)
        throws IOException
    {
        var state = getAnnotatorState();
        var cas = getEditorCas();
        var range = rangeClippedToDocument(cas, aBegin, aEnd);

        var pingRanges = new ArrayList<VRange>();
        pingRanges.add(new VRange(range.getBegin(), range.getEnd()));

        if (aAdditionalPingRanges != null) {
            for (var pingRange : aAdditionalPingRanges) {
                var clipped = rangeClippedToDocument(cas, pingRange.getBegin(), pingRange.getEnd());
                pingRanges.add(new VRange(clipped.getBegin(), clipped.getEnd()));
            }
        }

        state.getPagingStrategy().moveToOffset(state, cas, aBegin, pingRanges, CENTERED);

        if (!aForceRefresh && state.getPagingStrategy() instanceof NoPagingStrategy) {
            return;
        }

        actionRefreshDocument(aTarget);
    }

    @Override
    default void actionJumpTo(AjaxRequestTarget aTarget, int aBegin, int aEnd,
            List<VRange> aAdditionalPingRanges)
        throws IOException
    {
        // No document switch, so no forced refresh: a non-paged editor can scroll client-side.
        actionJump(aTarget, aBegin, aEnd, aAdditionalPingRanges, false);
    }

    @Override
    default void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner, Range aRange)
        throws IOException, AnnotationException
    {
        actionShowSelectedDocument(aTarget, aDocument, aDataOwner, aRange, null);
    }

    /**
     * Open the given document in the editor, scroll to the given location. Optionally highlight
     * additional ranges during the scroll.
     */
    @Override
    void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner, Range aRange, List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException;

    @Override
    default void actionClear(AjaxRequestTarget aTarget)
    {
        getAnnotatorState().setSelection(Selection.unselected());
        actionRefreshDocument(aTarget);
    }

    /**
     * Repaint everything in <b>this</b> editor that depends on the state of the document it shows -
     * in particular on whether the document may still be edited.
     * <p>
     * Call this after a document state transition instead of repainting the page. A page repaint
     * also works, but it takes every other editor with it, remounting panes whose document did not
     * change.
     *
     * @param aTarget
     *            the AJAX target. May be {@code null} outside a partial page update.
     */
    default void refreshAfterDocumentStateChange(AjaxRequestTarget aTarget)
    {
        // Nothing to do for an editor that renders nothing state-dependent.
    }

    /**
     * Unload the document from the editor.
     *
     * @param aTarget
     *            the AJAX target. May be {@code null} outside a partial page update.
     */
    void actionUnloadDocument(AjaxRequestTarget aTarget);
}
