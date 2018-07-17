/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.api.util;

import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.TokenObject;

/**
 * A helper class to load AnnotationObjects from a JCas object.
 * 
 *
 *
 */
public class CasUtil
{
    private static Logger LOG = LoggerFactory.getLogger(CasUtil.class);

    private CasUtil()
    {
    }

    /**
     *
     *
     * @param aJcas the JCas of the document from which the annotated sentences should be retrieved
     * @param aLayer the layer for which features should be retrieved
     * @param aFeatureName the name of the feature
     * @return an annotated document
     */
    public static <T extends Annotation> List<List<AnnotationObject>> loadAnnotatedSentences(
        JCas aJcas, long aRecommenderId, AnnotationLayer aLayer, String aFeatureName)
    {
        List<List<AnnotationObject>> result = new ArrayList<>();

        if (aJcas == null) {
            return result;
        }

        DocumentMetaData dmd = DocumentMetaData.get(aJcas);
        String documentURI = "N/A";
        String documentName = "N/A";

        if (dmd == null) {
            LOG.warn("DocumentMetaData is null! No DocumentURI retrievable.");
        }
        else {
            documentURI = dmd.getDocumentUri();
            documentName = dmd.getDocumentTitle();
        }

        int id = 0;

        for (Sentence s : select(aJcas, Sentence.class)) {
            Type annotationType = org.apache.uima.fit.util.CasUtil
                .getType(aJcas.getCas(), aLayer.getName());
            List<AnnotationFS> annotations = org.apache.uima.fit.util.CasUtil
                .selectCovered(annotationType, s);
            Feature feature = annotationType.getFeatureByBaseName(aFeatureName);

            if (annotations.isEmpty()) {
                continue;
            }

            // TODO #176 use the document Id once it it available in the CAS
            List<TokenObject> tokens = loadTokenObjects(s, documentURI, documentName);
            if (tokens.isEmpty()) {
                LOG.error("Could not retrieve tokens from annotated sentence! "
                    + "Continue, but the returned list of annotated sentences is incomplete");
                continue;
            }

            List<AnnotationObject> annotationObjects = getTokenAnnotations(annotations, tokens,
                documentURI, documentName, feature, aRecommenderId);

            List<AnnotationObject> completeSentence = getAnnotationsForCompleteSentence(tokens,
                annotationObjects, aFeatureName, id, aRecommenderId);
            result.add(completeSentence);
            id = id + completeSentence.size();
        }

        return result;
    }

    /**
     * Only for ClassificationTool Unit tests
     * 
     * @param jCas
     * @param annotationType
     * @param feature
     * @param applyFunction
     * @return an annotated document
     */
    @Deprecated
    public static <T extends Annotation> List<List<AnnotationObject>> loadAnnotatedSentences(
            JCas jCas, Class<T> annotationType, String feature, Function<T, String> applyFunction)
    {
        List<List<AnnotationObject>> result = new ArrayList<>();

        if (jCas == null) {
            return result;
        }
        
        DocumentMetaData dmd = DocumentMetaData.get(jCas);
        String documentURI = "N/A";
        String documentName = "N/A";
        
        if (dmd == null) {
            LOG.warn("DocumentMetaData is null! No DocumentURI retrievable.");
        }
        else {
            documentURI = dmd.getDocumentUri();
            documentName = dmd.getDocumentTitle();
        }
        
        int id = 0;
        
        for (Sentence s : select(jCas, Sentence.class)) {
            List<T> annotations = selectCovered(annotationType, s);
            if (annotations.isEmpty()) {
                continue;
            }

            // TODO #176 use the document Id once it it available in the CAS
            List<TokenObject> tokens = loadTokenObjects(s, documentURI, documentName);
            if (tokens.isEmpty()) {
                LOG.error("Could not retrieve tokens from annotated sentence! "
                        + "Continue, but the returned list of annotated sentences is incomplete");
                continue;
            }

            List<AnnotationObject> annotationObjects = getTokenAnnotations(annotations, tokens,
                    applyFunction, documentURI, documentName, feature);

            List<AnnotationObject> completeSentence = getAnnotationsForCompleteSentence(tokens,
                    annotationObjects, feature, id);
            result.add(completeSentence);
            id = id + completeSentence.size();
        }

        return result;
    }

