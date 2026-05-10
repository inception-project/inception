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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format;

import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationToCasConverter.LABEL_FEATURE;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LAYER_NAME;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.StreamSupport;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public class PubAnnotationAnnotationsReaderTest
{
    @Test
    public void thatReaderPopulatesCasFromFixture() throws Exception
    {
        var customTsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var basicSpan = customTsd.addType(BASIC_SPAN_LAYER_NAME, null, TYPE_NAME_ANNOTATION);
        basicSpan.addFeature(LABEL_FEATURE, null, TYPE_NAME_STRING);
        var merged = mergeTypeSystems(asList(createTypeSystemDescription(), customTsd));

        var reader = createReader( //
                PubAnnotationAnnotationsReader.class, merged, //
                PubAnnotationAnnotationsReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/samplePubAnnotation.json");

        assertThat(reader.hasNext()).isTrue();

        var jcas = JCasFactory.createJCas(merged);
        reader.getNext(jcas.getCas());

        assertThat(jcas.getDocumentText()).startsWith("Cancer-selective targeting");

        // Inflammaging track: "Sentence" → DKPro Sentence via suffix-match.
        var sentenceType = jcas.getTypeSystem().getType(Sentence.class.getName());
        var sentences = list(jcas.getCas().<AnnotationFS> getAnnotationIndex(sentenceType));
        assertThat(sentences).hasSize(2);

        // PubmedHPO track: "HP_0006775" → custom.Span fallback (T2 is discontinuous, skipped).
        var basicSpanType = jcas.getTypeSystem().getType(BASIC_SPAN_LAYER_NAME);
        var basicSpans = list(jcas.getCas().<AnnotationFS> getAnnotationIndex(basicSpanType));
        assertThat(basicSpans).hasSize(1);
        assertThat(
                basicSpans.get(0).getStringValue(basicSpanType.getFeatureByBaseName(LABEL_FEATURE)))
                        .isEqualTo("HP_0006775");
    }

    private static <T> java.util.List<T> list(Iterable<T> aIt)
    {
        return StreamSupport.stream(aIt.spliterator(), false).toList();
    }
}
