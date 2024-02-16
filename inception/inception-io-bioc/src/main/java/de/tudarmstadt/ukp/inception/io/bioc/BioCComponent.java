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
package de.tudarmstadt.ukp.inception.io.bioc;

import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XCasSchemaAnalyzer.isRelationLayer;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XCasSchemaAnalyzer.isSpanLayer;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LAYER_NAME;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.uima.cas.CAS.FEATURE_FULL_NAME_BEGIN;
import static org.apache.uima.cas.CAS.FEATURE_FULL_NAME_END;
import static org.apache.uima.cas.CAS.FEATURE_FULL_NAME_SOFA;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.MetaDataStringField;

public interface BioCComponent
{
    Set<String> EXCLUDED_FEATURES = Set.of(FEATURE_FULL_NAME_BEGIN, FEATURE_FULL_NAME_END,
            FEATURE_FULL_NAME_SOFA);

    String E_DOCUMENT = "document";
    String E_COLLECTION = "collection";
    String E_PASSAGE = "passage";
    String E_SENTENCE = "sentence";
    String E_ANNOTATION = "annotation";
    String E_NODE = "node";
    String E_RELATION = "relation";
    String E_INFON = "infon";
    String E_OFFSET = "offset";
    String E_LOCATION = "location";
    String E_KEY = "key";
    String E_DATE = "date";
    String E_ID = "id";
    String E_SOURCE = "source";
    String E_TEXT = "text";

    String A_ID = "id";
    String A_KEY = "key";
    String A_OFFSET = "offset";
    String A_LENGTH = "length";
    String A_REFID = "refid";
    String A_ROLE = "role";

    // Well-known infon keys
    String I_TYPE = "type";

    // Well-known relation roles
    String R_SOURCE = "source";
    String R_TARGET = "target";

    static void addCollectionMetadataField(JCas aJCas, String aKey, String aValue)
    {
        if (aValue == null) {
            return;
        }

        var keyField = new MetaDataStringField(aJCas);
        keyField.setKey(aKey);
        keyField.setValue(aValue);
        keyField.addToIndexes();
    }

    static Optional<MetaDataStringField> getCollectionMetadataField(CAS aCas, String aKey)
    {
        return aCas.select(MetaDataStringField.class) //
                .filter(f -> aKey.equals(f.getKey())) //
                .findFirst();
    }

    static Type guessBestRelationType(TypeSystem aTypeSystem, Map<String, List<String>> aInfons)
    {
        var type = guessBestType(aTypeSystem, aInfons);
        if (type != null && isRelationLayer(type)) {
            return type;
        }

        type = aTypeSystem.getType(BASIC_RELATION_LAYER_NAME);
        if (type != null) {
            return type;
        }

        return null;
    }

    static Type guessBestSpanType(TypeSystem aTypeSystem, Map<String, List<String>> aInfons)
    {
        var type = guessBestType(aTypeSystem, aInfons);
        if (type != null && isSpanLayer(type)) {
            return type;
        }

        type = aTypeSystem.getType(BASIC_SPAN_LAYER_NAME);
        if (type != null) {
            return type;
        }

        return null;
    }

    private static Type guessBestType(TypeSystem aTypeSystem, Map<String, List<String>> aInfons)
    {
        var typeInfon = aInfons.get(I_TYPE);
        if (isNotEmpty(typeInfon)) {
            var type = aTypeSystem.getType(typeInfon.get(0));
            if (type != null) {
                return type;
            }

            type = findTypeByShortName(aTypeSystem, typeInfon.get(0));
            if (type != null) {
                return type;
            }
        }

        return null;
    }

    private static Type findTypeByShortName(TypeSystem aTypeSystem, String aBaseName)
    {
        for (var t : aTypeSystem) {
            if (aBaseName.equals(t.getShortName())) {
                return t;
            }
        }

        return null;
    }

    public static void transferFeatures(AnnotationFS aAnnotation, Map<String, List<String>> aInfons)
    {
        var anyFeatureSet = false;
        for (var infon : aInfons.entrySet()) {
            if (EXCLUDED_FEATURES.contains(infon.getKey())) {
                continue;
            }

            var feature = aAnnotation.getType().getFeatureByBaseName(infon.getKey());
            if (feature == null) {
                continue;
            }

            if (feature.getRange().isPrimitive() && isNotEmpty(infon.getValue())) {
                aAnnotation.setFeatureValueFromString(feature, infon.getValue().get(0));
                anyFeatureSet = true;
            }

            if (CAS.TYPE_NAME_STRING_ARRAY.equals(feature.getRange().getName())
                    && isNotEmpty(infon.getValue())) {
                FSUtil.setFeature(aAnnotation, feature, infon.getValue());
                anyFeatureSet = true;
            }
        }

        // If there is only one infon, then we assume that it is the value
        var valueFeature = aAnnotation.getType()
                .getFeatureByBaseName(BASIC_SPAN_LABEL_FEATURE_NAME);
        if (!anyFeatureSet && aInfons.size() == 1 && valueFeature != null) {
            var values = aInfons.values().iterator().next();
            if (isNotEmpty(values)) {
                aAnnotation.setFeatureValueFromString(valueFeature, values.get(0));
            }
        }
    }
}
