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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.util;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureSupport.FEATURE_NAME_ROLE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureSupport.FEATURE_NAME_TARGET;
import static de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport.FEATURE_NAME_FIRST;
import static de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport.FEATURE_NAME_NEXT;
import static de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport.TYPE_SUFFIX_CHAIN;
import static de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport.TYPE_SUFFIX_LINK;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION_BASE;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOCUMENT_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_TOP;
import static org.apache.uima.fit.util.FSUtil.isMultiValuedFeature;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class TypeSystemAnalysis
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<AnnotationLayer> layers = new ArrayList<>();
    private final ListValuedMap<String, AnnotationFeature> features = new ArrayListValuedHashMap<>();
    private final Map<String, RelationDetails> relationDetailMap = new HashMap<>();
    private final Map<String, ChainDetails> chainDetailMap = new HashMap<>();

    public TypeSystemAnalysis()
    {
        // Nothing to do
    }

    public Optional<AnnotationLayer> getLayer(String aLayerName)
    {
        return layers.stream() //
                .filter(l -> aLayerName.equals(l.getName())) //
                .findFirst();
    }

    public List<AnnotationLayer> getLayers()
    {
        return layers;
    }

    public List<AnnotationFeature> getFeatures(String aLayerName)
    {
        return features.get(aLayerName);
    }

    public RelationDetails getRelationDetails(String aLayerName)
    {
        return relationDetailMap.get(aLayerName);
    }

    public ChainDetails getChainDetails(String aLayerName)
    {
        return chainDetailMap.get(aLayerName);
    }

    public static TypeSystemAnalysis of(TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        var analyzer = new TypeSystemAnalysis();
        analyzer.analyzeTypeSystem(aTSD);
        return analyzer;
    }

    private void analyzeTypeSystem(TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        // We create a CAS from which we can obtain an instantiated type system. With that, it
        // is easier to check type inheritance.
        var cas = WebAnnoCasUtil.createCas(aTSD);
        var ts = cas.getTypeSystem();

        for (var td : aTSD.getTypes()) {
            analyzeType(ts, aTSD, td);
        }

        LOG.debug("Recognized {} of {} types as layers ({}%)", getLayers().size(),
                aTSD.getTypes().length, getLayers().size() * 100 / aTSD.getTypes().length);
    }

    private void analyzeType(TypeSystem aTS, TypeSystemDescription aTSD, TypeDescription aTD)
    {
        LOG.trace("Analyzing [{}]", aTD.getName());

        var type = aTS.getType(aTD.getName());

        // Skip built-in UIMA types
        if (aTD.getName().startsWith("uima.tcas.")) {
            LOG.debug("[{}] is a built-in UIMA type. Skipping.", aTD.getName());
            return;
        }

        // Skip document metadata types
        if (aTS.subsumes(aTS.getType(TYPE_NAME_DOCUMENT_ANNOTATION), type)) {
            LOG.debug("[{}] is a document-level annotation. Skipping.", type.getName());
            return;
        }

        // Skip DKPro Core Token and Sentence which as handled specially by WebAnno
        if (Sentence._TypeName.equals(aTD.getName()) || Token._TypeName.equals(aTD.getName())) {
            LOG.debug("[{}] is a DKPro Core segmentation type. Skipping.", aTD.getName());
            return;
        }

        var relationDetails = analyzeRelationLayer(aTS, type);
        var chainDetails = analyzeChainLayer(aTS, type);
        var isChain = chainDetails.isPresent();

        // Layers must be sub-types of Annotation (unless they are chains)
        if (!isChain && !aTS.subsumes(aTS.getType(TYPE_NAME_ANNOTATION), type)) {
            LOG.debug("[{}] is not an annotation type. Skipping.", aTD.getName());
            return;
        }

        // Actually, layers must inherit directly from Annotation (unless they are chains)
        // because WebAnno presently does not support DKPro Core elevated types (e.g. NOUN).
        if (isElevatedType(aTS, type)) {
            LOG.debug("[{}] looks like an elevated type. Skipping.", aTD.getName());
            return;
        }

        var layer = new AnnotationLayer();
        Optional<? extends LayerDetails> details;
        if (isChain) {
            details = chainDetails;
            layer.setName(removeEnd(aTD.getName(), TYPE_SUFFIX_CHAIN));
            layer.setUiName(removeEnd(type.getShortName(), TYPE_SUFFIX_CHAIN));
            layer.setType(ChainLayerSupport.TYPE);
            chainDetailMap.put(layer.getName(), chainDetails.get());
        }
        else if (relationDetails.isPresent()) {
            details = relationDetails;
            layer.setName(aTD.getName());
            layer.setUiName(type.getShortName());
            layer.setType(RelationLayerSupport.TYPE);
            relationDetailMap.put(layer.getName(), relationDetails.get());
        }
        else if (isSpanLayer(aTS, type)) {
            details = empty();
            layer.setName(aTD.getName());
            layer.setUiName(type.getShortName());
            layer.setType(SpanLayerSupport.TYPE);
        }
        else {
            LOG.debug("Unable to determine layer type for [{}]", type.getName());
            return;
        }

        LOG.debug("[{}] seems to be a {}", aTD.getName(), layer.getType());

        layer.setDescription(trimToNull(aTD.getDescription()));

        layer.setEnabled(true);
        layer.setBuiltIn(false);

        // We cannot determine good values for these without looking at actual annotations, thus
        // we choose the most relaxed/permissive configuration here.
        layer.setOverlapMode(ANY_OVERLAP);
        layer.setCrossSentence(true);
        layer.setAnchoringMode(CHARACTERS);
        layer.setLinkedListBehavior(false);

        layers.add(layer);

        // If the current layer is a chain layer (chain head), then we do not record
        // any features for it - instead we record the features of the link type.
        if (ChainLayerSupport.TYPE.equals(layer.getType())) {
            var linkTypeDescription = aTSD.getType(layer.getName() + TYPE_SUFFIX_LINK);
            analyzeFeatures(layer, aTS, linkTypeDescription, details);
        }
        else {
            analyzeFeatures(layer, aTS, aTD, details);
        }
    }

    private void analyzeFeatures(AnnotationLayer aLayer, TypeSystem aTS, TypeDescription aTD,
            Optional<? extends LayerDetails> aDetails)
    {
        var type = aTS.getType(aTD.getName());
        for (var fd : aTD.getFeatures()) {
            var feat = type.getFeatureByBaseName(fd.getName());
            // We do not need to set up built-in features
            if (isBuiltInFeature(feat)) {
                continue;
            }

            if (aDetails.isPresent() && aDetails.get().isHiddenFeature(feat)) {
                continue;
            }

            AnnotationFeature f = analyzeFeature(aTS, fd, feat);
            features.put(aLayer.getName(), f);
        }
    }

    private AnnotationFeature analyzeFeature(TypeSystem aTS, FeatureDescription aFD, Feature aFeat)
    {
        var feat = new AnnotationFeature();
        feat.setType(aFeat.getRange().getName());
        feat.setName(aFeat.getShortName());
        feat.setUiName(aFeat.getShortName());
        feat.setDescription(trimToNull(aFD.getDescription()));
        feat.setEnabled(true);

        if (TYPE_NAME_STRING_ARRAY.equals(aFeat.getRange().getName())) {
            feat.setMode(ARRAY);
        }

        if (isSlotFeature(aTS, aFeat)) {
            feat.setType(aFeat.getRange().getComponentType()
                    .getFeatureByBaseName(FEATURE_NAME_TARGET).getRange().getName());
            feat.setMode(ARRAY);
            feat.setLinkMode(WITH_ROLE);
            // Need to strip the "[]" marking the type as multi-valued off the type name
            feat.setLinkTypeName(removeEnd(aFeat.getRange().getName(), "[]"));
            // FIXME Instead of hard-coding the feature names here, try to auto-detect them by
            // looking for a String feature and a feature whose type is subsumed by Annotation
            feat.setLinkTypeRoleFeatureName(FEATURE_NAME_ROLE);
            feat.setLinkTypeTargetFeatureName(FEATURE_NAME_TARGET);
        }

        return feat;
    }

    private boolean isSpanLayer(TypeSystem aTS, Type aType)
    {
        // A UIMA type can be a span layer if there are only ...
        // ... primitive features
        // ... string arrays
        // ... slot features

        return aType.getFeatures().stream() //
                .allMatch(f -> isBuiltInFeature(f) || f.getRange().isPrimitive()
                        || isSlotFeature(aTS, f) || isSupportedArrayType(f));
    }

    private Optional<RelationDetails> analyzeRelationLayer(TypeSystem aTS, Type aType)
    {
        // A UIMA type can be a relation layer if...
        // ... there are exactly two non-primitive features
        // ... both have the same range
        // ... the range is a span layer

        var nonPrimitiveFeatures = aType.getFeatures() //
                .stream().filter(f -> !isBuiltInFeature(f)) //
                .filter(f -> !(f.getRange().isPrimitive() || isSupportedArrayType(f))) //
                .toList();

        // ... there are exactly two non-primitive features
        if (nonPrimitiveFeatures.size() != 2) {
            return empty();
        }

        var ref1 = nonPrimitiveFeatures.get(0);
        var ref2 = nonPrimitiveFeatures.get(1);

        // Relations must use the names "Governor" and "Dependent" for its references because
        // these names are hard-coded throughout WebAnno.
        var validNames = asList(FEAT_REL_SOURCE, FEAT_REL_TARGET);
        if (!validNames.contains(ref1.getShortName())
                || !validNames.contains(ref2.getShortName())) {
            return empty();
        }

        // ... both have the same range
        if (!ref1.getRange().getName().equals(ref2.getRange().getName())) {
            return empty();
        }

        // ... the range is a span layer
        // Well, we will not test this in detail at the moment and assume that any sub-type of
        // Annotation should be fine.
        if (!aTS.subsumes(aTS.getType(TYPE_NAME_ANNOTATION), ref1.getRange())) {
            return empty();
        }

        var details = new RelationDetails();
        details.attachLayer = ref1.getRange().getName();
        details.sourceFeature = FEAT_REL_SOURCE;
        details.targetFeature = FEAT_REL_TARGET;

        // Hm, ok, so this looks like a relation layer.
        return Optional.of(details);
    }

    private Optional<ChainDetails> analyzeChainLayer(TypeSystem aTS, Type aType)
    {
        // Chains created within WebAnno always have the suffix "Chain", likewise does the
        // DKPro Core CoreferenceChain - we expect that all chains follow this convention.
        if (!aType.getName().endsWith(TYPE_SUFFIX_CHAIN)) {
            return empty();
        }

        // There must be an initial chain feature. The name of this feature is currently
        // hard-coded in WebAnno to the one used by the DKPro Core CoreferenceChain
        var first = aType.getFeatureByBaseName(FEATURE_NAME_FIRST);
        if (first == null) {
            return empty();
        }

        // The initial chain features must be a non-primitive
        if (first.getRange().isPrimitive()) {
            return empty();
        }

        // The chain link must contain a feature named "next" used to connect to the next link
        var next = first.getRange().getFeatureByBaseName(FEATURE_NAME_NEXT);
        if (next == null || !next.getRange().equals(first.getRange())) {
            return empty();
        }

        // The chain head type is not an anchored annotations, thus it must inherit from
        // AnnotationBase
        if (!aTS.getParent(aType).equals(aTS.getType(TYPE_NAME_ANNOTATION_BASE))) {
            return empty();
        }

        var details = new ChainDetails();
        details.nextFeature = FEATURE_NAME_NEXT;

        // Hm, ok, so this looks like a chain layer.
        return Optional.of(details);
    }

    private boolean isElevatedType(TypeSystem aTS, Type aType)
    {
        // Elevated types do not contribute any new features
        if (aType.getFeatures().stream().anyMatch(f -> f.getDomain().equals(aType))) {
            return false;
        }

        // Elevated types have only sub-types which are also elevated types
        if (aTS.getDirectSubtypes(aType).stream().anyMatch(t -> !isElevatedType(aTS, t))) {
            return false;
        }

        // Elevated types do not inherit from Annotation
        if (aTS.getParent(aType).equals(aTS.getType(TYPE_NAME_ANNOTATION))) {
            return false;
        }

        // Hm, ok, so this looks like an elevated type
        return true;
    }

    private boolean isBuiltInFeature(Feature aFeature)
    {
        return switch (aFeature.getDomain().getName()) {
        case TYPE_NAME_TOP, TYPE_NAME_ANNOTATION_BASE, TYPE_NAME_ANNOTATION -> true;
        default -> false;
        };
    }

    private boolean isSupportedArrayType(Feature aFeature)
    {
        return switch (aFeature.getRange().getName()) {
        case TYPE_NAME_STRING_ARRAY -> true;
        default -> false;
        };
    }

    private boolean isSlotFeature(TypeSystem aTS, Feature aFeature)
    {
        // Slot features are multi-valued
        if (!isMultiValuedFeature(aTS, aFeature)) {
            return false;
        }

        // The component type is the link type - it must be present
        var linkType = aFeature.getRange().getComponentType();
        if (linkType == null) {
            return false;
        }

        // The range of the slot feature is its link type which must inherit from TOP
        if (!aTS.getTopType().equals(aTS.getParent(linkType))) {
            return false;
        }

        // The link feature must have exactly two features (link-with-role)
        if (linkType.getFeatures().size() != 2) {
            return false;
        }

        // Does any feature look like the role feature (i.e. a String feature)?
        if (linkType.getFeatures().stream()
                .noneMatch(f -> f.getRange().getName().equals(CAS.TYPE_NAME_STRING))) {
            return false;
        }

        // Does any feature look like the target feature (i.e. a non-primitive feature)?
        if (linkType.getFeatures().stream().noneMatch(f -> !f.getRange().isPrimitive())) {
            return false;
        }

        // Hm, ok, so this looks like a slot feature.
        return true;
    }

    public static interface LayerDetails
    {
        boolean isHiddenFeature(Feature aFeature);
    }

    public static class ChainDetails
        implements LayerDetails
    {
        private String nextFeature;

        public String getNextFeature()
        {
            return nextFeature;
        }

        @Override
        public boolean isHiddenFeature(Feature aFeature)
        {
            return aFeature.getShortName().equals(nextFeature);
        }
    }

    public static class RelationDetails
        implements LayerDetails
    {
        private String attachLayer;
        private String sourceFeature;
        private String targetFeature;

        @Override
        public boolean isHiddenFeature(Feature aFeature)
        {
            return asList(sourceFeature, targetFeature).contains(aFeature.getShortName());
        }

        public String getAttachLayer()
        {
            return attachLayer;
        }

        public void setAttachLayer(String aAttachLayer)
        {
            attachLayer = aAttachLayer;
        }

        public String getSourceFeature()
        {
            return sourceFeature;
        }

        public String getTargetFeature()
        {
            return targetFeature;
        }
    }
}
