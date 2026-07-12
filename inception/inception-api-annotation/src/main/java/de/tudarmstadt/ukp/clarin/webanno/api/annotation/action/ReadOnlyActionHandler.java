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

import java.io.IOException;
import java.io.Serializable;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

/**
 * A read-only {@link AnnotationActionHandler} for editors that only display a document (e.g. a
 * curation view or a reference-document sidebar).
 * <p>
 * Provides the CAS to the editor via the given {@link CasProvider}, ignores all
 * selection/navigation callbacks (the viewer is fully passive) and rejects any mutating action.
 */
public class ReadOnlyActionHandler
    implements AnnotationActionHandler, Serializable
{
    private static final long serialVersionUID = 1L;

    private final CasProvider casProvider;

    public ReadOnlyActionHandler(CasProvider aCasProvider)
    {
        casProvider = aCasProvider;
    }

    @Override
    public CAS getEditorCas() throws IOException
    {
        return casProvider.get();
    }

    @Override
    public void ensureIsEditable() throws AnnotationException
    {
        throw new NotEditableException("This editor is read-only.");
    }

    // --- Passive: clicks in the read-only editor do nothing -----------------------------------

    @Override
    public void actionSelect(AjaxRequestTarget aTarget)
    {
        // Passive viewer - ignore
    }

    @Override
    public void actionSelect(AjaxRequestTarget aTarget, AnnotationFS aAnnoFs)
    {
        // Passive viewer - ignore
    }

    @Override
    public void actionSelect(AjaxRequestTarget aTarget, VID aVid)
    {
        // Passive viewer - ignore
    }

    @Override
    public void actionSelectAndJump(AjaxRequestTarget aTarget, VID aVid)
    {
        // Passive viewer - ignore
    }

    @Override
    public void actionSelectAndJump(AjaxRequestTarget aTarget, AnnotationFS aFS)
    {
        // Passive viewer - ignore
    }

    @Override
    public void actionJump(AjaxRequestTarget aTarget, VID aVid)
    {
        // Passive viewer - ignore
    }

    @Override
    public void actionJump(AjaxRequestTarget aTarget, AnnotationFS aFS)
    {
        // Passive viewer - ignore
    }

    @Override
    public void actionJump(AjaxRequestTarget aTarget, int aBegin, int aEnd)
    {
        // Passive viewer - ignore
    }

    @Override
    public void actionClear(AjaxRequestTarget aTarget)
    {
        // Passive viewer - ignore
    }

    // --- Mutating actions are not permitted ---------------------------------------------------

    @Override
    public void actionCreateOrUpdate(AjaxRequestTarget aTarget, CAS aCas) throws AnnotationException
    {
        throw new NotEditableException("This editor is read-only.");
    }

    @Override
    public void actionDelete(AjaxRequestTarget aTarget) throws AnnotationException
    {
        throw new NotEditableException("This editor is read-only.");
    }

    @Override
    public void actionReverse(AjaxRequestTarget aTarget) throws AnnotationException
    {
        throw new NotEditableException("This editor is read-only.");
    }

    @Override
    public void actionFillSlot(AjaxRequestTarget aTarget, CAS aCas, int aSlotFillerBegin,
            int aSlotFillerEnd)
        throws AnnotationException
    {
        throw new NotEditableException("This editor is read-only.");
    }

    @Override
    public void actionFillSlot(AjaxRequestTarget aTarget, CAS aCas, VID aExistingSlotFillerId)
        throws AnnotationException
    {
        throw new NotEditableException("This editor is read-only.");
    }

    @Override
    public void writeEditorCas() throws AnnotationException
    {
        throw new NotEditableException("This editor is read-only.");
    }
}
