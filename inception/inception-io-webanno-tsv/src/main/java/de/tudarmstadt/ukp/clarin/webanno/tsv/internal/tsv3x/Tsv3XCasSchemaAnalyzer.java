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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x;

import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.CHAIN_ELEMENT_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.CHAIN_LINK_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.RELATION_REF;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.CHAIN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.RELATION;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.SPAN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.CHAIN_FIRST_FEAT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.CHAIN_NEXT_FEAT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.COREFERENCE_TYPE_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_SLOT_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_SLOT_TARGET;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvColumn;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class Tsv3XCasSchemaAnalyzer
{
    private static final Logger LOG = LoggerFactory.getLogger(Tsv3XCasSchemaAnalyzer.class);

    public static TsvSchema analyze(TypeSystem aTypeSystem)
    {
        TsvSchema schema = new TsvSchema();

        Set<Type> incompatibleTypes = new HashSet<>();

        Set<Type> chainLinkTypes = new HashSet<>();

        // Consider only direct subtypes of the UIMA Annotation type. Currently, WebAnno only
        // supports such layers.
        Type annotationType = aTypeSystem.getType(CAS.TYPE_NAME_ANNOTATION);
        Type documentAnnotationType = aTypeSystem.getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);
        for (Type type : aTypeSystem.getDirectSubtypes(annotationType)) {
            if (aTypeSystem.subsumes(documentAnnotationType, type)) {
                continue;
            }

            if (type.getName().equals(Token.class.getName())
                    || type.getName().equals(Sentence.class.getName())) {
                continue;
            }

            switch (schema.getLayerType(type)) {
            case RELATION:
                schema.addColumn(new TsvColumn(type, RELATION,
                        type.getFeatureByBaseName(FEAT_REL_SOURCE), RELATION_REF));
                generateColumns(aTypeSystem, schema, RELATION, type);
                break;
            case CHAIN:
                schema.addColumn(new TsvColumn(type, CHAIN,
                        type.getFeatureByBaseName(COREFERENCE_TYPE_FEATURE), CHAIN_ELEMENT_TYPE));
                schema.addColumn(new TsvColumn(type, CHAIN,
                        type.getFeatureByBaseName(COREFERENCE_RELATION_FEATURE), CHAIN_LINK_TYPE));
                chainLinkTypes.add(type);
                break;
            case SPAN:
                schema.addColumn(new TsvColumn(type, SPAN));
                generateColumns(aTypeSystem, schema, SPAN, type);
                break;
            case INCOMPATIBLE:
                // Do not generate a column definition for incompatible types.
                incompatibleTypes.add(type);
                break;
            }
        }

        // Scan again for the chain head types
        Type topType = aTypeSystem.getType(CAS.TYPE_NAME_ANNOTATION_BASE);
        for (Type type : aTypeSystem.getDirectSubtypes(topType)) {
            Feature firstFeat = type.getFeatureByBaseName(CHAIN_FIRST_FEAT);
            if (firstFeat != null && chainLinkTypes.contains(firstFeat.getRange())) {
                schema.addChainHeadType(type);
                incompatibleTypes.remove(type);
            }
        }

        incompatibleTypes.forEach(schema::ignoreType);

        return schema;
    }

    private static void generateColumns(TypeSystem aTypeSystem, TsvSchema aSchema,
            LayerType aLayerType, Type aType)
    {
        List<String> specialFeatures = asList(CAS.FEATURE_FULL_NAME_BEGIN,
                CAS.FEATURE_FULL_NAME_END, CAS.FEATURE_FULL_NAME_SOFA);

        for (Feature feat : aType.getFeatures()) {
            if (specialFeatures.contains(feat.getName())) {
                continue;
            }

            if (isPrimitiveFeature(feat)) {
                aSchema.addColumn(new TsvColumn(aType, aLayerType, feat, FeatureType.PRIMITIVE));
            }
            else if (SPAN.equals(aLayerType) && isSlotFeature(aTypeSystem, feat)) {
                aSchema.addColumn(new TsvColumn(aType, aLayerType, feat, FeatureType.SLOT_ROLE));
                Type slotTargetType = feat.getRange().getComponentType()
                        .getFeatureByBaseName(FEAT_SLOT_TARGET).getRange();
                TsvColumn targetColumn = new TsvColumn(aType, aLayerType, feat,
                        FeatureType.SLOT_TARGET);
                targetColumn.setTargetTypeHint(slotTargetType);
                aSchema.addColumn(targetColumn);
            }
            else if (CAS.TYPE_NAME_STRING_ARRAY.equals(feat.getRange().getName())) {
                LOG.debug("Multi-value string features are not supported by WebAnno TSV: [{}]",
                        feat.getName());
            }
        }
    }

    private static boolean isSlotFeature(TypeSystem aTypeSystem, Feature feat)
    {
        // This could be written more efficiently using a single conjunction. The reason this
        // has not been done is to facilitate debugging.

        boolean multiValued = feat.getRange().isArray() || aTypeSystem
                .subsumes(aTypeSystem.getType(CAS.TYPE_NAME_LIST_BASE), feat.getRange());

        if (!multiValued) {
            return false;
        }

        boolean linkInheritsFromTop = CAS.TYPE_NAME_TOP
                .equals(aTypeSystem.getParent(feat.getRange().getComponentType()).getName());
        boolean hasTargetFeature = feat.getRange().getComponentType()
                .getFeatureByBaseName(FEAT_SLOT_TARGET) != null;
        boolean hasRoleFeature = feat.getRange().getComponentType()
                .getFeatureByBaseName(FEAT_SLOT_ROLE) != null;

        return linkInheritsFromTop && hasTargetFeature && hasRoleFeature;
    }

    public static boolean isRelationLayer(Type aType)
    {
        Feature relSourceFeat = aType.getFeatureByBaseName(FEAT_REL_SOURCE);
        boolean hasSourceFeature = relSourceFeat != null && !isPrimitiveFeature(relSourceFeat);
        Feature relTargetFeat = aType.getFeatureByBaseName(FEAT_REL_TARGET);
        boolean hasTargetFeature = relTargetFeat != null && !isPrimitiveFeature(relTargetFeat);

        boolean compatible = true;
        for (Feature feat : aType.getFeatures()) {
            if (CAS.FEATURE_BASE_NAME_SOFA.equals(feat.getShortName())
                    || FEAT_REL_SOURCE.equals(feat.getShortName())
                    || FEAT_REL_TARGET.equals(feat.getShortName())) {
                continue;
            }

            if (!isPrimitiveFeature(feat)) {
                compatible = false;
                // LOG.debug("Incompatible feature in type [" + aType + "]: " + feat);
                break;
            }
        }

        return hasSourceFeature && hasTargetFeature && compatible;
    }

    public static boolean isChainLayer(Type aType)
    {
        boolean hasTypeFeature = aType.getFeatureByBaseName(COREFERENCE_TYPE_FEATURE) != null;
        boolean hasRelationFeature = aType
                .getFeatureByBaseName(COREFERENCE_RELATION_FEATURE) != null;
        boolean nameEndsInLink = aType.getName().endsWith("Link");

        boolean compatible = true;
        for (Feature feat : aType.getFeatures()) {
            if (CAS.FEATURE_BASE_NAME_SOFA.equals(feat.getShortName())
                    || CHAIN_NEXT_FEAT.equals(feat.getShortName())
                    || COREFERENCE_TYPE_FEATURE.equals(feat.getShortName())
                    || COREFERENCE_RELATION_FEATURE.equals(feat.getShortName())) {
                continue;
            }

            if (!isPrimitiveFeature(feat)) {
                compatible = false;
                LOG.debug("Incompatible feature in type [" + aType + "]: " + feat);
                break;
            }
        }

        return hasTypeFeature && hasRelationFeature && nameEndsInLink && compatible;
    }

    public static boolean isSpanLayer(Type aType)
    {
        boolean compatible = true;
        for (Feature feat : aType.getFeatures()) {
            if (CAS.FEATURE_BASE_NAME_SOFA.equals(feat.getShortName())) {
                continue;
            }

            // We ignore multi-value string features which are a new feature in INCEpTION but will
            // not be supported by WebAnno TSV 3.x anymore. This should allow to at least export
            // the compatible information while skipping over the multi-value strings.
            if (CAS.TYPE_NAME_STRING_ARRAY.equals(feat.getRange().getName())) {
                continue;
            }

            if (!(isPrimitiveFeature(feat) || isSlotFeature(feat))) {
                compatible = false;
                // LOG.debug("Incompatible feature in type [" + aType + "]: " + feat);
                break;
            }
        }

        return compatible;
    }

    public static boolean isSlotFeature(Feature aFeature)
    {
        if (aFeature.getRange().isArray()) {
            Type elementType = aFeature.getRange().getComponentType();

            return elementType.getFeatureByBaseName(FEAT_SLOT_TARGET) != null
                    && elementType.getFeatureByBaseName(FEAT_SLOT_ROLE) != null;
        }

        return false;
    }

    public static boolean isPrimitiveFeature(Feature aFeature)
    {
        return aFeature.getRange().isPrimitive();
    }
}
