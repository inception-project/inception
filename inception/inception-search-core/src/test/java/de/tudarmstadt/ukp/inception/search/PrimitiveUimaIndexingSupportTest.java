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
package de.tudarmstadt.ukp.inception.search;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.feature.bool.BooleanFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;

class PrimitiveUimaIndexingSupportTest
{
    private PrimitiveUimaIndexingSupport sut;

    private AnnotationLayer layer;

    @BeforeEach
    void setup()
    {
        layer = AnnotationLayer.builder().forJCasClass(NamedEntity.class).build();

        var featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport(), new BooleanFeatureSupport()));
        featureSupportRegistry.init();

        sut = new PrimitiveUimaIndexingSupport(featureSupportRegistry);

    }

    @Test
    void thatUnknownStringFeatureIsSkippedDuringIndexing() throws Exception
    {
        var feature = AnnotationFeature.builder().withUiName("unknown").withName("unknown")
                .withLayer(layer).withType(CAS.TYPE_NAME_STRING).build();

        var cas = JCasFactory.createJCas();
        var ne = new NamedEntity(cas);
        ne.addToIndexes();

        assertThat(sut.indexFeatureValue(layer.getUiName(), ne, "", feature).size()).isZero();
    }

    @Test
    void thatUnknownBooleanFeatureIsIndexedAsFalse() throws Exception
    {
        var feature = AnnotationFeature.builder().withUiName("unknown").withName("unknown")
                .withLayer(layer).withType(CAS.TYPE_NAME_BOOLEAN).build();

        var cas = JCasFactory.createJCas();
        var ne = new NamedEntity(cas);
        ne.addToIndexes();

        var values = sut.indexFeatureValue(layer.getUiName(), ne, "", feature);
        assertThat(values.get(layer.getUiName() + "." + feature.getUiName()))
                .containsExactly("false");
    }
}
