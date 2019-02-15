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
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.type.TypeReference;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.conll.ConllUReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridService;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class LappsGridRecommenderConformityTest
{
    @Test
    @Ignore
    @Parameters(method = "getNerServices")
    public void testNerConformity(LappsGridService aService) throws Exception
    {
        CAS cas = loadData();

        predict(aService.getUrl(), cas);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(JCasUtil.select(cas.getJCas(), Token.class))
                .as("Prediction should contain Tokens")
                .isNotEmpty();
        softly.assertThat(JCasUtil.select(cas.getJCas(), Sentence.class))
                .as("Prediction should contain Sentences")
                .isNotEmpty();
        softly.assertThat(JCasUtil.select(cas.getJCas(), NamedEntity.class))
                .as("Prediction should contain NER")
                .isNotEmpty();

        softly.assertAll();
    }

    @Test
    @Ignore
    @Parameters(method = "getPosServices")
    public void testPosConformity(LappsGridService aService) throws Exception
    {
        CAS cas = loadData();

        predict(aService.getUrl(), cas);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(JCasUtil.select(cas.getJCas(), Token.class))
                .as("Prediction should contain Tokens")
                .isNotEmpty();
        softly.assertThat(JCasUtil.select(cas.getJCas(), Sentence.class))
                .as("Prediction should contain Sentences")
                .isNotEmpty();
        softly.assertThat(JCasUtil.select(cas.getJCas(), POS.class))
                .as("Prediction should contain POS tags")
                .isNotEmpty();

        softly.assertAll();
    }

    private void predict(String aUrl, CAS aCas) throws Exception
    {
        LappsGridRecommenderTraits traits = new LappsGridRecommenderTraits();
        traits.setUrl(aUrl);
        LappsGridRecommender recommender = new LappsGridRecommender(null, traits);
        recommender.predict(null, aCas);
    }

    private CAS loadData() throws IOException, UIMAException {
        File file = new File(getClass().getResource("/testdata/cas.xmi").getFile());

        return loadData(file);
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
}
