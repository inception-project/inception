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
package de.tudarmstadt.ukp.inception.search;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.beans.factory.BeanNameAware;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public interface FeatureIndexingSupport
    extends BeanNameAware
{
    public static final String ATTRIBUTE_SEP = ".";
    public static final String SPECIAL_SEP = "-";

    String getId();

    boolean accepts(AnnotationFeature aFeature);

    /**
     * Extracts key/value pairs from the given annotation/feature to be added to a search index.
     * 
     * @param aFieldPrefix
     *            the prefix of the field into which the values are to be indexed. All keys in the
     *            produced map will start with this value. Usually, we use the UI name of the layer
     *            to which the annotation belongs.
     * @param aAnnotation
     *            the annotation from which to extract the values.
     * @param aFeaturePrefix
     *            a prefix to be added before the feature name. This could be used to index nested
     *            features.
     * @param aFeature
     *            the feature from which to extract the values.
     * @return key/value pairs as a map
     */
    MultiValuedMap<String, String> indexFeatureValue(String aFieldPrefix, AnnotationFS aAnnotation,
            String aFeaturePrefix, AnnotationFeature aFeature);

    /**
     * Get the name of the feature how it is used in the index
     * 
     * @param aFieldPrefix
     *            the layer name
     * @param aFeaturePrefix
     *            possible prefix to the feature name, currently for source and target of relation
     *            annotations
     * @param aFeature
     *            the feature
     * @return index feature name
     */
    String featureIndexName(String aFieldPrefix, String aFeaturePrefix, AnnotationFeature aFeature);
}
