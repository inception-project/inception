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

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

/**
 * Type Adapters for span, arc, and chain annotations
 *
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 */
public interface TypeAdapter
{
    static final String FEATURE_SEPARATOR = " | ";

    /**
     * Add new annotation to the CAS using the MIRA prediction. This is different from the add
     * methods in the {@link TypeAdapter}s in such a way that the begin and end offsets are always
     * exact so that no need to re-compute
     * 
     * @param aJcas the JCas.
     * @param feature the feature.
     * @param labelValues the values.
     * @throws BratAnnotationException
     *             if the annotations could not be created/updated.
     * @throws IOException if an I/O error occurs.
     */
    void automate(JCas aJcas, AnnotationFeature feature, List<String> labelValues)
        throws BratAnnotationException, IOException;

    /**
     * Update this feature with a new value
     * 
     * @param aJcas the JCas.
     * @param feature the feature.
     * @param address the annotation ID.
     * @param value the value.
     */
    void updateFeature(JCas aJcas, AnnotationFeature feature, int address, String value);

    /**
     * Add annotations from the CAS, which is controlled by the window size, to the brat response
     * {@link GetDocumentResponse}
     *
     * @param aJcas
     *            The JCAS object containing annotations
     * @param features the features.
     * @param aResponse
     *            A brat response containing annotations in brat protocol
     * @param aBratAnnotatorModel
     *            Data model for brat annotations
     * @param aColoringStrategy
     *            the  coloring strategy to render this layer
     */
    void render(JCas aJcas, List<AnnotationFeature> features, GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel, ColoringStrategy aColoringStrategy);
    
    /**
     * The ID of the type.
     * 
     * @return the ID.
     */
    long getTypeId();

    /**
     * Get the CAS type of the this {@link TypeAdapter}
     * 
     * @param cas the CAS.
     * @return the type.
     */
    Type getAnnotationType(CAS cas);

    /**
     * Get the CAS type of the this {@link TypeAdapter}
     * 
     * @return the type.
     */
    String getAnnotationTypeName();

    /**
     * determine the type of Span annotation to be used to have arc annotations (as Origin and
     * target)
     * 
     * @return the attach feature name.
     */
    String getAttachFeatureName();

    /**
     * determine the type of Span annotation to be used to have arc annotations (as Origin and
     * target)
     * 
     * @return the attach type name.
     */
    String getAttachTypeName();

    /**
     * check if the annotation type is deletable
     * 
     * @return if the layer is deletable.
     */
    boolean isDeletable();

    /**
     * Delete a annotation from CAS.
     *
     * @param aJCas
     *            the CAS object
     * @param aAddress
     *            the low-level address of the span annotation.
     */
    public void delete(JCas aJCas, int aAddress);

    // delete based on the begin,end, and type of annotation
    void delete(JCas aJCas, AnnotationFeature feature, int aBegin, int aEnd, String aValue);

    void deleteBySpan(JCas aJCas, AnnotationFS fs, int aBegin, int aEnd);

    List<String> getAnnotation(JCas aJcas, AnnotationFeature feature, int begin, int end);
    
    AnnotationLayer getLayer();
}
