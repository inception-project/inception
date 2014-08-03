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

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst.RELATION_TYPE;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.EntityType;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.RelationType;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Generation of brat type definitions.
 *
 * @author Seid Muhie Yimam
 */
public class BratAjaxConfiguration
{
    /**
     * Generates brat type definitions from the WebAnno layer definitions.
     * 
     * @param aAnnotationLayers the layers
     * @param aAnnotationService the annotation service
     * @return the brat type definitions
     */
    public static Set<EntityType> buildEntityTypes(Set<AnnotationLayer> aAnnotationLayers,
            AnnotationService aAnnotationService)
    {
        // Sort layers
        List<AnnotationLayer> layers = new ArrayList<AnnotationLayer>(aAnnotationLayers);
        Collections.sort(layers, new Comparator<AnnotationLayer>()
        {
            @Override
            public int compare(AnnotationLayer o1, AnnotationLayer o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        });
        
        // Scan through the layers once to remember which layers attach to which layers
        Map<AnnotationLayer, AnnotationLayer> attachingLayers = new LinkedHashMap<AnnotationLayer, AnnotationLayer>();
        for (AnnotationLayer layer : layers) {
            if (layer.getType().equals(CHAIN_TYPE)) {
                attachingLayers.put(layer, layer);
            }
            else if (layer.getType().equals(RELATION_TYPE)) {
                // FIXME This implies that at most one relation layer can attach to a span layer
                attachingLayers.put(layer.getAttachType(), layer);
            }
        }

        // Now build the actual configuration
        Set<EntityType> entityTypes = new LinkedHashSet<EntityType>();
        for (AnnotationLayer layer : layers) {
            configureLayer(aAnnotationService, entityTypes, layer,
                    attachingLayers.get(layer));
        }

        return entityTypes;
    }
    
    private static void configureLayer(AnnotationService aAnnotationService,
            Set<EntityType> aEntityTypes, AnnotationLayer aLayer,
            AnnotationLayer aAttachingLayer)
    {
        // FIXME This is a hack! Actually we should check the type of the attachFeature when
        // determine which layers attach to with other layers. Currently we only use attachType,
        // but do not follow attachFeature if it is set.
        if (aLayer.isBuiltIn() && aLayer.getName().equals(POS.class.getName())) {
            aAttachingLayer = aAnnotationService.getLayer(Dependency.class.getName(),
                    aLayer.getProject());
        }
        
        String bratTypeName = TypeUtil.getBratTypeName(aLayer);
        
        // FIXME this is a hack because the chain layer consists of two UIMA types, a "Chain"
        // and a "Link" type. ChainAdapter always seems to use "Chain" but some places also
        // still use "Link" - this should be cleaned up so that knowledge about "Chain" and
        // "Link" types is local to the ChainAdapter and not known outside it!
        if (aLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
            bratTypeName += ChainAdapter.CHAIN;
        }

        EntityType entityType;
        if (aLayer.isBuiltIn() && aLayer.getName().equals(POS.class.getName())) {
            entityType = new EntityType(aLayer.getName(), bratTypeName);
        }
        else if (aLayer.isBuiltIn() && aLayer.getName().equals(NamedEntity.class.getName())) {
            entityType = new EntityType(aLayer.getName(), bratTypeName);
        }
        else if (aLayer.isBuiltIn() && aLayer.getName().equals(Lemma.class.getName())) {
            entityType = new EntityType(aLayer.getName(), bratTypeName);
        }

        // custom layers
        else {
            entityType = new EntityType(aLayer.getName(), bratTypeName);
        }

        if (aAttachingLayer != null) {
            String attachingLayerBratTypeName = TypeUtil.getBratTypeName(aAttachingLayer);
            // FIXME this is a hack because the chain layer consists of two UIMA types, a "Chain"
            // and a "Link" type. ChainAdapter always seems to use "Chain" but some places also
            // still use "Link" - this should be cleaned up so that knowledge about "Chain" and
            // "Link" types is local to the ChainAdapter and not known outside it!
            if (aLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                attachingLayerBratTypeName += ChainAdapter.CHAIN;
            }
            
            // Handle arrow-head styles depending on linkedListBehavior
            String arrowHead;
            if (aLayer.getType().equals(WebAnnoConst.CHAIN_TYPE) && !aLayer.isLinkedListBehavior()) {
                arrowHead = "none";
            }
            else {
                arrowHead = "triangle,5";
            }
            
            RelationType arc = new RelationType(aAttachingLayer.getName(),
                    attachingLayerBratTypeName, bratTypeName, null, arrowHead);
            entityType.setArcs(asList(arc));
        }

        aEntityTypes.add(entityType);
    }
}
