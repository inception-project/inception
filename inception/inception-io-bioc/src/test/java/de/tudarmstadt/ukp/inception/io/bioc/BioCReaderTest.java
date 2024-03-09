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
package de.tudarmstadt.ukp.inception.io.bioc;

import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.iteratePipeline;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.MetaDataStringField;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public class BioCReaderTest
{
    @Test
    void testRead() throws Exception
    {
        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var basicSpanType = tsd.addType(BASIC_SPAN_LAYER_NAME, null, TYPE_NAME_ANNOTATION);
        basicSpanType.addFeature(BASIC_SPAN_LABEL_FEATURE_NAME, null, TYPE_NAME_STRING);

        var basicRelationType = tsd.addType(BASIC_RELATION_LAYER_NAME, null, TYPE_NAME_ANNOTATION);
        basicRelationType.addFeature(FEAT_REL_SOURCE, null, CAS.TYPE_NAME_ANNOTATION);
        basicRelationType.addFeature(FEAT_REL_TARGET, null, CAS.TYPE_NAME_ANNOTATION);
        basicRelationType.addFeature(BASIC_RELATION_LABEL_FEATURE_NAME, null, TYPE_NAME_STRING);

        var fullTsd = mergeTypeSystems(
                asList(TypeSystemDescriptionFactory.createTypeSystemDescription(), tsd));

        var reader = createReader( //
                BioCReader.class, //
                BioCReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/bioc/example-with-sentences.xml");

        assertThat(reader.hasNext()).isTrue();

        var cas = CasFactory.createCas(fullTsd);
        reader.getNext(cas);

        assertThat(cas.getDocumentText()) //
                .contains("Sentence 1.") //
                .contains("Sentence 2.");

        assertThat(cas.select(Sentence.class).asList()) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("Sentence 1.", "Sentence 2.");

        assertThat(cas.select(MetaDataStringField.class).asList())
                .extracting(MetaDataStringField::getKey, MetaDataStringField::getValue)
                .containsExactlyInAnyOrder( //
                        tuple(BioCComponent.E_ID, "some-id-1234567"),
                        tuple(BioCComponent.E_SOURCE, "example-with-sentences"),
                        tuple(BioCComponent.E_KEY, "example-with-sentences.key"),
                        tuple(BioCComponent.E_DATE, "2023-01-01"));

        assertThat(cas.select(cas.getTypeSystem().getType(BASIC_SPAN_LAYER_NAME)).asList())
                .extracting( //
                        a -> getFeature(a, CAS.FEATURE_BASE_NAME_BEGIN, Integer.class),
                        a -> getFeature(a, CAS.FEATURE_BASE_NAME_END, Integer.class),
                        a -> getFeature(a, BASIC_RELATION_LABEL_FEATURE_NAME, String.class))
                .containsExactlyInAnyOrder( //
                        tuple(10, 18, "span-annotation-value"), //
                        tuple(19, 20, "span-annotation-value"));

        assertThat(cas.select(cas.getTypeSystem().getType(BASIC_RELATION_LAYER_NAME)).asList())
                .hasSize(1);
    }

    @Test
    void testReadMultipleFromOneFile() throws Exception
    {
        var reader = createReaderDescription( //
                BioCReader.class, //
                BioCReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/bioc/example-with-multiple-documents.xml");

        var texts = new ArrayList<String>();
        iteratePipeline(reader).forEach(cas -> texts.add(cas.getDocumentText().trim()));

        assertThat(texts) //
                .containsExactly("Document 1 text.", "Document 2 text.", "Document 3 text.");
    }

    @Test
    void testReadFileWithIncompleteMetadata() throws Exception
    {
        var reader = createReaderDescription( //
                BioCReader.class, //
                BioCReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/bioc/example-with-incomplete-metadata.xml");

        var texts = new ArrayList<String>();
        iteratePipeline(reader).forEach(cas -> texts.add(cas.getDocumentText().trim()));

        assertThat(texts) //
                .containsExactly("Document 1 text.", "Document 2 text.", "Document 3 text.");
    }
}
