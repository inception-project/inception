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
package de.tudarmstadt.ukp.inception.diam.editing;

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.FSUtil.setFeature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapterImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapterImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Tests how the service deals with a CAS whose type system lacks a feature that is defined on the
 * layer, e.g. because the feature was added after the CAS was last upgraded. Reading must tolerate
 * this, writing must refuse it without changing anything.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AnnotationEditingServiceImplStaleTypeSystemTest
{
    private static final String SPAN_TYPE = "webanno.custom.Span";
    private static final String RELATION_TYPE = "webanno.custom.Relation";

    private @Mock AnnotationSchemaService schemaService;
    private @Mock ConstraintsService constraintsService;
    private @Mock AnnotatorState state;

    private AnnotationEditingServiceImpl sut;

    private CAS cas;
    private SourceDocument document;
    private AnnotationLayer spanLayer;
    private SpanAdapterImpl spanAdapter;
    private RelationAdapterImpl relationAdapter;

    /** Feature that exists in the CAS type system. */
    private AnnotationFeature presentFeature;

    /** Feature that is defined on the layer but missing from the CAS type system. */
    private AnnotationFeature missingFeature;

    /** Relation feature that is defined on the layer but missing from the CAS type system. */
    private AnnotationFeature missingRelationFeature;

    @BeforeEach
    void setup() throws Exception
    {
        var project = new Project("test");
        project.setId(1l);
        document = new SourceDocument("doc.txt", project, "text");
        document.setId(1l);

        spanLayer = AnnotationLayer.builder() //
                .withId(1l) //
                .withProject(project) //
                .withName(SPAN_TYPE) //
                .withType(SpanLayerSupport.TYPE) //
                .build();

        var relationLayer = AnnotationLayer.builder() //
                .withId(2l) //
                .withProject(project) //
                .withName(RELATION_TYPE) //
                .withType(RelationLayerSupport.TYPE) //
                .build();

        presentFeature = stringFeature(1l, spanLayer, "present");
        missingFeature = stringFeature(2l, spanLayer, "missing");
        missingRelationFeature = stringFeature(3l, relationLayer, "label");

        var tsd = new TypeSystemDescription_impl();
        tsd.addType(SPAN_TYPE, "", TYPE_NAME_ANNOTATION) //
                .addFeature(presentFeature.getName(), "", TYPE_NAME_STRING);
        var relationType = tsd.addType(RELATION_TYPE, "", TYPE_NAME_ANNOTATION);
        relationType.addFeature(FEAT_REL_SOURCE, "", SPAN_TYPE);
        relationType.addFeature(FEAT_REL_TARGET, "", SPAN_TYPE);
        cas = CasFactory.createCas(tsd);
        cas.setDocumentText("one two three");

        var featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport()));
        featureSupportRegistry.init();
        var layerSupportRegistry = new LayerSupportRegistryImpl(emptyList());

        spanAdapter = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                spanLayer, () -> asList(presentFeature, missingFeature), emptyList(),
                constraintsService);
        relationAdapter = new RelationAdapterImpl(layerSupportRegistry, featureSupportRegistry,
                null, relationLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE,
                () -> asList(missingRelationFeature), emptyList(), constraintsService);

        sut = new AnnotationEditingServiceImpl(schemaService);

        when(schemaService.getAdapter(spanLayer)).thenReturn(spanAdapter);
        when(schemaService.listEnabledFeatures(spanLayer))
                .thenReturn(asList(presentFeature, missingFeature));
    }

    private static AnnotationFeature stringFeature(long aId, AnnotationLayer aLayer, String aName)
    {
        return AnnotationFeature.builder() //
                .withId(aId) //
                .withProject(aLayer.getProject()) //
                .withLayer(aLayer) //
                .withName(aName) //
                .withUiName(aName) //
                .withType(TYPE_NAME_STRING) //
                .build();
    }

    private AnnotationFS createSpan(int aBegin, int aEnd, String aValue)
    {
        var span = cas.createAnnotation(cas.getTypeSystem().getType(SPAN_TYPE), aBegin, aEnd);
        setFeature(span, presentFeature.getName(), aValue);
        cas.addFsToIndexes(span);
        return span;
    }

    @Test
    void thatLoadingToleratesAFeatureMissingFromTheTypeSystem() throws Exception
    {
        var span = createSpan(0, 3, "old");

        var messages = new ArrayList<LogMessage>();
        var featureStates = sut.loadFeatureStates(cas, state, spanLayer, span, null, messages);

        assertThat(featureStates) //
                .extracting(fs -> fs.feature, fs -> fs.value) //
                .containsExactly( //
                        tuple(presentFeature, "old"), //
                        tuple(missingFeature, null));
        assertThat(messages).isEmpty();
    }

    @Test
    void thatCommittingAFeatureMissingFromTheTypeSystemFailsWithoutWriting() throws Exception
    {
        var span = createSpan(0, 3, "old");

        var featureStates = asList( //
                new FeatureState(VID.of(span), presentFeature, "new"),
                new FeatureState(VID.of(span), missingFeature, "new"));

        assertThatExceptionOfType(StaleTypeSystemException.class) //
                .isThrownBy(() -> sut.commitFeatureStates(document, "user", cas, getAddr(span),
                        spanAdapter, featureStates))
                .satisfies(e -> assertThat(e.getFeature()).isEqualTo(missingFeature));

        assertThat(getFeature(span, presentFeature.getName(), String.class)).isEqualTo("old");
    }

    @Test
    void thatCommittingFeaturesPresentInTheTypeSystemWritesThem() throws Exception
    {
        var span = createSpan(0, 3, "old");

        var messages = sut.commitFeatureStates(document, "user", cas, getAddr(span), spanAdapter,
                asList(new FeatureState(VID.of(span), presentFeature, "new")));

        assertThat(messages).isEmpty();
        assertThat(getFeature(span, presentFeature.getName(), String.class)).isEqualTo("new");
    }

    @Test
    void thatReversingARelationWithAFeatureMissingFromTheTypeSystemLeavesItUntouched()
        throws Exception
    {
        var source = createSpan(0, 3, null);
        var target = createSpan(4, 7, null);
        var relation = cas.createAnnotation(cas.getTypeSystem().getType(RELATION_TYPE),
                target.getBegin(), target.getEnd());
        setFeature(relation, FEAT_REL_SOURCE, source);
        setFeature(relation, FEAT_REL_TARGET, target);
        cas.addFsToIndexes(relation);

        var featureStates = asList(
                new FeatureState(VID.of(relation), missingRelationFeature, "value"));

        assertThatExceptionOfType(StaleTypeSystemException.class) //
                .isThrownBy(() -> sut.reverseRelation(document, "user", cas, getAddr(relation),
                        relationAdapter, featureStates, new ArrayList<>()));

        assertThat(cas.getAnnotationIndex(cas.getTypeSystem().getType(RELATION_TYPE)))
                .containsExactly(relation);
        assertThat(getFeature(relation, FEAT_REL_SOURCE, AnnotationFS.class)).isSameAs(source);
        assertThat(getFeature(relation, FEAT_REL_TARGET, AnnotationFS.class)).isSameAs(target);
    }
}
