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

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.DocumentEditorActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public interface DetailPanelHostingPage
    extends DocumentEditorActionHandler
{
    AnnotationDetailEditorPanel getDetailEditor();

    @Override
    default void actionLoadSelectedAnnotationDetails(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        getDetailEditor().actionLoadSelectionDetails(aTarget);
    }

    @Override
    default void actionDelete(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        getDetailEditor().actionDelete(aTarget);
    }

    @Override
    default void actionReverse(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        getDetailEditor().actionReverse(aTarget);
    }

    @Override
    default void actionFillSlot(AjaxRequestTarget aTarget, int aSlotFillerBegin, int aSlotFillerEnd)
        throws IOException, AnnotationException
    {
        getDetailEditor().actionFillSlot(aTarget, aSlotFillerBegin, aSlotFillerEnd);
    }

    @Override
    default void actionFillSlot(AjaxRequestTarget aTarget, VID aExistingSlotFillerId)
        throws IOException, AnnotationException
    {
        getDetailEditor().actionFillSlot(aTarget, aExistingSlotFillerId);
    }

    @Override
    default void actionClear(AjaxRequestTarget aTarget)
    {
        getDetailEditor().actionClear(aTarget);
    }
}
