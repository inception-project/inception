/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.weblicht;

import static java.lang.String.format;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.chains.WeblichtChainService;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.converter.DKPro2Tcf;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.converter.Tcf2DKPro;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.model.WeblichtChain;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.traits.WeblichtRecommenderTraits;
import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WeblichtRecommender
    extends RecommendationEngine
{
    private static final Logger LOG = LoggerFactory.getLogger(WeblichtRecommender.class);

    private static final MediaType TCF = MediaType.parse("text/tcf+xml");
    private static final MediaType XML = MediaType.parse("application/xml");
    private static final MediaType TEXT = MediaType.parse("text/plain");
    private static final long CONNECT_TIMEOUT = 30;
    private static final long WRITE_TIMEOUT = 30;
    private static final long READ_TIMEOUT = 30;
    
    private final WeblichtChainService chainService;
    private final WeblichtRecommenderTraits traits;
    private final OkHttpClient client;

    public WeblichtRecommender(Recommender aRecommender, WeblichtRecommenderTraits aTraits,
            WeblichtChainService aChainService)
    {
        super(aRecommender);

        traits = aTraits;
        chainService = aChainService;
        client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT,TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }
    
    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
    {
        // Training not supported
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        if (!chainService.existsChain(getRecommender())) {
            return;
        }
        
        try {
            String documentText = aCas.getDocumentText();
            String documentLanguage = aCas.getDocumentLanguage();
            
            HttpUrl url = HttpUrl.parse(traits.getUrl()).newBuilder().build();
            
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("apikey", traits.getApiKey())
                    .addFormDataPart("chains", "chains.xml",
                            RequestBody.create(XML, getChainFile()));
            
            TextCorpusStored textCorpus;
            switch (traits.getChainInputFormat()) {
            case PLAIN_TEXT: {
                // Create TextCorpus object, specifying the language set in the recommender,
                // although it doesn't really matter here because we only send plain text to 
                // WebLicht, so it won't ever see the language.
                textCorpus = new TextCorpusStored(traits.getChainInputLanguage());
                // Create text annotation layer and add the string of the text into the layer
                textCorpus.createTextLayer().addText(aCas.getDocumentText());
                builder.addFormDataPart("content", "content.txt", 
                        RequestBody.create(TEXT, aCas.getDocumentText()));
                // Copy the tokens because we will clear the CAS later but then we want to restore
                // the original tokens...
                new DKPro2Tcf().writeTokens(aCas.getJCas(), textCorpus);
                break;
            }
            case TCF:
                // Create TextCorpus object, specifying the language set in the recommender
                textCorpus = new TextCorpusStored(traits.getChainInputLanguage());
                // Create text annotation layer and add the string of the text into the layer
                textCorpus.createTextLayer().addText(aCas.getDocumentText());
                // Convert tokens and sentences and leave the rest to the chain
                DKPro2Tcf dkpro2tcf = new DKPro2Tcf();
                Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> tokensBeginPositionMap;
                tokensBeginPositionMap = dkpro2tcf.writeTokens(aCas.getJCas(), textCorpus);
                dkpro2tcf.writeSentence(aCas.getJCas(), textCorpus, tokensBeginPositionMap);
                // write the annotated data object into the output stream
                byte[] bodyData;
                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    WLData wldata = new WLData(textCorpus);
                    WLDObjector.write(wldata, os);
                    bodyData = os.toByteArray();
                }
                builder.addFormDataPart("content", "content.tcf",
                        RequestBody.create(TCF, bodyData));
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown format [" + traits.getChainInputFormat() + "]");
            }
                    
            RequestBody body = builder.build();
                    
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "*/*")
                    .post(body)
                    .build();

            Response response = sendRequest(request);

            // If the response indicates that the request was not successful,
            // then it does not make sense to go on and try to decode the XMI
            if (!response.isSuccessful()) {
                int code = response.code();
                String responseBody = getResponseBody(response);
                String msg = format("Request was not successful: [%d] - [%s]", code, responseBody);
                throw new RecommendationException(msg);
            }
            
            aCas.reset();
            aCas.setDocumentText(documentText);
            aCas.setDocumentLanguage(documentLanguage);
            
            WLData wldata = deserializePredictionResponse(response);
            new Tcf2DKPro().convert(wldata.getTextCorpus(), aCas.getJCas());
            
            // Drop the tokens we got from the remote service since their boundaries might not
            // match ours. Then let's just re-add the tokens that we originally sent. We need the
            // tokens later when extracting the predicted annotations
            select(aCas, getType(aCas, Token.class)).forEach(aCas::removeFsFromIndexes);
            new Tcf2DKPro().convertTokens(aCas.getJCas(), textCorpus);
            
            // Mark predicted results
            Feature isPredictionFeature = getIsPredictionFeature(aCas);
            for (AnnotationFS predictedAnnotation : select(aCas, getPredictedType(aCas))) {
                predictedAnnotation.setBooleanValue(isPredictionFeature, true);
            }
        } catch (Exception e) {
            throw new RecommendationException("Cannot predict", e);
        }
    }
    
    private File getChainFile() throws IOException
    {
        Optional<WeblichtChain> optChain = chainService.getChain(recommender);
        if (optChain.isPresent()) {
            return chainService.getChainFile(optChain.get());
        }
        else {
            throw new IOException("No chain file available");
        }
    }
    
    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        EvaluationResult result = new EvaluationResult();
        result.setErrorMsg("Evaluation not supported (yet)");
        return result;
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return true;
    }

    @Override
    public RecommendationEngineCapability getTrainingCapability()
    {
        return RecommendationEngineCapability.TRAINING_NOT_SUPPORTED;
    }
    
    private Response sendRequest(Request aRequest) throws RecommendationException
    {
        try {
            return client.newCall(aRequest).execute();
        }
        catch (IOException e) {
            throw new RecommendationException("Error while sending request!", e);
        }
    }
    
    private String getResponseBody(Response response) throws RecommendationException
    {
        try {
            if (response.body() != null) {
                return response.body().string();
            } else {
                return "";
            }
        } catch (IOException e) {
            throw new RecommendationException("Error while reading response body!", e);
        }
    }
    
    private WLData deserializePredictionResponse(Response response) throws RecommendationException
    {
        try (InputStream is = response.body().byteStream()) {
            return WLDObjector.read(is);
        }
        catch (IOException e) {
            throw new RecommendationException("Error while deserializing prediction response!", e);
        }
    }
}
