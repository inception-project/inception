/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.brat.render;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.brat.adapter.TypeUtil.getAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.uima.jcas.JCas;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.brat.adapter.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.adapter.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.action.ActionContext;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.EntityType;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.RelationType;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * an Ajax Controller for the BRAT Front End. Most of the actions such as getCollectionInformation ,
 * getDocument, createArc, CreateSpan, deleteSpan, DeleteArc,... are implemented. Besides returning
 * the JSON response to the brat FrontEnd, This controller also manipulates creation of annotation
 * Documents
 */
public class BratRenderer
{
    /**
     * wrap JSON responses to BRAT visualizer
     *
     * @param aResponse
     *            the response.
     * @param aBModel
     *            the annotator model.
     * @param aJCas
     *            the JCas.
     * @param aAnnotationService
     *            the annotation service.s
     */
    public static void render(GetDocumentResponse aResponse, ActionContext aBModel,
            JCas aJCas, AnnotationService aAnnotationService)
    {
        aResponse.setRtlMode(ScriptDirection.RTL.equals(aBModel.getScriptDirection()));

        // Render invisible baseline annotations (sentence, tokens)
        SpanAdapter.renderTokenAndSentence(aJCas, aResponse, aBModel);

        // Render visible (custom) layers
        Map<String[], Queue<String>> colorQueues = new HashMap<>();
        for (AnnotationLayer layer : aBModel.getAnnotationLayers()) {
            if (layer.getName().equals(Token.class.getName())
                    || layer.getName().equals(Sentence.class.getName())
                    || (layer.getType().equals(CHAIN_TYPE)
                            && (aBModel.getMode().equals(Mode.AUTOMATION)
                                    || aBModel.getMode().equals(Mode.CORRECTION)
                                    || aBModel.getMode().equals(Mode.CURATION)))
                    || !layer.isEnabled()) { /* Hide layer if not enabled */
                continue;
            }

            ColoringStrategy coloringStrategy = ColoringStrategy.getBestStrategy(
                    aAnnotationService, layer, aBModel.getPreferences(), colorQueues);

            List<AnnotationFeature> features = aAnnotationService.listAnnotationFeature(layer);
            List<AnnotationFeature> invisibleFeatures = new ArrayList<AnnotationFeature>();
            for (AnnotationFeature feature : features) {
                if (!feature.isVisible()) {
                    invisibleFeatures.add(feature);
                }
            }
            features.removeAll(invisibleFeatures);
            TypeAdapter adapter = getAdapter(aAnnotationService, layer);
            adapter.render(aJCas, features, aResponse, aBModel, coloringStrategy);
        }
    }

    /**
     * Generates brat type definitions from the WebAnno layer definitions.
     *
     * @param aAnnotationLayers
     *            the layers
     * @param aAnnotationService
     *            the annotation service
     * @return the brat type definitions
     */
    public static Set<EntityType> buildEntityTypes(List<AnnotationLayer> aAnnotationLayers,
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

        // Now build the actual configuration
        Set<EntityType> entityTypes = new LinkedHashSet<EntityType>();
        for (AnnotationLayer layer : layers) {
            EntityType entityType = configureEntityType(layer);

            List<RelationType> arcs = new ArrayList<>();
            
            // For link features, we also need to configure the arcs, even though there is no arc
            // layer here.
            boolean hasLinkFeatures = false;
            for (AnnotationFeature f : aAnnotationService.listAnnotationFeature(layer)) {
                if (!LinkMode.NONE.equals(f.getLinkMode())) {
                    hasLinkFeatures = true;
                    break;
                }
            }
            if (hasLinkFeatures) {
                String bratTypeName = getBratTypeName(layer);
                arcs.add(new RelationType(layer.getName(), layer.getUiName(), bratTypeName,
                        bratTypeName, null, "triangle,5", "3,3"));
            }

            // Styles for the remaining relation and chain layers
            for (AnnotationLayer attachingLayer : getAttachingLayers(layer, layers,
                    aAnnotationService)) {
                arcs.add(configureRelationType(layer, attachingLayer));
            }

            entityType.setArcs(arcs);
            entityTypes.add(entityType);
        }

        return entityTypes;
    }

    /**
     * Scan through the layers once to remember which layers attach to which layers.
     */
    private static List<AnnotationLayer> getAttachingLayers(AnnotationLayer aTarget,
            List<AnnotationLayer> aLayers, AnnotationService aAnnotationService)
    {
        List<AnnotationLayer> attachingLayers = new ArrayList<>();

        // Chains always attach to themselves
        if (CHAIN_TYPE.equals(aTarget.getType())) {
            attachingLayers.add(aTarget);
        }

        // FIXME This is a hack! Actually we should check the type of the attachFeature when
        // determine which layers attach to with other layers. Currently we only use attachType,
        // but do not follow attachFeature if it is set.
        if (aTarget.isBuiltIn() && aTarget.getName().equals(POS.class.getName())) {
            attachingLayers.add(aAnnotationService.getLayer(Dependency.class.getName(),
                    aTarget.getProject()));
        }

        // Custom layers
        for (AnnotationLayer l : aLayers) {
            if (aTarget.equals(l.getAttachType())) {
                attachingLayers.add(l);
            }
        }

        return attachingLayers;
    }

    private static EntityType configureEntityType(AnnotationLayer aLayer)
    {
        String bratTypeName = getBratTypeName(aLayer);
        return new EntityType(aLayer.getName(), aLayer.getUiName(), bratTypeName);
    }

    private static RelationType configureRelationType(AnnotationLayer aLayer,
            AnnotationLayer aAttachingLayer)
    {
        String attachingLayerBratTypeName = TypeUtil.getBratTypeName(aAttachingLayer);
        // FIXME this is a hack because the chain layer consists of two UIMA types, a "Chain"
        // and a "Link" type. ChainAdapter always seems to use "Chain" but some places also
        // still use "Link" - this should be cleaned up so that knowledge about "Chain" and
        // "Link" types is local to the ChainAdapter and not known outside it!
        if (aLayer.getType().equals(CHAIN_TYPE)) {
            attachingLayerBratTypeName += ChainAdapter.CHAIN;
        }

        // Handle arrow-head styles depending on linkedListBehavior
        String arrowHead;
        if (aLayer.getType().equals(CHAIN_TYPE) && !aLayer.isLinkedListBehavior()) {
            arrowHead = "none";
        }
        else {
            arrowHead = "triangle,5";
        }

        String dashArray;
        switch (aLayer.getType()) {
        case CHAIN_TYPE:
            dashArray = "5,1";
            break;
        default:
            dashArray = "";
            break;
        }

        String bratTypeName = getBratTypeName(aLayer);
        RelationType arc = new RelationType(aAttachingLayer.getName(), aAttachingLayer.getUiName(),
                attachingLayerBratTypeName, bratTypeName, null, arrowHead, dashArray);
        return arc;
    }

    private static String getBratTypeName(AnnotationLayer aLayer)
    {
        String bratTypeName = TypeUtil.getBratTypeName(aLayer);

        // FIXME this is a hack because the chain layer consists of two UIMA types, a "Chain"
        // and a "Link" type. ChainAdapter always seems to use "Chain" but some places also
        // still use "Link" - this should be cleaned up so that knowledge about "Chain" and
        // "Link" types is local to the ChainAdapter and not known outside it!
        if (aLayer.getType().equals(CHAIN_TYPE)) {
            bratTypeName += ChainAdapter.CHAIN;
        }
        return bratTypeName;
    }
}
