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
package de.tudarmstadt.ukp.inception.recommendation.imls.weblicht;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.http.entity.ContentType.MULTIPART_FORM_DATA;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.chains.WeblichtChainService;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.converter.DKPro2Tcf;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.converter.Tcf2DKPro;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.traits.WeblichtRecommenderTraits;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;

public class WeblichtRecommender
    extends RecommendationEngine
{
    private static final ContentType TCF = ContentType.create("text/tcf+xml");
    private static final ContentType XML = ContentType.create("application/xml");
    private static final ContentType TEXT = ContentType.create("text/plain");
    private static final Duration CONNECT_TIMEOUT = Duration.of(30, SECONDS);

    private final WeblichtChainService chainService;
    private final WeblichtRecommenderTraits traits;
    private final HttpClient client;

    public WeblichtRecommender(Recommender aRecommender, WeblichtRecommenderTraits aTraits,
            WeblichtChainService aChainService)
    {
        super(aRecommender);

        traits = aTraits;
        chainService = aChainService;

        HttpClientBuilder builder = HttpClientBuilder.create();

        builder.setDefaultRequestConfig(RequestConfig.custom() //
                .setConnectTimeout((int) CONNECT_TIMEOUT.toMillis()) //
                .setConnectionRequestTimeout((int) CONNECT_TIMEOUT.toMillis()) //
                .setSocketTimeout((int) CONNECT_TIMEOUT.toMillis()).build());

        client = builder.build();
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
    {
        // Training not supported
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        // Begin and end are not used here because we do not know what kind of WebLicht pipeline
        // the user calls and whether it actually makes sense to limit it in scope.

        if (!chainService.existsChain(getRecommender())) {
            return new Range(aCas);
        }

        try {
            var documentText = aCas.getDocumentText();
            var documentLanguage = aCas.getDocumentLanguage();

            // Build http request and assign multipart upload data
            var builder = MultipartEntityBuilder.create() //
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE) //
                    .addTextBody("apikey", traits.getApiKey(), MULTIPART_FORM_DATA) //
                    .addBinaryBody("chains", getChainFile(), XML, "chains.xml");

            TextCorpusStored textCorpus;
            switch (traits.getChainInputFormat()) {
            case PLAIN_TEXT: {
                // Create TextCorpus object, specifying the language set in the recommender,
                // although it doesn't really matter here because we only send plain text to
                // WebLicht, so it won't ever see the language.
                textCorpus = new TextCorpusStored(traits.getChainInputLanguage());
                // Create text annotation layer and add the string of the text into the layer
                textCorpus.createTextLayer().addText(aCas.getDocumentText());
                builder.addBinaryBody("content", aCas.getDocumentText().getBytes(UTF_8), TEXT,
                        "content.txt");
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
                var dkpro2tcf = new DKPro2Tcf();
                var tokensBeginPositionMap = dkpro2tcf.writeTokens(aCas.getJCas(), textCorpus);
                dkpro2tcf.writeSentence(aCas.getJCas(), textCorpus, tokensBeginPositionMap);
                // write the annotated data object into the output stream
                byte[] bodyData;
                try (var os = new ByteArrayOutputStream()) {
                    var wldata = new WLData(textCorpus);
                    WLDObjector.write(wldata, os);
                    bodyData = os.toByteArray();
                }
                builder.addBinaryBody("content", bodyData, TCF, "content.tcf");
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown format [" + traits.getChainInputFormat() + "]");
            }

            var request = RequestBuilder.post(traits.getUrl())//
                    .addHeader("Accept", "*/*") //
                    .setEntity(builder.build()) //
                    .build();

            var response = sendRequest(request);

            // If the response indicates that the request was not successful,
            // then it does not make sense to go on and try to decode the XMI
            if (response.getStatusLine().getStatusCode() >= 400) {
                int code = response.getStatusLine().getStatusCode();
                String responseBody = getResponseBody(response);
                String msg = format("Request was not successful: [%d] - [%s]", code, responseBody);
                throw new RecommendationException(msg);
            }

            aCas.reset();
            aCas.setDocumentText(documentText);
            aCas.setDocumentLanguage(documentLanguage);

            var wldata = deserializePredictionResponse(response);
            new Tcf2DKPro().convert(wldata.getTextCorpus(), aCas.getJCas());

            // Drop the tokens we got from the remote service since their boundaries might not
            // match ours. Then let's just re-add the tokens that we originally sent. We need the
            // tokens later when extracting the predicted annotations
            select(aCas, getType(aCas, Token.class)).forEach(aCas::removeFsFromIndexes);
            new Tcf2DKPro().convertTokens(aCas.getJCas(), textCorpus);

            // Mark predicted results
            var isPredictionFeature = getIsPredictionFeature(aCas);
            for (var predictedAnnotation : select(aCas, getPredictedType(aCas))) {
                predictedAnnotation.setBooleanValue(isPredictionFeature, true);
            }
        }
        catch (Exception e) {
            throw new RecommendationException("Cannot predict", e);
        }

        return new Range(aCas);
    }

    private File getChainFile() throws IOException
    {
        var optChain = chainService.getChain(recommender);
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
        var result = new EvaluationResult();
        result.setErrorMsg("Evaluation not supported (yet)");
        return result;
    }

    @Override
    public int estimateSampleCount(List<CAS> aCasses)
    {
        return -1;
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return true;
    }

    @Override
    public TrainingCapability getTrainingCapability()
    {
        return TrainingCapability.TRAINING_NOT_SUPPORTED;
    }

    private HttpResponse sendRequest(HttpUriRequest aRequest) throws RecommendationException
    {
        try {
            return client.execute(aRequest);
        }
        catch (IOException e) {
            throw new RecommendationException("Error while sending request!", e);
        }
    }

    private String getResponseBody(HttpResponse response) throws RecommendationException
    {
        try {
            if (response.getEntity() != null) {
                try (var is = response.getEntity().getContent()) {
                    var encoding = response.getEntity().getContentEncoding();
                    return IOUtils.toString(is,
                            encoding != null ? Charset.forName(encoding.getValue()) : UTF_8);
                }
            }
            else {
                return "";
            }
        }
        catch (IOException e) {
            throw new RecommendationException("Error while reading response body!", e);
        }
    }

    private WLData deserializePredictionResponse(HttpResponse response)
        throws RecommendationException
    {
        try (var is = response.getEntity().getContent()) {
            return WLDObjector.read(is);
        }
        catch (IOException e) {
            throw new RecommendationException("Error while deserializing prediction response!", e);
        }
    }
}
