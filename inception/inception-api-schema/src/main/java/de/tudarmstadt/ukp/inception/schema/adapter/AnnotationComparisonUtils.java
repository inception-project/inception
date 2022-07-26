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
package de.tudarmstadt.ukp.inception.schema.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil.getDefaultFeatureValue;
import static de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil.getFeatureValue;

import java.util.Objects;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.text.AnnotationFS;

public class AnnotationComparisonUtils
{
    /**
     * Return true if these two annotations agree on every non slot features
     */
    public static boolean isEquivalentSpanAnnotation(AnnotationFS aFs1, AnnotationFS aFs2,
            FeatureFilter aFilter)
    {
        // Check offsets (because they are excluded by shouldIgnoreFeatureOnMerge())
        if (aFs1.getBegin() != aFs2.getBegin() || aFs1.getEnd() != aFs2.getEnd()) {
            return false;
        }

        // Check the features (basically limiting to the primitive features)
        for (Feature f1 : aFs1.getType().getFeatures()) {
            if (aFilter != null && !aFilter.isAllowed(aFs1, f1)) {
                continue;
            }

            Object value1 = getFeatureValue(aFs1, f1);

            Feature f2 = aFs2.getType().getFeatureByBaseName(f1.getShortName());
            Object value2 = f2 != null ? getFeatureValue(aFs2, f2) : getDefaultFeatureValue(f1);

            if (!Objects.equals(value1, value2)) {
                return false;
            }
        }
        return true;
    }
}
