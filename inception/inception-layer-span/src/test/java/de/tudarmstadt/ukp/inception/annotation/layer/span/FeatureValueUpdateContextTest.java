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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import static de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser.parse;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder;

/**
 * Verifies that {@code TypeAdapter#updateFeatureValues} defers constraint-driven hidden feature
 * clearing until the batch is closed. Regresses
 * <a href="https://github.com/inception-project/inception/issues/5786">#5786</a>: when feature B's
 * visibility is gated by a constraint on feature A and the per-feature setter clears B immediately,
 * B is wiped before A is set if the loop happens to set B first.
 */
@ExtendWith(MockitoExtension.class)
public class FeatureValueUpdateContextTest
{
    private static final String SPAN_TYPE = "my.Span";
    private static final String GATING_FEATURE = "superLayer";
    private static final String GATED_FEATURE = "entitiesLayer";
    private static final String GATING_VALUE_THAT_SHOWS = "Entité";
    private static final String GATED_VALUE = "TEMPS";

    private @Mock ConstraintsService constraintsService;

    private SourceDocument document;
    private CAS cas;
    private AnnotationFS span;
    private AnnotationFeature gatingFeature;
    private AnnotationFeature gatedFeature;
    private SpanAdapterImpl sut;

    @BeforeEach
    void setup() throws Exception
    {
        var project = new Project();
        project.setId(1l);

        document = new SourceDocument();
        document.setId(1l);
        document.setProject(project);

        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var spanType = tsd.addType(SPAN_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        spanType.addFeature(GATING_FEATURE, "", CAS.TYPE_NAME_STRING);
        spanType.addFeature(GATED_FEATURE, "", CAS.TYPE_NAME_STRING);
        cas = CasFactory.createCas(tsd);
        cas.setDocumentText("text");

        var spanLayer = AnnotationLayer.builder() //
                .withId(1l) //
                .withProject(project) //
                .withName(SPAN_TYPE) //
                .withType(SpanLayerSupport.TYPE) //
                .build();

        gatingFeature = AnnotationFeature.builder() //
                .withId(1l) //
                .withProject(project) //
                .withLayer(spanLayer) //
                .withName(GATING_FEATURE) //
                .withType(CAS.TYPE_NAME_STRING) //
                .build();

        // The gated feature is hidden whenever no constraint rule's condition matches for it. The
        // only rule mentioning it requires the gating feature to equal GATING_VALUE_THAT_SHOWS.
        gatedFeature = AnnotationFeature.builder() //
                .withId(2l) //
                .withProject(project) //
                .withLayer(spanLayer) //
                .withName(GATED_FEATURE) //
                .withType(CAS.TYPE_NAME_STRING) //
                .withHideUnconstraintFeature(true) //
                .build();

        var constraints = parse("""
                import %s as Span;
                Span {
                  %s = "%s" -> %s = "%s";
                }
                """.formatted(SPAN_TYPE, GATING_FEATURE, GATING_VALUE_THAT_SHOWS, GATED_FEATURE,
                GATED_VALUE));
        when(constraintsService.getMergedConstraints(project)).thenReturn(constraints);

        var featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport()));
        featureSupportRegistry.init();

        var layerSupportRegistry = new LayerSupportRegistryImpl(emptyList());

        sut = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null, spanLayer,
                () -> asList(gatingFeature, gatedFeature), emptyList(), constraintsService);

        span = AnnotationBuilder.buildAnnotation(cas, SPAN_TYPE) //
                .on("text") //
                .buildAndAddToIndexes();
    }

    @Test
    void thatPerFeatureSetterClearsGatedFeatureWhenSetBeforeGatingFeature() throws Exception
    {
        // Reproduces the original bug: setting the gated feature first triggers the
        // clear-hidden-features pass while the gating feature is still null, so the gated value
        // is wiped out before the gating feature is even set.
        sut.setFeatureValue(document, "user", cas, getAddr(span), gatedFeature, GATED_VALUE);
        sut.setFeatureValue(document, "user", cas, getAddr(span), gatingFeature,
                GATING_VALUE_THAT_SHOWS);

        assertThat(getFeature(span, GATED_FEATURE, String.class)) //
                .as("gated feature is wiped because constraint clearing ran while gating feature was still null") //
                .isNull();
        assertThat(getFeature(span, GATING_FEATURE, String.class)) //
                .isEqualTo(GATING_VALUE_THAT_SHOWS);
    }

    @Test
    void thatBatchUpdateDefersClearingUntilCloseSoFeatureOrderDoesNotMatter() throws Exception
    {
        // Same value-setting order as the buggy test above, but inside an updateFeatureValues
        // batch. The clear-hidden-features pass should run only once, at close, by which time
        // both features are set and the constraint condition is satisfied.
        try (var ctx = sut.updateFeatureValues(document, "user", cas, getAddr(span))) {
            ctx.setFeatureValue(gatedFeature, GATED_VALUE);
            ctx.setFeatureValue(gatingFeature, GATING_VALUE_THAT_SHOWS);
        }

        assertThat(getFeature(span, GATED_FEATURE, String.class)) //
                .as("gated feature should survive because clearing is deferred to close time") //
                .isEqualTo(GATED_VALUE);
        assertThat(getFeature(span, GATING_FEATURE, String.class)) //
                .isEqualTo(GATING_VALUE_THAT_SHOWS);
    }

    @Test
    void thatBatchUpdateStillClearsHiddenFeatureAtCloseWhenConstraintIsNotSatisfied()
        throws Exception
    {
        // When the gating feature is set to a value that does NOT satisfy the constraint, the
        // gated feature must still be cleared at close time - deferred clearing must not become
        // skipped clearing.
        try (var ctx = sut.updateFeatureValues(document, "user", cas, getAddr(span))) {
            ctx.setFeatureValue(gatingFeature, "something-else");
            ctx.setFeatureValue(gatedFeature, GATED_VALUE);
        }

        assertThat(getFeature(span, GATED_FEATURE, String.class)) //
                .as("gated feature must be cleared on close because the constraint is not satisfied") //
                .isNull();
        assertThat(getFeature(span, GATING_FEATURE, String.class)) //
                .isEqualTo("something-else");
    }
}
