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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SENTENCES;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.cohenkappa.CohenKappaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.fleisskappa.FleissKappaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.gamma.GammaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.gwetac1.GwetAC1AgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.gwetac2.GwetAC2AgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalpha.KrippendorffAlphaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalphaunitizing.KrippendorffAlphaUnitizingAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerTypes;

/**
 * Pins down which layers and features each measure accepts. {@code accepts} used to be implemented
 * by every measure itself; it is now derived from {@code getInapplicabilityReason}, and this test
 * ensures the measures still accept and reject exactly what they did before that change.
 */
class AgreementMeasureApplicabilityTest
{
    private static final String SPAN = LayerTypes.SPAN_LAYER_TYPE;
    private static final String RELATION = LayerTypes.RELATION_LAYER_TYPE;
    private static final String DOCUMENT = LayerTypes.DOCUMENT_LAYER_TYPE;
    private static final String CHAIN = LayerTypes.CHAIN_LAYER_TYPE;

    static List<AgreementMeasureSupport<?, ?, ?>> codingMeasures()
    {
        return List.of( //
                new CohenKappaAgreementMeasureSupport(null, null), //
                new FleissKappaAgreementMeasureSupport(null, null), //
                new GwetAC1AgreementMeasureSupport(null, null), //
                new GwetAC2AgreementMeasureSupport(null, null), //
                new KrippendorffAlphaAgreementMeasureSupport(null, null));
    }

    static List<AgreementMeasureSupport<?, ?, ?>> positionMeasures()
    {
        return List.of( //
                new GammaAgreementMeasureSupport(), //
                new KrippendorffAlphaUnitizingAgreementMeasureSupport());
    }

    static Stream<Arguments> codingCases()
    {
        var cases = Stream.of( //
                Arguments.of("span / tokens", layer(SPAN, TOKENS), feature(), true),
                Arguments.of("span / single token", layer(SPAN, SINGLE_TOKEN), feature(), true),
                Arguments.of("span / sentences", layer(SPAN, SENTENCES), feature(), true),
                Arguments.of("span / characters", layer(SPAN, CHARACTERS), feature(), false),
                Arguments.of("relation", layer(RELATION, TOKENS), feature(), true),
                Arguments.of("document metadata", layer(DOCUMENT, TOKENS), feature(), true),
                Arguments.of("chain", layer(CHAIN, TOKENS), feature(), false),
                Arguments.of("no feature", layer(SPAN, TOKENS), null, false),
                Arguments.of("multi-value string", layer(SPAN, TOKENS), multiValueFeature(), false),
                Arguments.of("link feature", layer(SPAN, TOKENS), linkFeature(), true));

        return cases.flatMap(c -> codingMeasures().stream().map(
                m -> Arguments.of(m.getId(), c.get()[0], m, c.get()[1], c.get()[2], c.get()[3])));
    }

    static Stream<Arguments> positionCases()
    {
        var cases = Stream.of( //
                Arguments.of("span", layer(SPAN, TOKENS), feature(), true),
                Arguments.of("span without feature", layer(SPAN, TOKENS), null, true),
                Arguments.of("span / characters", layer(SPAN, CHARACTERS), feature(), true),
                Arguments.of("relation", layer(RELATION, TOKENS), feature(), false),
                Arguments.of("document metadata", layer(DOCUMENT, TOKENS), feature(), false),
                Arguments.of("chain", layer(CHAIN, TOKENS), feature(), false),
                Arguments.of("link feature", layer(SPAN, TOKENS), linkFeature(), false));

        return cases.flatMap(c -> positionMeasures().stream().map(
                m -> Arguments.of(m.getId(), c.get()[0], m, c.get()[1], c.get()[2], c.get()[3])));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource({ "codingCases", "positionCases" })
    void thatMeasureAcceptsExactlyTheSupportedLayers(String aMeasure, String aCase,
            AgreementMeasureSupport<?, ?, ?> aSupport, AnnotationLayer aLayer,
            AnnotationFeature aFeature, boolean aExpected)
    {
        var reason = aSupport.getInapplicabilityReason(aLayer, aFeature);

        assertThat(aSupport.accepts(aLayer, aFeature)).isEqualTo(aExpected);
        assertThat(reason.isEmpty()).isEqualTo(aExpected);

        // A rejected measure is shown disabled in the selection dialog, together with the reason -
        // an empty reason would leave the user guessing.
        reason.ifPresent(r -> assertThat(r).isNotBlank());
    }

    private static AnnotationLayer layer(String aType, AnchoringMode aAnchoringMode)
    {
        var layer = new AnnotationLayer();
        layer.setName("custom.Layer");
        layer.setType(aType);
        layer.setAnchoringMode(aAnchoringMode);
        return layer;
    }

    private static AnnotationFeature feature()
    {
        var feature = new AnnotationFeature();
        feature.setName("value");
        return feature;
    }

    private static AnnotationFeature multiValueFeature()
    {
        var feature = feature();
        feature.setMode(ARRAY);
        return feature;
    }

    private static AnnotationFeature linkFeature()
    {
        var feature = feature();
        feature.setMode(ARRAY);
        feature.setLinkMode(WITH_ROLE);
        return feature;
    }
}
