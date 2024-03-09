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
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XCasSchemaAnalyzer.isSpanLayer;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.I_TYPE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.R_SOURCE;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.R_TARGET;
import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.getCollectionMetadataField;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.FEATURE_FULL_NAME_BEGIN;
import static org.apache.uima.cas.CAS.FEATURE_FULL_NAME_END;
import static org.apache.uima.cas.CAS.FEATURE_FULL_NAME_SOFA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.dkpro.core.api.xml.type.XmlElement;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.MetaDataStringField;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Div;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.io.bioc.BioCComponent;

public class CasToBioC
{
    private static final Set<String> EXCLUDED_TYPES = Set.of(DocumentMetaData._TypeName,
            Sentence._TypeName, Token._TypeName, MetaDataStringField._TypeName, Div._TypeName);

    private static final Set<String> EXCLUDED_FEATURES = Set.of(FEATURE_FULL_NAME_BEGIN,
            FEATURE_FULL_NAME_END, FEATURE_FULL_NAME_SOFA);

    private Map<AnnotationFS, String> annotationToId;

    public void convert(JCas aJCas, BioCCollection aCollection)
    {
        annotationToId = new HashMap<>();

        var bioCDocument = new BioCDocument();
        bioCDocument.setId(DocumentMetaData.get(aJCas).getDocumentId());

        // Overwrite with BioC metadata if available
        getCollectionMetadataField(aJCas.getCas(), BioCComponent.E_ID)
                .ifPresent($ -> bioCDocument.setId($.getValue()));

        var passagesIndex = buildPassagesIndex(aJCas);

        for (var passageEntry : passagesIndex.entrySet()) {
            var bioCPassage = passageEntry.getKey();
            var div = passageEntry.getValue();
            if (div.getDivType() != null) {
                bioCPassage.addInfon(I_TYPE, div.getDivType());
            }

            var sentences = aJCas.select(Sentence.class).coveredBy(div).asList();
            var annotations = aJCas.select(Annotation.class).coveredBy(div);
            if (sentences.isEmpty()) {
                bioCPassage.setText(div.getCoveredText());
                processAnnotations(bioCPassage, bioCPassage.getOffset(), annotations);
            }
            else {
                var bioCSentences = processSentences(div.getBegin(), sentences);
                bioCPassage.setSentences(bioCSentences);
                processAnnotations(bioCPassage, bioCPassage.getOffset(),
                        annotations.filter(a -> aJCas.select(Sentence.class).covering(a).isEmpty())
                                .collect(Collectors.toList()));
            }
        }

        bioCDocument.setPassages(new ArrayList<>(passagesIndex.keySet()));
        aCollection.addDocument(bioCDocument);
    }

    private LinkedHashMap<BioCPassage, Div> buildPassagesIndex(JCas aJCas)
    {
        var passagesIndex = new LinkedHashMap<BioCPassage, Div>();
        var divs = aJCas.select(Div.class).asList();
        if (divs.isEmpty()) {
            var bioCPassage = new BioCPassage();
            bioCPassage.setOffset(0);
            passagesIndex.put(bioCPassage, new Div(aJCas, 0, aJCas.getDocumentText().length()));
        }
        else {
            for (var div : divs) {
                var bioCPassage = new BioCPassage();
                bioCPassage.setOffset(0);
                passagesIndex.put(bioCPassage, div);
            }
        }
        return passagesIndex;
    }

    private List<BioCSentence> processSentences(int aPassageOffset, List<Sentence> sentences)
    {
        var bioCSentences = new ArrayList<BioCSentence>();
        for (var sentence : sentences) {
            var bioCSentence = new BioCSentence();
            bioCSentence.setOffset(sentence.getBegin() - aPassageOffset);
            bioCSentence.setText(sentence.getCoveredText());

            processAnnotations(bioCSentence, sentence.getBegin(),
                    sentence.getCAS().select(Annotation.class).coveredBy(sentence));

            bioCSentences.add(bioCSentence);
        }
        return bioCSentences;
    }

    private void processAnnotations(BioCAnnotationContainer aContainer, int aContainerOffset,
            Iterable<Annotation> aAnnotations)
    {
        for (var annotation : aAnnotations) {
            if (isExcludedType(annotation)) {
                continue;
            }

            if (isSpanLayer(annotation.getType())) {
                var span = processSpanAnnotation(aContainerOffset, annotation);
                aContainer.addAnnotation(span);
            }

            if (isRelationLayer(annotation.getType())) {
                var relation = processRelationAnnotation(annotation);
                aContainer.addRelation(relation);
            }
        }
    }

    private BioCRelation processRelationAnnotation(Annotation aAnnotation)
    {
        var relation = new BioCRelation();
        relation.setId(idOf(aAnnotation));
        relation.addInfon(I_TYPE, aAnnotation.getType().getName());
        relation.addNode(R_SOURCE,
                idOf(FSUtil.getFeature(aAnnotation, FEAT_REL_SOURCE, Annotation.class)));
        relation.addNode(R_TARGET,
                idOf(FSUtil.getFeature(aAnnotation, FEAT_REL_TARGET, Annotation.class)));
        serializeFeatures(aAnnotation, relation);
        return relation;
    }

    private String idOf(Annotation aAnnotation)
    {
        return annotationToId.computeIfAbsent(aAnnotation,
                $ -> Integer.toString(annotationToId.size() + 1));
    }

    private BioCAnnotation processSpanAnnotation(int aContainerOffset, Annotation aAnnotation)
    {
        var annotation = new BioCAnnotation();
        annotation.setId(idOf(aAnnotation));
        annotation.addInfon(I_TYPE, aAnnotation.getType().getName());
        annotation.setLocations(asList(new BioCLocation(aAnnotation.getBegin() - aContainerOffset,
                aAnnotation.getEnd() - aAnnotation.getBegin())));
        annotation.setText(aAnnotation.getCoveredText());
        serializeFeatures(aAnnotation, annotation);
        return annotation;
    }

    private void serializeFeatures(Annotation aAnnotation, BioCObject aBioCAnnotation)
    {
        for (var feature : aAnnotation.getType().getFeatures()) {
            if (EXCLUDED_FEATURES.contains(feature.getName())) {
                continue;
            }

            if (feature.getRange().isPrimitive()) {
                var value = aAnnotation.getFeatureValueAsString(feature);
                if (value != null) {
                    aBioCAnnotation.addInfon(feature.getShortName(), value);
                }
            }

            if (CAS.TYPE_NAME_STRING_ARRAY.equals(feature.getRange().getName())) {
                var values = FSUtil.getFeature(aAnnotation, feature, String[].class);
                if (values != null) {
                    for (var value : values) {
                        if (value != null) {
                            aBioCAnnotation.addInfon(feature.getShortName(), value);
                        }
                    }
                }
            }
        }
    }

    private boolean isExcludedType(AnnotationFS aAnnotation)
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
}
