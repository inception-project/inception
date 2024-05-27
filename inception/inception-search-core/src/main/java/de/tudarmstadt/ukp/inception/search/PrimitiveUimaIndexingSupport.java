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

import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;
import static org.apache.uima.cas.CAS.TYPE_NAME_FLOAT;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;

import java.util.List;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link SearchServiceAutoConfiguration#primitiveUimaIndexingSupport}.
 * </p>
 */
public class PrimitiveUimaIndexingSupport
    implements FeatureIndexingSupport
{
    private String id;
    private FeatureSupportRegistry featureSupportRegistry;

    public PrimitiveUimaIndexingSupport(@Autowired FeatureSupportRegistry aFeatureSupportRegistry)
    {
        featureSupportRegistry = aFeatureSupportRegistry;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public void setBeanName(String aBeanName)
    {
        id = aBeanName;
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        switch (aFeature.getMultiValueMode()) {
        case NONE:
            switch (aFeature.getType()) {
            case TYPE_NAME_INTEGER: // fall-through
            case TYPE_NAME_FLOAT: // fall-through
            case TYPE_NAME_BOOLEAN: // fall-through
            case TYPE_NAME_STRING:
                return true;
            default:
                return false;
            }
        case ARRAY: // fall-through
            switch (aFeature.getType()) {
            case TYPE_NAME_STRING_ARRAY:
                return true;
            default:
                return false;
            }
        default:
            return false;
        }
    }

    @Override
    public MultiValuedMap<String, String> indexFeatureValue(String aFieldPrefix,
            AnnotationFS aAnnotation, String aFeaturePrefix, AnnotationFeature aFeature)
    {
        var featSup = featureSupportRegistry.findExtension(aFeature).orElseThrow();

        var featureIndexName = featureIndexName(aFieldPrefix, aFeaturePrefix, aFeature);
        if (aFeature.getMultiValueMode() == ARRAY) {
            var valuesMap = new HashSetValuedHashMap<String, String>();
            List<String> values = featSup.getFeatureValue(aFeature, aAnnotation);
            if (values != null) {
                for (var value : values) {
                    var featureValue = featSup.renderFeatureValue(aFeature, value);
                    if (isNotBlank(featureValue)) {
                        valuesMap.put(featureIndexName, featureValue);
                    }
                }
            }
            return valuesMap;
        }

        if (TYPE_NAME_BOOLEAN.equals(aFeature.getType())) {
            var booleanFeature = aAnnotation.getType().getFeatureByBaseName(aFeature.getName());
            boolean value;
            if (booleanFeature == null) {
                value = false;
            }
            else {
                value = aAnnotation.getBooleanValue(booleanFeature);
            }

            var valuesMap = new HashSetValuedHashMap<String, String>();
            valuesMap.put(featureIndexName, value ? "true" : "false");
            return valuesMap;
        }

        var valuesMap = new HashSetValuedHashMap<String, String>();
        var featureValue = featSup.renderFeatureValue(aFeature, aAnnotation);
        if (isNotBlank(featureValue)) {
            valuesMap.put(featureIndexName, featureValue);
        }
        return valuesMap;
    }

    @Override
    public String featureIndexName(String aFieldPrefix, String aFeaturePrefix,
            AnnotationFeature aFeature)
    {
        return aFieldPrefix + aFeaturePrefix + ATTRIBUTE_SEP + aFeature.getUiName();
    }
}
