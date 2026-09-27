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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.support.uima.Range;

/**
 * Action handler for an annotation shown in the {@link AnnotationDetailEditorPanel} without being
 * part of an editor. It only provides the CAS, so that feature editors resolve addresses against
 * the CAS the annotation comes from. Editing actions fail and navigation actions do nothing - there
 * is no editor to navigate in.
 */
class ReadOnlyAnnotationActionHandler
    implements AnnotationActionHandler, Serializable
{
    private static final long serialVersionUID = -2410386453402513095L;

    private final CasProvider casProvider;

    ReadOnlyAnnotationActionHandler(CasProvider aCasProvider)
    {
        casProvider = aCasProvider;
    }

    @Override
    public CAS getEditorCas() throws IOException
    {
        return casProvider.get();
    }

    @Override
    public boolean isEditable()
    {
        return false;
    }

    @Override
    public void ensureIsEditable() throws NotEditableException
    {
        throw new NotEditableException("This annotation is shown read-only");
    }

    @Override
    public void actionLoadSelectedAnnotationDetails(AjaxRequestTarget aTarget)
    {
        // Nothing to do - there is no editor to load the details from
    }

    @Override
    public void actionSelect(AjaxRequestTarget aTarget, VID aVid)
    {
        // Nothing to do - there is no editor to select in
    }

    @Override
    public void actionSelectAndJump(AjaxRequestTarget aTarget, VID aVid)
    {
        // Nothing to do - there is no editor to select in
    }

    @Override
    public void actionJump(AjaxRequestTarget aTarget, int aBegin, int aEnd)
    {
        // Nothing to do - there is no editor to jump in
    }

    @Override
    public void actionJumpTo(AjaxRequestTarget aTarget, int aBegin, int aEnd,
            List<VRange> aAdditionalPingRanges)
    {
        // Nothing to do - there is no editor to jump in
    }

    @Override
    public void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner, Range aRange, List<VRange> aAdditionalPingRanges)
    {
        // Nothing to do - there is no editor to show the document in
    }

    @Override
    public void actionDelete(AjaxRequestTarget aTarget) throws NotEditableException
    {
        ensureIsEditable();
    }

    @Override
    public void actionClear(AjaxRequestTarget aTarget) throws NotEditableException
    {
        ensureIsEditable();
    }

    @Override
    public void actionReverse(AjaxRequestTarget aTarget) throws NotEditableException
    {
        ensureIsEditable();
    }

    @Override
    public void actionFillSlot(AjaxRequestTarget aTarget, int aSlotFillerBegin, int aSlotFillerEnd)
        throws NotEditableException
    {
        ensureIsEditable();
    }

    @Override
    public void actionFillSlot(AjaxRequestTarget aTarget, VID aExistingSlotFillerId)
        throws NotEditableException
    {
        ensureIsEditable();
    }

    @Override
    public void writeEditorCas() throws NotEditableException
    {
        ensureIsEditable();
    }

    @Override
    public void writeEditorCas(CAS aCas) throws NotEditableException
    {
        ensureIsEditable();
    }
}
