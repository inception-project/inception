/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;

import java.util.Collection;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

/**
 * Type Adapters for span, arc, and chain annotations
 */
public interface TypeAdapter
{
    String FEATURE_SEPARATOR = " | ";

    /**
     * Update this feature with a new value
     *
     * @param aJcas
     *            the JCas.
     * @param aFeature
     *            the feature.
     * @param aAddress
     *            the annotation ID.
     * @param aValue
     *            the value.
     */
    default void updateFeature(JCas aJcas, AnnotationFeature aFeature, int aAddress, Object aValue)
    {
        FeatureStructure fs = selectByAddr(aJcas, FeatureStructure.class, aAddress);
        setFeature(fs, aFeature, aValue);
    }

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
     * @param aVid
     *            the VID of the object to be deleted.
     */
    void delete(JCas aJCas, VID aVid);

    AnnotationLayer getLayer();
    
    Collection<AnnotationFeature> listFeatures();
}