    /**
     * Only for ClassificationTool Unit tests
     */
    @Deprecated
    private static <A extends Annotation> List<AnnotationObject> getAnnotationsForCompleteSentence(
        List<TokenObject> sentence, List<AnnotationObject> annotationObjects, String feature,
        int id)
    {
        return getAnnotationsForCompleteSentence(sentence, annotationObjects, feature, id, 0);
    }

    private static <A extends Annotation> List<AnnotationObject> getAnnotationsForCompleteSentence(
        List<TokenObject> sentence, List<AnnotationObject> annotations, String feature, int id,
        long aRecommenderId)
    {
        List<AnnotationObject> result = new ArrayList<>();
        int indexAnnotations = 0;

        if (sentence == null || annotations == null || sentence.isEmpty()
                || annotations.isEmpty()) {
            return result;
        }

        for (int i = 0; i < sentence.size(); i++) {
            TokenObject tObj = sentence.get(i);

            if (indexAnnotations >= annotations.size()) {
                result.add(new AnnotationObject(tObj, id, feature, aRecommenderId));
                id++;
                continue;
            }

            AnnotationObject aObj = annotations.get(indexAnnotations);

            if (aObj.getOffset().equals(tObj.getOffset())) {
                AnnotationObject annotationObject = new AnnotationObject(aObj);
                annotationObject.setId(id);
                annotationObject.setFeature(feature);
                annotationObject.setRecommenderId(aRecommenderId);
                result.add(annotationObject);
                indexAnnotations++;
            }
            else {
                // Since not all tokens in the sentence are annotated, 
                // we need to add some AnnotationObjects with empty label in respective positions
                result.add(new AnnotationObject(tObj, id, feature, aRecommenderId));
            }
            id++;
        }

        return result;
    }


    private static <A extends Annotation> List<AnnotationObject> getTokenAnnotations(
        List<AnnotationFS> annotations, List<TokenObject> sentence, String documentURI,
        String documentName, Feature feature, long aRecommenderId)
    {
        List<AnnotationObject> result = new ArrayList<>();

        int id = 0;

        for (AnnotationFS a : annotations) {
            String annotationLabel = a.getStringValue(feature);

            List<Token> tokens = selectCovered(Token.class, a);

            if (tokens == null || tokens.isEmpty()) {
                continue;
            }

            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                Offset offset = getTokenOffset(token, sentence);
                TokenObject tObj = new TokenObject(offset, token.getCoveredText(),
                    documentURI, documentName, id);
                AnnotationObject ao = new AnnotationObject(tObj, id, feature.getName(),
                    aRecommenderId);
                ao.setLabel(annotationLabel);
                result.add(ao);
                id++;
            }
        }

        Collections.sort(result, (ao1, ao2) -> ao1.getOffset().compareTo(ao2.getOffset()));

