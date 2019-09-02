/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.action;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public interface AnnotationActionHandler
{
    void actionCreateOrUpdate(AjaxRequestTarget aTarget, CAS aCas)
        throws IOException, AnnotationException;

    /**
     * Create annotation on the next token for forward annotation
     */
    void actionCreateForward(AjaxRequestTarget aTarget, CAS aCas)
        throws IOException, AnnotationException;

    /**
     * Load the annotation pointed to in {@link AnnotatorState#getSelection()} in the detail panel.
     */
    void actionSelect(AjaxRequestTarget aTarget, CAS aCas)
        throws AnnotationException;

    /**
     * Delete currently selected annotation.
     */
    void actionDelete(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException;

    /**
     * Clear the currently selected annotation from the editor panel.
     */
    void actionClear(AjaxRequestTarget aTarget)
        throws AnnotationException;

    /**
     * Reverse the currently selected relation.
     */
    void actionReverse(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException;
    
    /**
     * Fill the currently armed slot with the given annotation.
     * 
     * @param aTarget
     *            the AJAX request target.
     * @param aCas
     *            the CAS in which the slot is going to be filled.
     * @param aSlotFillerBegin
     *            the begin of the span selected by the user to create a new annotation or the begin
     *            of the span of the selected existing annotation.
     * @param aSlotFillerEnd
     *            the corresponding end.
     * @param aExistingSlotFillerId
     *            the {@link VID} of a selected existing annotation. If no annotation has been
     *            selected, this is {@link VID#NONE_ID} and {@link VID#isSet()} returns
     *            {@code false}.
     */
    void actionFillSlot(AjaxRequestTarget aTarget, CAS aCas, int aSlotFillerBegin,
            int aSlotFillerEnd, VID aExistingSlotFillerId)
        throws IOException, AnnotationException;
    
    CAS getEditorCas()
            throws IOException;
}
