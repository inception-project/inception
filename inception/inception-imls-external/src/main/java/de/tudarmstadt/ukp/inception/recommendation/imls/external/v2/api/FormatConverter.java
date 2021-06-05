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
package de.tudarmstadt.ukp.inception.recommendation.imls.external.v2.api;

import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class FormatConverter
{

    public Document fromCas(CAS aCas, int aVersion, String aLayerName, String aFeatureName)
    {
        String text = aCas.getDocumentText();
        Map<String, List<Annotation>> annotations = new HashMap<>();

        // Add sentences
        Type sentenceType = CasUtil.getAnnotationType(aCas, Sentence.class);
        List<Annotation> sentences = new ArrayList<>();
        for (AnnotationFS sentence : CasUtil.select(aCas, sentenceType)) {
            sentences.add(new Annotation(sentence.getBegin(), sentence.getEnd()));
        }
        annotations.put("t.sentence", sentences);

        // Add tokens
        Type tokenType = CasUtil.getAnnotationType(aCas, Token.class);
        List<Annotation> tokens = new ArrayList<>();
        for (AnnotationFS token : CasUtil.select(aCas, tokenType)) {
            tokens.add(new Annotation(token.getBegin(), token.getEnd()));
        }
        annotations.put("t.token", tokens);

        // Add targets
        Type targetType = CasUtil.getAnnotationType(aCas, aLayerName);
        Feature feature = targetType.getFeatureByBaseName(aFeatureName);
        List<Annotation> targets = new ArrayList<>();
        for (AnnotationFS target : CasUtil.select(aCas, targetType)) {
            String featureValue = target.getFeatureValueAsString(feature);
            Map<String, Object> featureValues = singletonMap("f.value", featureValue);
            targets.add(new Annotation(target.getBegin(), target.getEnd(), featureValues));
        }
        annotations.put("t.span_annotation", targets);

        return new Document(text, annotations, aVersion);
    }

    public CAS toCas(Document aDocument)
    {
        return null;
    }
}