        return result;
    }

    /*
     * Only for ClassificationTool Unit tests
     */
    @Deprecated
    private static <A extends Annotation> List<AnnotationObject> getTokenAnnotations(
            List<A> annotations, List<TokenObject> sentence, Function<A, String> applyFunction,
            String documentURI, String documentName, String feature)
    {
        List<AnnotationObject> result = new ArrayList<>();

        int id = 0;
        
        for (A a : annotations) {
            String annotationLabel = applyFunction.apply(a);

            List<Token> tokens = selectCovered(Token.class, a);

            if (tokens == null || tokens.isEmpty()) {
                continue;
            }

            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                Offset offset = getTokenOffset(token, sentence);
                TokenObject tObj = new TokenObject(offset, token.getCoveredText(), 
                    documentURI, documentName, id);
                AnnotationObject ao = new AnnotationObject(tObj, id, feature, 0);
                ao.setLabel(annotationLabel);
                result.add(ao);
                id++;
            }
        }

        Collections.sort(result, (ao1, ao2) -> ao1.getOffset().compareTo(ao2.getOffset()));

        return result;
    }

    private static Offset getTokenOffset(Token token, List<TokenObject> sentence)
    {
        for (TokenObject tObj : sentence) {
            Offset tOffset = tObj.getOffset();
            if (token.getBegin() == tOffset.getBeginCharacter()
                    && token.getEnd() == tOffset.getEndCharacter()) {
                return tOffset;
            }
        }

        return new Offset(token.getBegin(), token.getEnd(), 0, 0);
    }

    // TODO #176 use the document Id once it it available in the CAS
    public static List<TokenObject> loadTokenObjects(Sentence s, String documentURI, 
        String documentName)
    {
        List<TokenObject> result = new ArrayList<>();

        if (s == null) {
            return result;
        }

        List<Token> tokens = selectCovered(Token.class, s);
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);

            Offset offset = new Offset(t.getBegin(), t.getEnd(), i, i);
            // TODO #176 use the document Id once it it available in the CAS
            result.add(new TokenObject(offset, t.getCoveredText(), documentURI, documentName, i));
        }

        return result;
    }

    public static List<List<TokenObject>> loadTokenObjects(JCas jCas)
    {
        List<List<TokenObject>> result = new ArrayList<>();

        if (jCas == null) {
            return result;
        }

        DocumentMetaData dmd = DocumentMetaData.get(jCas);
        String documentURI = "N/A";
        String documentName = "N/A";
        
        if (dmd == null) {
            LOG.warn("DocumentMetaData is null! No DocumentURI retrievable.");
        }
        else {
            documentURI = dmd.getDocumentUri();
            documentName = dmd.getDocumentTitle();
        }

        for (Sentence s : select(jCas, Sentence.class)) {
            // TODO #176 use the document Id once it it available in the CAS
            result.add(loadTokenObjects(s, documentURI, documentName));
        }

        return result;
    }

    /**
     * Get sentences within the specified window
     */
    public static List<List<TokenObject>> loadTokenObjects(JCas jCas, int begin, int end)
    {
        List<List<TokenObject>> result = new ArrayList<>();

        if (jCas == null) {
            return result;
        }

        DocumentMetaData dmd = DocumentMetaData.get(jCas);
        String documentURI = "N/A";
        String documentName = "N/A";
        
        if (dmd == null) {
            LOG.warn("DocumentMetaData is null! No DocumentURI retrievable.");
        }
        else {
            documentURI = dmd.getDocumentUri();
            documentName = dmd.getDocumentTitle();
        }

        for (Sentence s : select(jCas, Sentence.class)) {
            if (s.getBegin() >= begin && s.getEnd() <= end) {
                // TODO #176 use the document Id once it it available in the CAS
                result.add(loadTokenObjects(s, documentURI, documentName));
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T extends TokenObject> List<List<AnnotationObject>> transformToAnnotationObjects(
            List<List<T>> sentences, String feature, String classifier, long aRecommenderId)
    {
        List<List<AnnotationObject>> result = new ArrayList<>();

        if (sentences == null) {
            return result;
        }

        int id = 0;
        
        for (List<T> sentence : sentences) {
            List<AnnotationObject> annotations = new ArrayList<>();

            for (int i = 0; i < sentence.size(); i++) {
                T t = sentence.get(i);
                annotations.add(new AnnotationObject(t, id, feature, classifier, aRecommenderId));
                id++;
            }

            result.add(annotations);
        }

        return result;
    }

    public static <T extends TokenObject> String[] getCoveredTexts(List<T> tokens)
    {
        if (tokens == null) {
            return new String[0];
        }

        String[] result = new String[tokens.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = tokens.get(i).getCoveredText();
        }

        return result;
    }

    public static <T extends Annotation> List<List<AnnotationObject>> loadCustomAnnotatedSentences(
        JCas jCas, Type annotationType, Feature feature, long aRecommenderId)
    {
        List<List<AnnotationObject>> result = new ArrayList<>();

        if (jCas == null) {
            return result;
        }

        CAS cas = jCas.getCas();
        Type sentenceType = JCasUtil.getType(jCas, Sentence.class);

        DocumentMetaData dmd = DocumentMetaData.get(jCas);
        String documentURI = "N/A";
        String documentName = "N/A";

        if (dmd == null) {
            LOG.warn("DocumentMetaData is null! No DocumentURI retrievable.");
        }
        else {
            documentURI = dmd.getDocumentUri();
            documentName = dmd.getDocumentTitle();
        }

        int id = 0;

        for (AnnotationFS sentence : org.apache.uima.fit.util.CasUtil.select(cas, sentenceType)) {
            List<AnnotationFS> annotations = org.apache.uima.fit.util.CasUtil
                    .selectCovered(annotationType, sentence);
            if (annotations.isEmpty()) {
                continue;
            }

            // TODO #176 use the document Id once it it available in the CAS
            List<TokenObject> tokens = loadTokenObjects(sentence, documentURI, documentName, jCas);
            if (tokens.isEmpty()) {
                LOG.error("Could not retrieve tokens from annotated sentence! "
                    + "Continue, but the returned list of annotated sentences is incomplete");
                continue;
            }

            List<AnnotationObject> annotationObjects = getTokenAnnotationsFromFS(annotations,
                    tokens, documentURI, documentName, feature, aRecommenderId);

            List<AnnotationObject> completeSentence = getAnnotationsForCompleteSentence(tokens,
                annotationObjects, feature.getShortName(), id);
            result.add(completeSentence);
            id = id + completeSentence.size();
        }

        return result;
    }

    private static <A extends Annotation> List<AnnotationObject> getTokenAnnotationsFromFS(
        List<AnnotationFS> annotations, List<TokenObject> sentence,
        String documentURI, String documentName, Feature feature, long aRecommenderId)
    {
        List<AnnotationObject> result = new ArrayList<>();
        int id = 0;

        for (AnnotationFS a : annotations) {
            String annotationLabel = a.getFeatureValueAsString(feature);

            List<Token> tokens = selectCovered(Token.class, a);

            if (tokens == null || tokens.isEmpty()) {
                continue;
            }

            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                Offset offset = getTokenOffset(token, sentence);
                TokenObject tObj = new TokenObject(offset, token.getCoveredText(), documentURI,
                        documentName, id);
                AnnotationObject ao = new AnnotationObject(tObj, id, feature.getName(),
                    aRecommenderId);
                ao.setLabel(annotationLabel);
                result.add(ao);
                id++;
            }
        }

        Collections.sort(result, (ao1, ao2) -> ao1.getOffset().compareTo(ao2.getOffset()));

        return result;
    }

    // TODO #176 use the document Id once it it available in the CAS
    public static List<TokenObject> loadTokenObjects(AnnotationFS sentence, String documentURI,
                                                     String documentName, JCas aJCas)
    {
        List<TokenObject> result = new ArrayList<>();

        if (sentence == null) {
            return result;
        }

        List<AnnotationFS> tokens = org.apache.uima.fit.util.CasUtil
                .selectCovered(JCasUtil.getType(aJCas, Token.class), sentence);
        for (int i = 0; i < tokens.size(); i++) {
            int begin = tokens.get(i).getBegin();
            int end = tokens.get(i).getEnd();
            Token t = new Token(aJCas, begin, end);

            Offset offset = new Offset(t.getBegin(), t.getEnd(), i, i);
            // TODO #176 use the document Id once it it available in the CAS
            result.add(new TokenObject(offset, t.getCoveredText(), documentURI, documentName, i));
        }

        return result;
    }
}
