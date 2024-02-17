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
package de.tudarmstadt.ukp.inception.io.bioc.xml;

import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XCasSchemaAnalyzer.isRelationLayer;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XCasSchemaAnalyzer.isSpanLayer;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_ID;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_KEY;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_LENGTH;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_OFFSET;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_REFID;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_ROLE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_ANNOTATION;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_COLLECTION;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_DATE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_DOCUMENT;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_INFON;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_KEY;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_LOCATION;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_NODE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_PASSAGE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_RELATION;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_SENTENCE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_SOURCE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_TEXT;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.I_TYPE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.R_SOURCE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.R_TARGET;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.getCollectionMetadataField;
import static de.tudarmstadt.ukp.inception.io.bioc.xml.BioCXmlUtils.getChildTextElement;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.cas.CAS.FEATURE_FULL_NAME_BEGIN;
import static org.apache.uima.cas.CAS.FEATURE_FULL_NAME_END;
import static org.apache.uima.cas.CAS.FEATURE_FULL_NAME_SOFA;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlTextNode;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.MetaDataStringField;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.io.bioc.model.CasToBioC;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.Cas2SaxEvents;

/**
 * @deprecated Experimental code that was deprecated in favor of {@link CasToBioC}
 */
@Deprecated
public class Cas2BioCSaxEvents
    extends Cas2SaxEvents
{
    private static final Set<String> EXCLUDED_TYPES = Set.of(DocumentMetaData._TypeName,
            Sentence._TypeName, Token._TypeName, MetaDataStringField._TypeName);
    private static final Set<String> EXCLUDED_FEATURES = Set.of(FEATURE_FULL_NAME_BEGIN,
            FEATURE_FULL_NAME_END, FEATURE_FULL_NAME_SOFA);
    private static final Set<String> DATA_ELEMENTS = Set.of(E_COLLECTION, E_DOCUMENT, E_PASSAGE,
            E_SENTENCE, E_ANNOTATION, E_RELATION);

    private Map<AnnotationFS, String> annotationToId;

    public Cas2BioCSaxEvents(ContentHandler aHandler)
    {
        super(aHandler);
    }

    @Override
    public void process(JCas aJCas) throws SAXException
    {
        annotationToId = new HashMap<>();

        super.process(aJCas);
    }

    private String idOf(Annotation aAnnotation)
    {
        return annotationToId.computeIfAbsent(aAnnotation,
                $ -> Integer.toString(annotationToId.size() + 1));
    }

    @Override
    public void processChildren(XmlElement aElement) throws SAXException
    {
        if (E_COLLECTION.equals(aElement.getQName())) {
            processCollectionElement(aElement);
        }

        super.processChildren(aElement);

        if (E_SENTENCE.equals(aElement.getQName())) {
            processSentenceElement(aElement);
        }

        if (E_PASSAGE.equals(aElement.getQName())) {
            processPassageElement(aElement);
        }
    }

    @Override
    public void process(XmlTextNode aChild) throws SAXException
    {
        if (DATA_ELEMENTS.contains(aChild.getParent().getQName())) {
            return;
        }

        super.process(aChild);
    }

    private void processSentenceElement(XmlElement aSentenceElement) throws SAXException
    {
        var sentenceTextElement = getChildTextElement(aSentenceElement);

        if (sentenceTextElement.isEmpty()) {
            // If the sentence has no text element, we cannot determine the offset...
            return;
        }

        var annotations = aSentenceElement.getCAS().select(Annotation.class)
                .coveredBy(aSentenceElement);
        for (var annotation : annotations) {
            serializeAnnotation(sentenceTextElement.get().getBegin(), annotation);
        }
    }

    private void processPassageElement(XmlElement aPassageElement) throws SAXException
    {
        CAS cas = aPassageElement.getCAS();
        var sentencesInPassage = cas.select(Sentence.class) //
                .filter(s -> s.overlapping(aPassageElement)) //
                .collect(toList());

        var passageTextElement = getChildTextElement(aPassageElement);

        if (passageTextElement.isEmpty()) {
            // If the passage has no text element, we cannot determine the offset...
            return;
        }

        for (var annotation : cas.select(Annotation.class).coveredBy(aPassageElement)) {
            if (isSentenceLevelAnnotation(sentencesInPassage, annotation)) {
                // Already serialized as a sentence-level annotation
                continue;
            }
            serializeAnnotation(passageTextElement.get().getBegin(), annotation);
        }
    }

    private void serializeAnnotation(int aContainerOffset, Annotation aAnnotation)
        throws SAXException
    {
        if (isExcludedType(aAnnotation)) {
            return;
        }

        if (isSpanLayer(aAnnotation.getType())) {
            serializeSpanAnnotation(aContainerOffset, aAnnotation);
        }

        if (isRelationLayer(aAnnotation.getType())) {
            serializeRelationAnnotation(aContainerOffset, aAnnotation);
        }
    }

    private void serializeRelationAnnotation(int aContainerOffset, Annotation aAnnotation)
        throws SAXException
    {
        handler.startElement(E_RELATION, Map.of(A_ID, idOf(aAnnotation)));
        infon(I_TYPE, aAnnotation.getType().getName());
        featureInfons(aAnnotation);
        node(R_SOURCE, FSUtil.getFeature(aAnnotation, FEAT_REL_SOURCE, Annotation.class));
        node(R_TARGET, FSUtil.getFeature(aAnnotation, FEAT_REL_TARGET, Annotation.class));
        handler.endElement(E_RELATION);
    }

    private void serializeSpanAnnotation(int aContainerOffset, Annotation aAnnotation)
        throws SAXException
    {
        handler.startElement(E_ANNOTATION, Map.of(A_ID, idOf(aAnnotation)));
        infon(I_TYPE, aAnnotation.getType().getName());
        featureInfons(aAnnotation);
        location(aContainerOffset, aAnnotation);
        text(aAnnotation.getCoveredText());
        handler.endElement(E_ANNOTATION);
    }

    private boolean isExcludedType(Annotation aAnnotation)
    {
        String typeName = aAnnotation.getType().getName();
        if (typeName.startsWith(XmlElement.class.getPackageName())) {
            return true;
        }

        if (EXCLUDED_TYPES.contains(typeName)) {
            return true;
        }

        return false;
    }

    private void featureInfons(Annotation aAnnotation) throws SAXException
    {
        for (var feature : aAnnotation.getType().getFeatures()) {
            if (EXCLUDED_FEATURES.contains(feature.getName())) {
                continue;
            }

            if (feature.getRange().isPrimitive()) {
                var value = aAnnotation.getFeatureValueAsString(feature);
                if (value != null) {
                    infon(feature.getShortName(), value);
                }
            }
        }
    }

    private void node(String aRole, Annotation aReference) throws SAXException
    {
        handler.startElement(E_NODE, Map.of( //
                A_ROLE, aRole, //
                A_REFID, idOf(aReference)));
        handler.endElement(E_NODE);
    }

    private void text(String aCoveredText) throws SAXException
    {
        handler.startElement(E_TEXT);
        handler.characters(aCoveredText);
        handler.endElement(E_TEXT);
    }

    private void location(int aContainerOffset, Annotation aAnnotation) throws SAXException
    {
        handler.startElement(E_LOCATION, Map.of( //
                A_OFFSET, Integer.toString(aAnnotation.getBegin() - aContainerOffset), //
                A_LENGTH, Integer.toString(aAnnotation.getEnd() - aAnnotation.getBegin())));
        handler.endElement(E_LOCATION);
    }

    private void infon(String aKey, String aValue) throws SAXException
    {
        handler.startElement(E_INFON, Map.of(A_KEY, aKey));
        if (aValue != null) {
            handler.characters(aValue);
        }
        handler.endElement(E_INFON);
    }

    private boolean isSentenceLevelAnnotation(List<Sentence> sentences, Annotation annotation)
    {
        return sentences.stream().anyMatch(s -> s.covering(annotation));
    }

    private void processCollectionElement(XmlElement aElement) throws SAXException
    {
        processMetaDataField(aElement.getCAS(), E_SOURCE);
        processMetaDataField(aElement.getCAS(), E_DATE);
        processMetaDataField(aElement.getCAS(), E_KEY);
    }

    private void processMetaDataField(CAS aCas, String aKey) throws SAXException
    {
        var field = getCollectionMetadataField(aCas, aKey);

        if (field.isPresent()) {
            handler.startElement(field.get().getKey());
            handler.characters(field.get().getValue());
            handler.endElement(field.get().getKey());
        }
    }
}
