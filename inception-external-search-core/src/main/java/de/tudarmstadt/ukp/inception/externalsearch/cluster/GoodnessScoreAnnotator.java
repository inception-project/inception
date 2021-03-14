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
package de.tudarmstadt.ukp.inception.externalsearch.cluster;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class GoodnessScoreAnnotator
        extends JCasAnnotator_ImplBase {
    public static String TYPE_NAME_UNIT = "Unit";
    public static String FEATURE_NAME_SCORE = "score";
    public static final String PARAM_QUERY_WORD = "queryWord";
    @ConfigurationParameter(name = PARAM_QUERY_WORD, mandatory = true)
    private String queryWord;
    
    @Override
    public void process(JCas aJCas) {
        Type tUnit = aJCas.getTypeSystem().getType(TYPE_NAME_UNIT);
        Feature fScore = tUnit.getFeatureByBaseName(FEATURE_NAME_SCORE);
        for (AnnotationFS unit : CasUtil.select(aJCas.getCas(), tUnit)) {
            List<String> tokens = JCasUtil.selectCovered(Token.class, unit).stream()
                    .map(AnnotationFS::getCoveredText).collect(toList());
            unit.setDoubleValue(fScore,
                    getFrequencyScore(queryWord, tokens) + getLengthScore(tokens));
        }
    }
    
    // should penalize sentences more than 25 and less than 10 words
    private double getLengthScore(List<String> aTokens) {
        int sentenceLength = aTokens.size();
        if (sentenceLength >= 10 && sentenceLength <= 25) {
            return 1.0;
        } else {
            return 0.0;
        }
    }
    
    // should penalize sentences with multiple occurrences of the query
    private double getFrequencyScore(String aQueryWord, List<String> aTokens) {
        int queryFrequency = Collections.frequency(aTokens, aQueryWord);
        if (queryFrequency == 0) {
            return 0.0;
        } else {
            return 1.0 / (Collections.frequency(aTokens, aQueryWord));
        }
    }
}
