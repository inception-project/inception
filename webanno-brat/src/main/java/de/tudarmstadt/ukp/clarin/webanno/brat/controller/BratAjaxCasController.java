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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.EntityType;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.RelationType;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Stored;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentTimestampResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.ImportDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.LoadConfResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.StoreSvgResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.WhoamiResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * an Ajax Controller for the BRAT Front End. Most of the actions such as getCollectionInformation ,
 * getDocument, createArc, CreateSpan, deleteSpan, DeleteArc,... are implemented. Besides returning
 * the JSON response to the brat FrontEnd, This controller also manipulates creation of annotation
 * Documents
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public class BratAjaxCasController
{
    @Resource(name = "documentRepository")
    private RepositoryService repository;

    @Resource(name = "annotationService")
    private static AnnotationService annotationService;

    public BratAjaxCasController()
    {

    }

    public BratAjaxCasController(RepositoryService aRepository, AnnotationService aAnnotationService)
    {
        annotationService = aAnnotationService;
        this.repository = aRepository;
    }

    /**
     * a protocol which returns the logged in user
     * 
     * @return the response.
     */
    public WhoamiResponse whoami()
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return new WhoamiResponse(username);
    }

    /**
     * some BRAT UI global configurations such as {@code textBackgrounds}
     * 
     * @return the response.
     */
    public LoadConfResponse loadConf()
    {
        return new LoadConfResponse();
    }

    /**
     * This the the method that send JSON response about annotation project information which
     * includes List {@link Tag}s and {@link TagSet}s It includes information about span types
     * {@link POS}, {@link NamedEntity}, and {@link CoreferenceLink#getReferenceType()} and relation
     * types such as {@link Dependency}, {@link CoreferenceChain}
     * 
     * @param aAnnotationLayers the layers.
     * @return the response.
     *
     * @see <a href="http://brat.nlplab.org/index.html">Brat</a>
     */
    public GetCollectionInformationResponse getCollectionInformation(
            List<AnnotationLayer> aAnnotationLayers)
    {
        GetCollectionInformationResponse info = new GetCollectionInformationResponse();
        info.setEntityTypes(buildEntityTypes(aAnnotationLayers, annotationService));
        return info;
    }

    /**
     * Returns the JSON representation of the document for brat visualizer
     * 
     * @param aBratAnnotatorModel the annotator model.
     * @param aAnnotationOffsetStart the begin offset.
     * @param aJCas the JCas.
     * @param aIsGetDocument hum?
     * @return the response
     * @throws UIMAException if a conversion error occurs.
     * @throws IOException if an I/O error occurs.
     * @throws ClassNotFoundException if a DKPro Core reader/writer cannotbe loaded.
     */
    public GetDocumentResponse getDocumentResponse(BratAnnotatorModel aBratAnnotatorModel,
            int aAnnotationOffsetStart, JCas aJCas, boolean aIsGetDocument)
        throws UIMAException, IOException, ClassNotFoundException
    {
        GetDocumentResponse response = new GetDocumentResponse();
        render(response, aBratAnnotatorModel, aAnnotationOffsetStart, aJCas, aIsGetDocument);

        return response;
    }

    /**
     * wrap JSON responses to BRAT visualizer
     * 
     * @param aResponse the response.
     * @param aBratAnnotatorModel the annotator model.
     * @param aAnnotationOffsetStart the begin offset.
     * @param aJCas the JCas.
     * @param aIsGetDocument hum?
     */
    public static void render(GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel, int aAnnotationOffsetStart, JCas aJCas,
            boolean aIsGetDocument)
    {
        // Maybe this section should be moved elsewehere and the aIsGetDocument parameter should
        // be removed, so that this method really only renders and does not additionally update
        // the BratAnnotatorModel state? -- REC
        if (aBratAnnotatorModel.isScrollPage() && !aIsGetDocument) {
            aBratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(aJCas,
                    aBratAnnotatorModel.getSentenceAddress(), aAnnotationOffsetStart,
                    aBratAnnotatorModel.getProject(), aBratAnnotatorModel.getDocument(),
                    aBratAnnotatorModel.getWindowSize()));
        }

        render(aResponse, aBratAnnotatorModel, aJCas, annotationService);
    }

    /**
     * wrap JSON responses to BRAT visualizer
     * 
     * @param aResponse the response.
     * @param aBModel the annotator model.
     * @param aJCas the JCas.
     * @param aAnnotationService the annotation service.s
     */
    public static void render(GetDocumentResponse aResponse, BratAnnotatorModel aBModel,
            JCas aJCas, AnnotationService aAnnotationService)
    {
        // Render invisible baseline annotations (sentence, tokens)
        SpanAdapter.renderTokenAndSentence(aJCas, aResponse, aBModel);

        // Render visible (custom) layers
        int i = 0;
        for (AnnotationLayer layer : aBModel.getAnnotationLayers()) {
            if (layer.getName().equals(Token.class.getName())
                    || layer.getName().equals(Sentence.class.getName())
                    || (layer.getType().equals(WebAnnoConst.CHAIN_TYPE) && (aBModel.getProject()
                            .getMode().equals(Mode.AUTOMATION)
                            || aBModel.getProject().getMode().equals(Mode.CORRECTION) || aBModel
                            .getProject().getMode().equals(Mode.CURATION)))) {
                continue;
            }

            ColoringStrategy coloringStrategy = ColoringStrategy.getBestStrategy(layer, aBModel, i);

            List<AnnotationFeature> features = aAnnotationService.listAnnotationFeature(layer);
            List<AnnotationFeature> invisibleFeatures = new ArrayList<AnnotationFeature>();
            for (AnnotationFeature feature : features) {
                if (!feature.isVisible()) {
                    invisibleFeatures.add(feature);
                }
            }
            features.removeAll(invisibleFeatures);
            TypeAdapter adapter = getAdapter(layer);
            adapter.render(aJCas, features, aResponse, aBModel, coloringStrategy);
            i++;
        }
    }
    
    /**
     * Generates brat type definitions from the WebAnno layer definitions.
     * 
     * @param aAnnotationLayers the layers
     * @param aAnnotationService the annotation service
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

            for (AnnotationLayer attachingLayer : getAttachingLayers(layer, layers, aAnnotationService)) {
                RelationType arc = configureRelationType(layer, attachingLayer);
                entityType.setArcs(asList(arc));
            }
            
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
        
        String bratTypeName = getBratTypeName(aLayer);
        RelationType arc = new RelationType(aAttachingLayer.getName(),
                aAttachingLayer.getUiName(), attachingLayerBratTypeName, bratTypeName, null,
                arrowHead);
        return arc;
    }
    
    private static String getBratTypeName(AnnotationLayer aLayer)
    {
        String bratTypeName = TypeUtil.getBratTypeName(aLayer);
        
        // FIXME this is a hack because the chain layer consists of two UIMA types, a "Chain"
        // and a "Link" type. ChainAdapter always seems to use "Chain" but some places also
        // still use "Link" - this should be cleaned up so that knowledge about "Chain" and
        // "Link" types is local to the ChainAdapter and not known outside it!
        if (aLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
            bratTypeName += ChainAdapter.CHAIN;
        } 
        return bratTypeName;
    }
}