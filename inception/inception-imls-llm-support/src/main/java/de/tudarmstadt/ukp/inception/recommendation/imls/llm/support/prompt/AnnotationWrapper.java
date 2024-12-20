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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt;

import java.util.AbstractMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation;

public class AnnotationWrapper
    extends AbstractMap<String, Object>
{
    private static final String COVERED_TEXT = "$coveredText";

    private static final Set<String> FEATURE_BLACKLIST = Set.of(CAS.FEATURE_BASE_NAME_SOFA);

    private final Annotation annotation;

    public AnnotationWrapper(Annotation aAnnotation)
    {
        annotation = aAnnotation;
    }

    @Override
    public Object get(Object aKey)
    {
        if (aKey instanceof String key) {
            if (COVERED_TEXT.equals(key)) {
                return annotation.getCoveredText();
            }

            var feature = annotation.getType().getFeatureByBaseName(key);
            if (feature != null) {
                return annotation.getFeatureValueAsString(feature);
            }
        }

        return null;
    }

    @Override
    public Set<String> keySet()
    {
        var features = annotation.getType().getFeatures().stream() //
                .map(Feature::getShortName) //
                .filter(name -> !FEATURE_BLACKLIST.contains(name));

        var specials = Stream.of(COVERED_TEXT);

        return Stream.concat(features, specials).collect(Collectors.toSet());
    }

    @Override
    public Set<Entry<String, Object>> entrySet()
    {
        return keySet().stream().map(k -> new SimpleEntry<>(k, get(k))).collect(Collectors.toSet());
    }

    @Override
    public String toString()
    {
        return annotation.getCoveredText();
    }
}
