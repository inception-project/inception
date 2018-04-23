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
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.classifier.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.TokenObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.custom.CustomAnnotationObjectLoader;

public class ExternalClassifier
    extends Classifier<Object>
{

    public ExternalClassifier(ClassifierConfiguration<Object> conf)
    {
        super(conf);
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
        // TODO Auto-generated method stub

    }

    @Override
    public <T extends TokenObject> List<AnnotationObject> predict(JCas aJCas, AnnotationLayer layer)
    {
        
        //serialize Typesystem
        ByteArrayOutputStream typeOS = new ByteArrayOutputStream();
        try {
            TypeSystemUtil.typeSystem2TypeSystemDescription(aJCas.getTypeSystem()).toXML(typeOS);
        }
        catch (CASRuntimeException | SAXException | IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        

        //Serialize the JCas to XMI and sent it to the Python webservice. 
        ByteArrayOutputStream casOS = new ByteArrayOutputStream();
        try {
            XmiCasSerializer.serialize(aJCas.getCas(), null, casOS, true, null);
            casOS.close();
        }
        catch (SAXException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        //Contruct Http Request
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://localhost:12889/tag"); //TODO: pass URL in parameter
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
        catch (JSONException | UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        //Send Query and wait for the results
        try (CloseableHttpResponse response = httpclient.execute(httpPost);) {
            System.out.println(response.getStatusLine());
            HttpEntity entity2 = response.getEntity();
            
            //extract the results 
            XmiCasDeserializer.deserialize(entity2.getContent(), aJCas.getCas());

            // ensure request is fully consumed
            EntityUtils.consume(entity2);
        }
        catch (UnsupportedOperationException | SAXException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
        //TODO: Pass FeatureName as Parameter
        
        CustomAnnotationObjectLoader loader = new CustomAnnotationObjectLoader();
        List<List<AnnotationObject>> annotatedSentences = loader.loadAnnotationObjects(aJCas, "ArgF");
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
