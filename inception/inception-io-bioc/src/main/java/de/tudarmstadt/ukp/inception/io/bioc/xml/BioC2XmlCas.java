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
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_ID;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_KEY;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_LENGTH;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_OFFSET;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_REFID;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.A_ROLE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_ANNOTATION;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_INFON;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_NODE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_RELATION;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.E_SENTENCE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.R_SOURCE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.R_TARGET;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.guessBestRelationType;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.guessBestSpanType;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.transferFeatures;
import static de.tudarmstadt.ukp.inception.io.bioc.xml.BioCXmlUtils.getChildLocationElement;
import static de.tudarmstadt.ukp.inception.io.bioc.xml.BioCXmlUtils.getChildTextElement;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.getAttributeValue;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.getMandatoryAttributeValue;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.xml.type.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XCasSchemaAnalyzer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.io.bioc.model.BioCToCas;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils;

/**
 * @deprecated Experimental code that was deprecated in favor of {@link BioCToCas}
 */
@Deprecated
public class BioC2XmlCas
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void transferAnnotations(JCas aJCas)
    {
        transferSentences(aJCas);
        var id2Span = transferSpanAnnotations(aJCas);
        transferRelationAnnotations(aJCas, id2Span);
    }

    private void transferRelationAnnotations(JCas aJCas, Map<String, AnnotationFS> aId2Span)
    {
        var relationElements = aJCas.select(XmlElement.class) //
                .filter(e -> E_RELATION.equals(e.getQName())) //
                .collect(toList());

        var cas = aJCas.getCas();
        for (var relationElement : relationElements) {
            var infons = extractInfons(relationElement);
            var nodes = extractNodes(relationElement);

            var uimaType = guessBestRelationType(aJCas.getTypeSystem(), infons);
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

        relationElements.forEach(XmlNodeUtils::removeWithDescendantsFromTree);
    }

    private Map<String, AnnotationFS> transferSpanAnnotations(JCas aJCas)
    {
        var annotationElements = aJCas.select(XmlElement.class) //
                .filter(e -> E_ANNOTATION.equals(e.getQName())) //
                .collect(toList());

        var id2Span = new LinkedHashMap<String, AnnotationFS>();

        var cas = aJCas.getCas();
        for (var annotationElement : annotationElements) {
            var containerElement = annotationElement.getParent();
            var containerTextElement = getChildTextElement(containerElement);
            var locationElement = getChildLocationElement(annotationElement);
            var id = getAttributeValue(annotationElement, A_ID);
            var offset = parseInt(getMandatoryAttributeValue(locationElement.get(), A_OFFSET));
            var length = parseInt(getMandatoryAttributeValue(locationElement.get(), A_LENGTH));

            var infons = extractInfons(annotationElement);
            Type uimaType = guessBestSpanType(aJCas.getTypeSystem(), infons);
            if (uimaType == null || !Tsv3XCasSchemaAnalyzer.isSpanLayer(uimaType)) {
                LOG.debug("Unable to find suitable UIMA type for span annotation");
                continue;
            }

            var begin = containerTextElement.get().getBegin() + offset;
            var end = begin + length;
            var annotation = cas.createAnnotation(uimaType, begin, end);
            transferFeatures(annotation, infons);
            cas.addFsToIndexes(annotation);

            id.ifPresent($ -> id2Span.put($, annotation));
        }

        annotationElements.forEach(XmlNodeUtils::removeWithDescendantsFromTree);

        return id2Span;
    }

    private Map<String, List<String>> extractInfons(XmlElement aElement)
    {
        var children = aElement.getChildren();

        if (children == null) {
            return emptyMap();
        }

        var infonChildren = children.select(XmlElement.class) //
                .filter(e -> E_INFON.equals(e.getQName())) //
                .toList();

        var infons = new LinkedHashMap<String, List<String>>();
        for (var infonChild : infonChildren) {
            var key = getMandatoryAttributeValue(infonChild, A_KEY);
            var value = XmlNodeUtils.textContent(infonChild);
            var list = infons.computeIfAbsent(key, $ -> new ArrayList<>());
            list.add(value);
        }

        return infons;
    }

    private Map<String, String> extractNodes(XmlElement aElement)
    {
        var children = aElement.getChildren();

        if (children == null) {
            return emptyMap();
        }

        var infonChildren = children.select(XmlElement.class) //
                .filter(e -> E_NODE.equals(e.getQName())) //
                .collect(toList());

        var nodes = new LinkedHashMap<String, String>();
        for (var infonChild : infonChildren) {
            var key = getMandatoryAttributeValue(infonChild, A_ROLE);
            var value = getMandatoryAttributeValue(infonChild, A_REFID);
            nodes.put(key, value);
        }

        return nodes;
    }

    private void transferSentences(JCas aJCas)
    {
        var sentenceElements = aJCas.select(XmlElement.class) //
                .filter(e -> E_SENTENCE.equals(e.getQName()))//
                .collect(toList());

        for (var sentenceElement : sentenceElements) {
            var sentence = new Sentence(aJCas, sentenceElement.getBegin(),
                    sentenceElement.getEnd());
            sentence.trim();
            sentence.addToIndexes();

            // We do not remove the sentence elements from the XML tree. That way, we do not have to
            // re-generated them on export. It means we cannot change the boundaries of sentences,
            // but if we did that we would be running out-of-sync with the offsets referenced in
            // the BioC file anyway. That is a potential drawback of the XML-based approach to
            // handling BioC files.
            // XmlNodeUtils.removeFromTree(sentenceElement);
        }
    }

}
