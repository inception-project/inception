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
package de.tudarmstadt.ukp.inception.io.bioc.model;

import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XCasSchemaAnalyzer.isRelationLayer;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_ID;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.I_TYPE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.R_SOURCE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.R_TARGET;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.addCollectionMetadataField;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.guessBestRelationType;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.guessBestSpanType;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.transferFeatures;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Div;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

public class BioCToCas
{
    private static final Logger LOG = getLogger(lookup().lookupClass());

    public void parseXml(InputStream aReader, JCas aJCas) throws JAXBException, XMLStreamException
    {
        var collection = loadBioCCollection(aReader);

        convert(collection, aJCas);
    }

    public void convert(BioCCollection collection, JCas aJCas)
    {
        var builder = new JCasBuilder(aJCas);
        readDocument(builder, collection.getDocuments().get(0));
        builder.close();
    }

    public BioCCollection loadBioCCollection(InputStream aReader)
        throws XMLStreamException, JAXBException
    {
        var xmlInputFactory = XmlParserUtils.newXmlInputFactory();
        var xmlEventReader = xmlInputFactory.createXMLEventReader(aReader);

        var context = JAXBContext.newInstance(BioCCollection.class);
        var unmarshaller = context.createUnmarshaller();

        var collection = unmarshaller.unmarshal(xmlEventReader, BioCCollection.class).getValue();
        return collection;
    }

    public void readDocument(JCasBuilder aBuilder, BioCDocument aDocument)
    {
        addCollectionMetadataField(aBuilder.getJCas(), E_ID, aDocument.getId());

        if (isNotEmpty(aDocument.getInfons())) {
            LOG.warn("Document-level infons not supported");
        }

        if (aDocument.getPassages() != null) {
            for (var passage : aDocument.getPassages()) {
                readPassage(aBuilder, passage);
            }
        }
    }

    void readPassage(JCasBuilder aBuilder, BioCPassage aPassage)
    {
        if (aPassage.getText() != null) {
            readPassageWithoutSentences(aBuilder, aPassage);
        }
        else if (aPassage.getSentences() != null) {
            readPassageWithSentences(aBuilder, aPassage);
        }
        else {
            LOG.warn("Passage contains neither text nor sentence!");
        }

        var id2Span = readAnnotations(aBuilder, aPassage.getOffset(), aPassage.getAnnotations());
        readRelations(aBuilder, aPassage.getRelations(), id2Span);
    }

    private void readPassageWithSentences(JCasBuilder aBuilder, BioCPassage aPassage)
    {
        int passageBegin = aBuilder.getPosition();

        for (var sentence : aPassage.getSentences()) {
            readSentence(aBuilder, sentence);
        }

        var div = aBuilder.add(passageBegin, Div.class);
        div.trim();
        aPassage.infon(I_TYPE).ifPresent(div::setDivType);
    }

    private void readPassageWithoutSentences(JCasBuilder aBuilder, BioCPassage aPassage)
    {
        if (isNotEmpty(aPassage.getInfons())) {
            LOG.warn("Passage-level infons not supported");
        }

        // Add empty line after passage - if there is space.
        if (aPassage.getOffset() - aBuilder.getPosition() >= 2) {
            aBuilder.add("\n\n");
        }

        if (aBuilder.getPosition() < aPassage.getOffset()) {
            aBuilder.add(repeat(" ", aPassage.getOffset() - aBuilder.getPosition()));
        }

        var div = aBuilder.add(aPassage.getText(), Div.class);
        div.trim();
        aPassage.infon(I_TYPE).ifPresent(div::setDivType);
    }

    void readSentence(JCasBuilder aBuilder, BioCSentence aSentence)
    {
        if (aSentence.getText() == null) {
            LOG.warn("Sentence contains no text!");
        }

        // Add line-break after sentences - if there is space.
        if (aSentence.getOffset() - aBuilder.getPosition() >= 1) {
            aBuilder.add("\n");
        }

        if (aBuilder.getPosition() < aSentence.getOffset()) {
            aBuilder.add(repeat(" ", aSentence.getOffset() - aBuilder.getPosition()));
        }

        var sentence = aBuilder.add(aSentence.getText(), Sentence.class);
        sentence.trim();

        if (isNotEmpty(aSentence.getInfons())) {
            LOG.warn("Sentence-level infons not supported");
        }

        var id2Span = readAnnotations(aBuilder, sentence.getBegin(), aSentence.getAnnotations());
        readRelations(aBuilder, aSentence.getRelations(), id2Span);
    }

    private Map<String, AnnotationFS> readAnnotations(JCasBuilder aBuilder, int aContainerBegin,
            List<BioCAnnotation> aBioCAnnotations)
    {
        if (aBioCAnnotations == null) {
            return emptyMap();
        }

        var id2Span = new LinkedHashMap<String, AnnotationFS>();
        var cas = aBuilder.getJCas().getCas();
        var typeSystem = aBuilder.getJCas().getTypeSystem();
        for (var bioCAnnotation : aBioCAnnotations) {
            var infons = bioCAnnotation.infonMap();
            Type uimaType = guessBestSpanType(typeSystem, infons);
            if (uimaType == null) {
                LOG.debug("Unable to find suitable UIMA type for span annotation");
                continue;
            }

            var firstLocation = bioCAnnotation.getLocations().get(0);
            var begin = aContainerBegin + firstLocation.getOffset();
            var end = begin + firstLocation.getLength();
            var annotation = cas.createAnnotation(uimaType, begin, end);
            transferFeatures(annotation, infons);
            cas.addFsToIndexes(annotation);

            if (bioCAnnotation.getId() != null) {
                id2Span.put(bioCAnnotation.getId(), annotation);
            }
        }
        return id2Span;
    }

    private void readRelations(JCasBuilder aBuilder, List<BioCRelation> aRelations,
            Map<String, AnnotationFS> aId2Span)
    {
        if (aRelations == null) {
            return;
        }

        var cas = aBuilder.getJCas().getCas();

        for (var bioCRelation : aRelations) {
            var infons = bioCRelation.infonMap();
            var nodes = bioCRelation.nodeMap();

            Type uimaType = guessBestRelationType(cas.getTypeSystem(), infons);
            if (uimaType == null || !isRelationLayer(uimaType)) {
                LOG.debug("Unable to find suitable UIMA type for relation annotation");
                continue;
            }

            var sourceId = nodes.get(R_SOURCE);
            var targetId = nodes.get(R_TARGET);
            if (sourceId == null || targetId == null) {
                LOG.debug("Relation must have a source ID and a target ID");
                continue;
            }

            var sourceSpan = aId2Span.get(sourceId);
            var targetSpan = aId2Span.get(targetId);
            if (sourceSpan == null || targetSpan == null) {
                LOG.debug("Relation must have a source and a target");
                continue;
            }

            var annotation = cas.createAnnotation(uimaType, targetSpan.getBegin(),
                    targetSpan.getEnd());
            transferFeatures(annotation, infons);
            FSUtil.setFeature(annotation, FEAT_REL_SOURCE, sourceSpan);
            FSUtil.setFeature(annotation, FEAT_REL_TARGET, targetSpan);
            cas.addFsToIndexes(annotation);
        }
    }
}
