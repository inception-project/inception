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

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public static final String MIME_TYPE_XML = "application/xml";
    public static final String PRODUCES_JSON = "application/json";
    public static final String PRODUCES_XML = "application/xml";
    public static final String CONSUMES_URLENCODED = "application/x-www-form-urlencoded";

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
     * This Method, a generic Ajax call serves the purpose of returning expected export file types.
     * This only will be called for Larger annotation documents
     * 
     * @param aParameters the parameters.
     * @return export file type once in a while!!!
     */
    public StoreSvgResponse ajaxCall(MultiValueMap<String, String> aParameters)
    {
        StoreSvgResponse storeSvgResponse = new StoreSvgResponse();
        ArrayList<Stored> storedList = new ArrayList<Stored>();
        Stored stored = new Stored();

        stored.setName("TCF");
        stored.setSuffix("TCF");
        storedList.add(stored);

        storeSvgResponse.setStored(storedList);

        return storeSvgResponse;
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
     * a protocol to retunr the expected file type for annotation document exporting . Currently, it
     * returns only tcf file type where in the future svg and pdf types are to be supported
     * 
     * @return the response.
     */
    public StoreSvgResponse storeSVG()
    {
        StoreSvgResponse storeSvgResponse = new StoreSvgResponse();
        ArrayList<Stored> storedList = new ArrayList<Stored>();
        Stored stored = new Stored();

        stored.setName("TCF");
        stored.setSuffix("TCF");
        storedList.add(stored);

        storeSvgResponse.setStored(storedList);

        return storeSvgResponse;
    }

    public ImportDocumentResponse importDocument(String aCollection, String aDocId, String aText,
            String aTitle, HttpServletRequest aRequest)
    {
        ImportDocumentResponse importDocument = new ImportDocumentResponse();
        importDocument.setDocument(aDocId);
        return importDocument;
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
        info.setEntityTypes(BratAjaxConfiguration.buildEntityTypes(aAnnotationLayers,
                annotationService));
        return info;
    }

    public GetDocumentTimestampResponse getDocumentTimestamp(String aCollection, String aDocument)
    {
        return new GetDocumentTimestampResponse();
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
}