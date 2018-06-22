/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.TypeSystemUtil;
import org.apache.wicket.ajax.json.JSONException;
import org.apache.wicket.ajax.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.TokenObject;

public class ExternalClassifier
    extends Classifier<Object>
{

    private final Logger log = LoggerFactory.getLogger(getClass());
    private CustomAnnotationObjectLoader loader;
    private ExternalClassifierTraits traits;
    private long recommenderId;

    public ExternalClassifier(ClassifierConfiguration<Object> aConfiguration,
                              CustomAnnotationObjectLoader aLoader,
                              ExternalClassifierTraits aTraits, long aRecommenderId)
    {
        super(aConfiguration);
        loader = aLoader;
        traits = aTraits;
        recommenderId = aRecommenderId;
    }

    @Override
    public void setModel(Object m)
    {
        
        // Probably nothing to do here since the model is on the side of the webservice and in the
        // first iteration, we do not train it - it is pretrained.
    }

    @Override
    public List predictSentences(List inputData)
    {
        throw new UnsupportedOperationException("Currently not implemented.");
    }

    @Override
    public void reconfigure()
    {
        // Nothing to do here
    }

    @Override
    public <T extends TokenObject> List<AnnotationObject> predict(JCas aJCas, AnnotationLayer layer)
    {
        //serialize Typesystem
        ByteArrayOutputStream typeOS = new ByteArrayOutputStream();
        try {
            TypeSystemUtil.typeSystem2TypeSystemDescription(aJCas.getTypeSystem()).toXML(typeOS);
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            log.error("Error while serializing type system!", e);
        }

        //Serialize the JCas to XMI and sent it to the Python webservice. 
        ByteArrayOutputStream casOS = new ByteArrayOutputStream();
        try {
            XmiCasSerializer.serialize(aJCas.getCas(), null, casOS, true, null);
            casOS.close();
        }
        catch (SAXException | IOException e) {
            log.error("Error while serializing CAS!", e);
        }
        
        //Contruct Http Request
        String remoteUrl = traits.getRemoteUrl();

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(remoteUrl);
        httpPost.addHeader("content-type", "application/json");
        httpPost.addHeader("charset", "utf-8");
        
        //construct the request's content
        //i.e. JSON with Base64 encoded bytestreams of Typesystem-XML and CAS-XMI
        try {
            String jsonString = new JSONObject()
                    .put("CAS", new String(Base64.getEncoder().encode(casOS.toByteArray()), "utf-8"))
                    .put("Typesystem", new String(Base64.getEncoder().encode(typeOS.toByteArray()), "utf-8"))
                    .put("Layer", new String(layer.getName()))
                    .toString();
            httpPost.setEntity(new StringEntity(jsonString, "utf-8"));
        }
        catch (JSONException | UnsupportedEncodingException e) {
            log.error("Error while creating request!", e);
        }
        
        //Send Query and wait for the results
        try (CloseableHttpResponse response = httpclient.execute(httpPost);) {
            System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            
            //extract the results 
            XmiCasDeserializer.deserialize(entity.getContent(), aJCas.getCas());

            // ensure request is fully consumed
            EntityUtils.consume(entity);
        }
        catch (UnsupportedOperationException | SAXException | IOException e) {
            log.error("Error while sending request!", e);
        }

        List<List<AnnotationObject>> annotatedSentences = loader.loadAnnotationObjects(aJCas,
            recommenderId);
        List<List<List<AnnotationObject>>> wrappedSents = new LinkedList<>();
        for (List<AnnotationObject> sentence : annotatedSentences) {
            List<List<AnnotationObject>> sentenceList = new LinkedList<>();
            for (AnnotationObject annotation : sentence) {
                List<AnnotationObject> annotationList = new LinkedList<>();
                annotationList.add(annotation);
                sentenceList.add(annotationList);
            }
            wrappedSents.add(sentenceList);
        }
        
        List<AnnotationObject> result = mergeAdjacentTokensWithSameLabel(wrappedSents, layer);

        return result;
    }
}
