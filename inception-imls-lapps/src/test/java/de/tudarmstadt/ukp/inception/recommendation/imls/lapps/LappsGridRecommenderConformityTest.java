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
package de.tudarmstadt.ukp.inception.recommendation.imls.lapps;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.getObjectMapper;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.assertj.core.api.SoftAssertions;
import org.dkpro.core.io.conll.ConllUReader;
import org.dkpro.core.io.xmi.XmiReader;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.type.TypeReference;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridService;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class LappsGridRecommenderConformityTest
{
    @Test
    @Parameters(method = "getNerServices")
    public void testNerConformity(LappsGridService aService) throws Exception
    {
        CAS cas = loadData();

        predict(aService.getUrl(), cas);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(JCasUtil.select(cas.getJCas(), Token.class))
                .as("Prediction should contain Tokens")
                .isNotEmpty();
        softly.assertThat(JCasUtil.select(cas.getJCas(), NamedEntity.class))
                .as("Prediction should contain NER")
                .isNotEmpty();

        softly.assertAll();
    }

    @Test
    @Parameters(method = "getPosServices")
    public void testPosConformity(LappsGridService aService) throws Exception
    {
        CAS cas = loadData();

        predict(aService.getUrl(), cas);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(JCasUtil.select(cas.getJCas(), Token.class))
                .as("Prediction should contain Tokens")
                .isNotEmpty();
        softly.assertThat(JCasUtil.select(cas.getJCas(), POS.class))
                .as("Prediction should contain POS tags")
                .isNotEmpty();

        softly.assertAll();
    }

    private void predict(String aUrl, CAS aCas) throws Exception
    {
        assumeTrue(isReachable(aUrl));
        
        LappsGridRecommenderTraits traits = new LappsGridRecommenderTraits();
        traits.setUrl(aUrl);
        LappsGridRecommender recommender = new LappsGridRecommender(buildRecommender(), traits);
        recommender.predict(null, aCas);
    }

    private CAS loadData() throws IOException, UIMAException {
        Path path = Paths.get("src", "test", "resources", "testdata", "tnf.xmi");
        CAS cas = loadData(path.toFile());

        RecommenderTestHelper.addScoreFeature(cas, NamedEntity.class, "value");

        return cas;
    }

    private static CAS loadData(File aFile) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(XmiReader.class,
                ConllUReader.PARAM_PATTERNS, aFile);

        List<CAS> casList = new ArrayList<>();
        while (reader.hasNext()) {
            JCas cas = JCasFactory.createJCas();
            reader.getNext(cas.getCas());
            casList.add(cas.getCas());
        }

        return casList.get(0);
    }

    private static List<LappsGridService> getNerServices() throws Exception
    {
        Map<String, List<LappsGridService>> services = loadPredefinedServicesData();
        return services.get("ner");
    }

    private static List<LappsGridService> getPosServices() throws Exception
    {
        Map<String, List<LappsGridService>> services = loadPredefinedServicesData();
        return services.get("pos");
    }

    private static Map<String, List<LappsGridService>> loadPredefinedServicesData()
            throws Exception
    {
        try (InputStream is = LappsGridRecommenderTraitsEditor
                .class.getResourceAsStream("services.json")) {
            TypeReference<Map<String, List<LappsGridService>>> typeRef =
                    new TypeReference<Map<String, List<LappsGridService>>>() {};
            return getObjectMapper().readValue(is, typeRef);
        }
    }

    private static Recommender buildRecommender()
    {
        AnnotationLayer layer = new AnnotationLayer();
        layer.setName(POS.class.getName());

        AnnotationFeature feature = new AnnotationFeature();
        feature.setName("value");

        Recommender recommender = new Recommender();
        recommender.setLayer(layer);
        recommender.setFeature(feature);

        return recommender;
    }
    
    public static boolean isReachable(String aUrl)
    {
        try {
            URL url = new URL(aUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(2500);
            con.setReadTimeout(2500);
            con.setRequestProperty("Content-Type", "application/sparql-query");
            int status = con.getResponseCode();
            
            if (status == HTTP_MOVED_TEMP || status == HTTP_MOVED_PERM) {
                String location = con.getHeaderField("Location");
                return isReachable(location);
            }
            
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}

