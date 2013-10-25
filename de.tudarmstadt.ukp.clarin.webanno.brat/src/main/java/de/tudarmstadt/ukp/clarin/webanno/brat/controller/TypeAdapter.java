/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;

public interface TypeAdapter
{
    /**
     * Add annotations from the CAS, which is controlled by the window size, to the brat response
     * {@link GetDocumentResponse}
     * 
     * @param aJcas
     *            The JCAS object containing annotations
     * @param aResponse
     *            A brat response containing annotations in brat protocol
     * @param aBratAnnotatorModel
     *            Data model for brat annotations
     */
    void render(JCas aJcas, GetDocumentResponse aResponse, BratAnnotatorModel aBratAnnotatorModel);

    /**
     * @return The feature of an UIMA annotation containing the label to be displayed in the UI.
     */
    String getLabelFeatureName();

    /**
     * Prefix of the label value for Brat to make sure that different annotation types can use the
     * same label, e.g. a POS tag "N" and a named entity type "N".
     *
     * This is used to differentiate the different types in the brat annotation/visualization. The
     * prefix will not stored in the CAS (striped away at {@link BratAjaxCasController#getType} )
     */
    String getLabelPrefix();
    
//    /**
//     * Update the CAS with new/modification of span annotations from brat
//     * 
//     * @param aLabelValue
//     *            the value of the annotation for the span
//     */
//    void add(String aLabelValue, JCas aJcas, int aAnnotationOffsetStart, int aAnnotationOffsetEnd);

    /**
     * Delete a annotation from CAS.
     *
     * @param aJCas
     *            the CAS object
     * @param aId
     *            the low-level address of the span annotation.
     */
    public void delete(JCas aJCas, int aAddress);
}
