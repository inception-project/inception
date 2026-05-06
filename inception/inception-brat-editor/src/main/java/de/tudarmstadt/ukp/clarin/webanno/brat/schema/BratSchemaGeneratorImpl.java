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
package de.tudarmstadt.ukp.clarin.webanno.brat.schema;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.brat.schema.model.EntityType;
import de.tudarmstadt.ukp.clarin.webanno.brat.schema.model.RelationType;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import jakarta.persistence.NoResultException;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link BratAnnotationEditorAutoConfiguration#bratSchemaGenerator}.
 * </p>
 */
public class BratSchemaGeneratorImpl
    implements BratSchemaGenerator
{
    private final AnnotationSchemaService annotationService;

    public BratSchemaGeneratorImpl(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    /**
     * Generates brat type definitions from the WebAnno layer definitions.
     *
     * @param aProject
     *            the project to which the layers belong
     * @param aAnnotationLayers
     *            the layers
     * @return the brat type definitions
     */
    @Override
    public Set<EntityType> buildEntityTypes(Project aProject,
            List<AnnotationLayer> aAnnotationLayers)
    {
        // Sort layers
        var layers = new ArrayList<AnnotationLayer>(aAnnotationLayers);
        layers.sort(comparing(AnnotationLayer::getName));

        // Look up all the features once to avoid hammering the database in the loop below
        var layerToFeatures = annotationService.listSupportedFeatures(aProject).stream()
                .collect(groupingBy(AnnotationFeature::getLayer));

        // Now build the actual configuration
        var entityTypes = new LinkedHashMap<AnnotationLayer, EntityType>();
        var relationTypes = new LinkedHashMap<AnnotationLayer, RelationType>();
        for (var layer : layers) {
            if (RelationLayerSupport.TYPE.equals(layer.getType())) {
                continue;
            }

            var entityType = configureEntityType(layer);

            var arcs = new LinkedHashSet<RelationType>();

            // For link features, we also need to configure the arcs, even though there is no arc
            // layer here.
            boolean hasLinkFeatures = false;
            for (var f : layerToFeatures.computeIfAbsent(layer, k -> emptyList())) {
                if (!LinkMode.NONE.equals(f.getLinkMode())) {
                    hasLinkFeatures = true;
                    break;
                }
            }

            if (hasLinkFeatures) {
                arcs.add(new RelationType(layer.getUiName(), getBratTypeName(layer),
                        getBratTypeName(layer), null, "triangle,5", "3,3"));
            }

            // Styles for the remaining relation and chain layers
            for (var attachingLayer : getAttachingLayers(layer, layers)) {
                var relationLayer = relationTypes.computeIfAbsent(attachingLayer,
                        $ -> buildRelationType(layer, attachingLayer));
                relationLayer.addTarget(getBratTypeName(layer));
                arcs.add(relationLayer);
            }

            entityType.setArcs(new ArrayList<>(arcs));
            entityTypes.put(layer, entityType);
        }

        return new LinkedHashSet<>(entityTypes.values());
    }

    /**
     * Scan through the layers once to remember which layers attach to which layers.
     */
    private List<AnnotationLayer> getAttachingLayers(AnnotationLayer aTarget,
            List<AnnotationLayer> aLayers)
    {
        // Chains always attach to themselves
        if (ChainLayerSupport.TYPE.equals(aTarget.getType())) {
            return asList(aTarget);
        }

        var attachingLayers = new ArrayList<AnnotationLayer>();

        // FIXME This is a hack! Actually we should check the type of the attachFeature when
        // determine which layers attach to with other layers. Currently we only use attachType,
        // but do not follow attachFeature if it is set.
        if (aTarget.isBuiltIn() && aTarget.getName().equals(POS.class.getName())) {
            try {
                attachingLayers.add(annotationService.findLayer(aTarget.getProject(),
                        Dependency.class.getName()));
            }
            catch (NoResultException e) {
                // If the Dependency layer does not exist in the project, we do not care.
            }
        }

        // Custom layers
        for (var layer : aLayers) {
            // Layer attaches explicitly to other layer
            if (aTarget.equals(layer.getAttachType())) {
                attachingLayers.add(layer);
            }
            // Relation layer that attaches to "any" other span layer
            else if (SpanLayerSupport.TYPE.equals(aTarget.getType())
                    && RelationLayerSupport.TYPE.equals(layer.getType())
                    && layer.getAttachType() == null) {
                attachingLayers.add(layer);
            }
        }

        return attachingLayers;
    }

    private EntityType configureEntityType(AnnotationLayer aLayer)
    {
        var bratTypeName = getBratTypeName(aLayer);
        return new EntityType(aLayer.getName(), aLayer.getUiName(), bratTypeName);
    }

    private RelationType buildRelationType(AnnotationLayer aLayer, AnnotationLayer aAttachingLayer)
    {
        String attachingLayerBratTypeName = getBratTypeName(aAttachingLayer);

        // // FIXME this is a hack because the chain layer consists of two UIMA types, a "Chain"
        // // and a "Link" type. ChainAdapter always seems to use "Chain" but some places also
        // // still use "Link" - this should be cleaned up so that knowledge about "Chain" and
        // // "Link" types is local to the ChainAdapter and not known outside it!
        // if (aLayer.getType().equals(CHAIN_TYPE)) {
        // attachingLayerBratTypeName += ChainAdapter.LINK;
        // }

        // Handle arrow-head styles depending on linkedListBehavior
        String arrowHead;
        if (aLayer.getType().equals(ChainLayerSupport.TYPE) && !aLayer.isLinkedListBehavior()) {
            arrowHead = "none";
        }
        else {
            arrowHead = "triangle,5";
        }

        String dashArray;
        switch (aLayer.getType()) {
        case ChainLayerSupport.TYPE:
            dashArray = "5,1";
            break;
        default:
            dashArray = "";
            break;
        }

        var bratTypeName = getBratTypeName(aLayer);
        return new RelationType(aAttachingLayer.getUiName(), attachingLayerBratTypeName,
                bratTypeName, null, arrowHead, dashArray);
    }

    public static String getBratTypeName(AnnotationLayer aLayer)
    {
        return aLayer.getId().toString();
    }
}
