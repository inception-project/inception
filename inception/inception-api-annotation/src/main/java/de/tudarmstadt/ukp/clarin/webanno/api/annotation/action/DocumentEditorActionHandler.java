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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.action;

import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.Range.rangeClippedToDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;

public interface DocumentEditorActionHandler
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

    /**
     * Open the given document in the editor.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document to open
     * @throws AnnotationException
     *             if the document cannot be opened in this editor
     */
    void actionOpenDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
        throws AnnotationException;

    @Override
    default void actionSelect(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        var annoFs = selectAnnotationByAddr(getEditorCas(), aVid.getId());
        getAnnotatorState().setSelection(selectionFor(aVid, annoFs));
        actionLoadSelectedAnnotationDetails(aTarget);
    }

    @Override
    default void actionSelectAndJump(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        actionSelect(aTarget, aVid);

        var cas = getEditorCas();
        var annoFs = selectAnnotationByAddr(cas, aVid.getId());

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
     * @param aBegin
     *            the offset to scroll to
     * @param aEnd
     *            the corresponding end offset
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
    default void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            int aBegin, int aEnd)
        throws IOException, AnnotationException
    {
        actionShowSelectedDocument(aTarget, aDocument, aBegin, aEnd, null);
    }

    /**
     * Open the given document in the editor, scroll to the given location. Optionally highlight
     * additional ranges during the scroll.
     */
    @Override
    default void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            int aBegin, int aEnd, List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException
    {
        // A null document means "wherever we are" - callers default to the context's own document,
        // which is itself null until the first one has been loaded.
        var switched = aDocument != null && !aDocument.equals(getAnnotatorState().getDocument());
        if (switched) {
            actionOpenDocument(aTarget, aDocument);
        }

        actionJump(aTarget, aBegin, aEnd, aAdditionalPingRanges, switched);
    }

    @Override
    default void actionClear(AjaxRequestTarget aTarget)
    {
        getAnnotatorState().setSelection(Selection.unselected());
        actionRefreshDocument(aTarget);
    }
}
