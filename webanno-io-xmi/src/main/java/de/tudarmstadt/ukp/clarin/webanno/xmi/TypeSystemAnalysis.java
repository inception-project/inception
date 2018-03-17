/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.xmi;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;

public class TypeSystemAnalysis
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private final List<AnnotationLayer> layers = new ArrayList<>();
    private final ListValuedMap<String, AnnotationFeature> features = 
            new ArrayListValuedHashMap<>();
    
    public TypeSystemAnalysis()
    {
        // Nothing to do
    }

    public List<AnnotationLayer> getLayers()
    {
        return layers;
    }
    
    public List<AnnotationFeature> getFeatures(String aLayerName)
    {
        return features.get(aLayerName);
    }
    
    public static TypeSystemAnalysis of(TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        TypeSystemAnalysis analyzer = new TypeSystemAnalysis();
        
        // We create a CAS from which we can obtain an instantiated type system. With that, it
        // is easier to check type inheritance.
        CAS cas = CasCreationUtils.createCas(aTSD, null, null);

        for (TypeDescription td : aTSD.getTypes()) {
            analyzer.analyze(cas.getTypeSystem(), aTSD, td);
        }
        
        return analyzer;
    }
    
    private void analyze(TypeSystem aTS, TypeSystemDescription aTSD, TypeDescription aTD) {
        log.debug("Analyzing [{}]", aTD.getName());
        
        Type typeAnnotation = aTS.getType(CAS.TYPE_NAME_ANNOTATION);
        Type type = aTS.getType(aTD.getName());
        
        boolean isChain = isChainLayer(aTS, type);
        
        // Layers must be sub-types of Annotation (unless they are chains)
        if (!isChain && !aTS.subsumes(typeAnnotation, type)) {
            log.debug("[{}] is not an annotation type. Skipping.", aTD.getName());
            return;
        }

        // Actually, layers must inherit directly from Annotation (unless they are chains)
        // because WebAnno presently does not support DKPro Core elevated types (e.g. NOUN).
        if (!isChain && !aTS.getParent(type).equals(aTS.getType(CAS.TYPE_NAME_ANNOTATION))) {
            log.debug("[{}] does not inherit from Annotation, it might be an elevated type "
                    + "which is currently not supported. Skipping.", aTD.getName());
            return;
        }

        AnnotationLayer layer = new AnnotationLayer();        
        
        if (isChain) {
            layer.setName(removeEnd(aTD.getName(), "Chain"));
            layer.setUiName(removeEnd(type.getShortName(), "Chain"));
            layer.setType(CHAIN_TYPE);
        }
        else if (isRelationLayer(aTS, type)) {
            layer.setName(aTD.getName());
            layer.setUiName(type.getShortName());
            layer.setType(RELATION_TYPE);
        }
        else if (isSpanLayer(aTS, type)) {
            layer.setName(aTD.getName());
            layer.setUiName(type.getShortName());
            layer.setType(SPAN_TYPE);
        }
        else {
            log.debug("Unable to determine layer type for [{}]", type.getName());
            return;
        }
        
        log.debug("[{}] seems to be a {}", aTD.getName(), layer.getType());
        
        layer.setDescription(trimToNull(aTD.getDescription()));
        
        layer.setEnabled(true);
        layer.setBuiltIn(false);
        
        // We cannot determine good values for these without looking at actual annotations, thus
        // we choose the most relaxed/permissive configuration here.
        layer.setAllowStacking(true);
        layer.setCrossSentence(true);
        layer.setLockToTokenOffset(false);
        layer.setMultipleTokens(false);
        layer.setLinkedListBehavior(false);
        
        layers.add(layer);
        
        // If the current layer is a chain layer (chain head), then we do not record
        // any features for it - instead we record the features of the link type.
        if (CHAIN_TYPE.equals(layer.getType())) {
            TypeDescription linkTypeDescription = aTSD.getType(layer.getName() + "Link");
            analyzeFeatures(layer, aTS, linkTypeDescription);
        }
        else {
            analyzeFeatures(layer, aTS, aTD);
        }
    }
    
    private void analyzeFeatures(AnnotationLayer aLayer, TypeSystem aTS, TypeDescription aTD)
    {
        Type type = aTS.getType(aTD.getName());
        for (FeatureDescription fd : aTD.getFeatures()) {
            Feature feat = type.getFeatureByBaseName(fd.getName());
            // We do not need to set up built-in features
            if (isBuiltInFeature(feat)) {
                continue;
            }
            
            // If the current layer is a chain layer, then we need to skip the "next" feature
            if (CHAIN_TYPE.equals(aLayer.getType()) && "next".equals(feat.getShortName())) {
                continue;
            }
            
            AnnotationFeature f = analyze(aTS, fd, feat);
            features.put(aLayer.getName(), f);
        }
    }
    
    private AnnotationFeature analyze(TypeSystem aTS, FeatureDescription aFD, Feature aFeat) {
        AnnotationFeature feat = new AnnotationFeature();
        feat.setType(aFeat.getRange().getName());
        feat.setName(aFeat.getShortName());
        feat.setUiName(aFeat.getShortName());
        feat.setDescription(trimToNull(aFD.getDescription()));
        
        feat.setEnabled(true);
        
        if (isSlotFeature(aTS, aFeat)) {
            feat.setType(aFeat.getRange().getComponentType().getFeatureByBaseName("target")
                    .getRange().getName());
            feat.setLinkMode(LinkMode.WITH_ROLE);
            feat.setLinkTypeName(aFeat.getRange().getName());
            // FIXME Instead of hard-coding the feature names here, try to auto-detect them by
            // looking for a String feature and a feature whose type is subsumed by Annotation
            feat.setLinkTypeRoleFeatureName("role");
            feat.setLinkTypeTargetFeatureName("target");
        }
        
        return feat;
    }
        
    private boolean isSpanLayer(TypeSystem aTS, Type aType)
    {
        // A UIMA type can be a relation layer if...
        // ... there are only primitive features or slot features
        
        List<Feature> nonPrimitiveFeatures = aType.getFeatures().stream()
                .filter(f -> !isBuiltInFeature(f))
                .filter(f -> !(f.getRange().isPrimitive() || isSlotFeature(aTS, f)))
                .collect(Collectors.toList());
        
        return nonPrimitiveFeatures.isEmpty();
    }

    private boolean isRelationLayer(TypeSystem aTS, Type aType)
    {
        // A UIMA type can be a relation layer if...
        // ... there are exactly two non-primitive features
        // ... both have the same range
        // ... the range is a span layer

        List<Feature> nonPrimitiveFeatures = aType.getFeatures().stream()
                .filter(f -> !isBuiltInFeature(f))
                .filter(f -> !f.getRange().isPrimitive())
                .collect(Collectors.toList());
        
        // ... there are exactly two non-primitive features
        if (nonPrimitiveFeatures.size() != 2) {
            return false;
        }
        
        Feature ref1 = nonPrimitiveFeatures.get(0);
        Feature ref2 = nonPrimitiveFeatures.get(1);
        
        // ... both have the same range
        if (!ref1.getRange().getName().equals(ref2.getRange().getName())) {
            return false;
        }
        
        // ... the range is a span layer
        // Well, we will not test this in detail at the moment and assume that any sub-type of
        // Annotation should be fine.
        Type typeAnnotation = aTS.getType(CAS.TYPE_NAME_ANNOTATION);
        if (!aTS.subsumes(typeAnnotation, ref1.getRange())) {
            return false;
        }
        
        // Hm, ok, so this looks like a relation layer.
        return true;
    }
    
    private boolean isChainLayer(TypeSystem aTS, Type aType)
    {
        // Chains created within WebAnno always have the suffix "Chain", likewise does the
        // DKPro Core CoreferenceChain - we expect that all chains follow this convention.
        if (!aType.getName().endsWith("Chain")) {
            return false;
        }
        
        // There must be an initial chain feature. The name of this feature is currently 
        // hard-coded in WebAnno to the one used by the DKPro Core CoreferenceChain
        Feature first = aType.getFeatureByBaseName("first");
        if (first == null) {
            return false;
        }
        
        // The initial chain features must be a non-primitive
        if (first.getRange().isPrimitive()) {
            return false;
        }
        
        // The chain head type is not an anchored annotations, thus it must inherit from
        // AnnotationBase
        if (!aTS.getParent(aType).equals(aTS.getType(CAS.TYPE_NAME_ANNOTATION_BASE))) {
            return false;
        }
        
        // Hm, ok, so this looks like a chain layer.
        return true;
    }
    
    private boolean isBuiltInFeature(Feature aFeature)
    {
        Set<String> builtInTypes = new HashSet<>(asList(
                CAS.TYPE_NAME_TOP, 
                CAS.TYPE_NAME_ANNOTATION_BASE, 
                CAS.TYPE_NAME_ANNOTATION));
        return builtInTypes.contains(aFeature.getDomain().getName());
    }
    
    private boolean isSlotFeature(TypeSystem aTS, Feature aFeature)
    {
        // Slot features are multi-valued
        if (!FSUtil.isMultiValuedFeature(aTS, aFeature)) {
            return false;
        }
        
        Type linkType = aFeature.getRange().getComponentType();
        
        // The range of the slot feature is its link type which must inherit from TOP
        if (!aTS.getParent(linkType).equals(aTS.getTopType())) {
            return false;
        }
        
        // The link feature must have exactly two features (link-with-role)
        if (linkType.getFeatures().size() != 2) {
            return false;
        }
        
        Optional<Feature> roleFeature = linkType.getFeatures().stream()
                .filter(f -> f.getRange().getName().equals(CAS.TYPE_NAME_STRING)).findFirst();
        if (!roleFeature.isPresent()) {
            return false;
        }

        Optional<Feature> linkFeature = linkType.getFeatures().stream()
                .filter(f -> !f.getRange().isPrimitive()).findFirst();
        if (!linkFeature.isPresent()) {
            return false;
        }

        // Hm, ok, so this looks like a slot feature.
        return true;
    }
}
