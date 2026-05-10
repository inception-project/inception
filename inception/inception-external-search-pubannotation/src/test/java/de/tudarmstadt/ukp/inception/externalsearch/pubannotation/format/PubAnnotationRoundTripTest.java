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

import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationToCasConverter.LABEL_FEATURE;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public class PubAnnotationRoundTripTest
{
    private static final String CUSTOM_PROTEIN = "custom.Protein";
    private static final String CUSTOM_INTERACT = "custom.InteractWith";
    private static final String CUSTOM_TAGGED = "custom.Tagged";

    @Test
    public void thatExportThenImportRoundTrips() throws Exception
    {
        var sourceCas = createCas();
        sourceCas.setDocumentText("alpha bravo charlie delta echo");
        var ts = sourceCas.getTypeSystem();

        // Specific span: custom.Protein with feature uniprot=Q15306
        var proteinType = ts.getType(CUSTOM_PROTEIN);
        var p1 = sourceCas.createAnnotation(proteinType, 0, 5);
        p1.setStringValue(proteinType.getFeatureByBaseName("uniprot"), "Q15306");
        sourceCas.addFsToIndexes(p1);

        var p2 = sourceCas.createAnnotation(proteinType, 6, 11);
        p2.setBooleanValue(proteinType.getFeatureByBaseName("uncertain"), true);
        sourceCas.addFsToIndexes(p2);

        // Tagged span with multi-valued tags
        var taggedType = ts.getType(CUSTOM_TAGGED);
        var tag = sourceCas.createAnnotation(taggedType, 12, 19);
        var tags = sourceCas.createStringArrayFS(2);
        tags.set(0, "red");
        tags.set(1, "blue");
        tag.setFeatureValue(taggedType.getFeatureByBaseName("tags"), tags);
        sourceCas.addFsToIndexes(tag);

        // Basic span fallback case: custom.Span with label="HP_0006775"
        var basicSpanType = ts.getType(BASIC_SPAN_LAYER_NAME);
        var hp = sourceCas.createAnnotation(basicSpanType, 20, 24);
        hp.setStringValue(basicSpanType.getFeatureByBaseName(LABEL_FEATURE), "HP_0006775");
        sourceCas.addFsToIndexes(hp);

        // Specific relation: custom.InteractWith between p1 and p2
        var interactType = ts.getType(CUSTOM_INTERACT);
        var rel = sourceCas.createAnnotation(interactType, p2.getBegin(), p2.getEnd());
        rel.setFeatureValue(interactType.getFeatureByBaseName(FEAT_REL_SOURCE), p1);
        rel.setFeatureValue(interactType.getFeatureByBaseName(FEAT_REL_TARGET), p2);
        sourceCas.addFsToIndexes(rel);

        // Convert CAS → PubAnnotation document
        var spanTypes = orderedSet(Sentence.class.getName(), CUSTOM_PROTEIN, CUSTOM_TAGGED,
                BASIC_SPAN_LAYER_NAME);
        var relationTypes = orderedSet(CUSTOM_INTERACT, BASIC_RELATION_LAYER_NAME);
        var doc = new CasToPubAnnotationConverter(spanTypes, relationTypes).convert(sourceCas,
                "PMC", "1234");

        assertThat(doc.getSourceDb()).isEqualTo("PMC");
        assertThat(doc.getSourceId()).isEqualTo("1234");
        assertThat(doc.getDenotations()).hasSize(4);
        assertThat(doc.getRelations()).hasSize(1);

        // Re-import into a fresh CAS via the mapper
        var roundTripCas = createCas();
        new PubAnnotationToCasConverter(roundTripCas).apply(doc);

        assertThat(roundTripCas.getDocumentText()).isEqualTo(sourceCas.getDocumentText());

        var roundTripProteins = annotationsOfType(roundTripCas,
                roundTripCas.getTypeSystem().getType(CUSTOM_PROTEIN));
        assertThat(roundTripProteins).hasSize(2);
        assertThat(roundTripProteins.get(0)
                .getStringValue(roundTripProteins.get(0).getType().getFeatureByBaseName("uniprot")))
                        .isEqualTo("Q15306");
        assertThat(roundTripProteins.get(1).getBooleanValue(
                roundTripProteins.get(1).getType().getFeatureByBaseName("uncertain"))).isTrue();

        var roundTripTagged = annotationsOfType(roundTripCas,
                roundTripCas.getTypeSystem().getType(CUSTOM_TAGGED));
        assertThat(roundTripTagged).hasSize(1);
        var roundTripTags = (StringArrayFS) roundTripTagged.get(0)
                .getFeatureValue(roundTripTagged.get(0).getType().getFeatureByBaseName("tags"));
        assertThat(roundTripTags.toArray()).containsExactly("red", "blue");

        var roundTripBasic = annotationsOfType(roundTripCas,
                roundTripCas.getTypeSystem().getType(BASIC_SPAN_LAYER_NAME));
        assertThat(roundTripBasic).hasSize(1);
        assertThat(roundTripBasic.get(0).getStringValue(
                roundTripBasic.get(0).getType().getFeatureByBaseName(LABEL_FEATURE)))
                        .isEqualTo("HP_0006775");

        var roundTripRels = annotationsOfType(roundTripCas,
                roundTripCas.getTypeSystem().getType(CUSTOM_INTERACT));
        assertThat(roundTripRels).hasSize(1);
        assertThat(roundTripRels.get(0).getFeatureValue(
                roundTripRels.get(0).getType().getFeatureByBaseName(FEAT_REL_SOURCE)))
                        .isSameAs(roundTripProteins.get(0));
        assertThat(roundTripRels.get(0).getFeatureValue(
                roundTripRels.get(0).getType().getFeatureByBaseName(FEAT_REL_TARGET)))
                        .isSameAs(roundTripProteins.get(1));
    }

    @Test
    public void thatShortNamesAreEmittedByDefaultWhenUnique() throws Exception
    {
        var cas = createCas();
        cas.setDocumentText("alpha bravo");
        var ts = cas.getTypeSystem();

        // custom.Protein has a unique simple name in this CAS → should emit "Protein"
        var proteinType = ts.getType(CUSTOM_PROTEIN);
        var p = cas.createAnnotation(proteinType, 0, 5);
        cas.addFsToIndexes(p);

        var doc = new CasToPubAnnotationConverter(orderedSet(CUSTOM_PROTEIN), orderedSet())
                .convert(cas, "PMC", "1");
        assertThat(doc.getDenotations().get(0).getObject()).isEqualTo("Protein");
    }

    @Test
    public void thatFqnIsEmittedWhenSimpleNameIsAmbiguous() throws Exception
    {
        // Build a CAS with TWO types both ending in ".Protein" — short name would be ambiguous.
        var customTsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var p1 = customTsd.addType("custom.Protein", null, TYPE_NAME_ANNOTATION);
        p1.addFeature("uniprot", null, TYPE_NAME_STRING);
        var p2 = customTsd.addType("other.Protein", null, TYPE_NAME_ANNOTATION);
        p2.addFeature("uniprot", null, TYPE_NAME_STRING);
        var merged = mergeTypeSystems(asList(createTypeSystemDescription(), customTsd));
        var cas = CasFactory.createCas(merged);
        cas.setDocumentText("alpha bravo");

        var fs = cas.createAnnotation(cas.getTypeSystem().getType("custom.Protein"), 0, 5);
        cas.addFsToIndexes(fs);

        var doc = new CasToPubAnnotationConverter(orderedSet("custom.Protein", "other.Protein"),
                orderedSet()).convert(cas, "PMC", "1");
        // Short name "Protein" is ambiguous → must emit FQN.
        assertThat(doc.getDenotations().get(0).getObject()).isEqualTo("custom.Protein");
    }

    @Test
    public void thatShortNamesCanBeDisabled() throws Exception
    {
        var cas = createCas();
        cas.setDocumentText("alpha bravo");
        var p = cas.createAnnotation(cas.getTypeSystem().getType(CUSTOM_PROTEIN), 0, 5);
        cas.addFsToIndexes(p);

        var doc = new CasToPubAnnotationConverter(orderedSet(CUSTOM_PROTEIN), orderedSet(), false)
                .convert(cas, "PMC", "1");
        assertThat(doc.getDenotations().get(0).getObject()).isEqualTo(CUSTOM_PROTEIN);
    }

    private static Set<String> orderedSet(String... aValues)
    {
        return new LinkedHashSet<>(asList(aValues));
    }

    private static java.util.List<AnnotationFS> annotationsOfType(CAS aCas, Type aType)
    {
        return StreamSupport
                .stream(aCas.<AnnotationFS> getAnnotationIndex(aType).spliterator(), false)
                .toList();
    }

    private static CAS createCas() throws Exception
    {
        var customTsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();

        var protein = customTsd.addType(CUSTOM_PROTEIN, null, TYPE_NAME_ANNOTATION);
        protein.addFeature("uniprot", null, TYPE_NAME_STRING);
        protein.addFeature("uncertain", null, TYPE_NAME_BOOLEAN);

        var interact = customTsd.addType(CUSTOM_INTERACT, null, TYPE_NAME_ANNOTATION);
        interact.addFeature(FEAT_REL_SOURCE, null, TYPE_NAME_ANNOTATION);
        interact.addFeature(FEAT_REL_TARGET, null, TYPE_NAME_ANNOTATION);

        var tagged = customTsd.addType(CUSTOM_TAGGED, null, TYPE_NAME_ANNOTATION);
        tagged.addFeature("tags", null, TYPE_NAME_STRING_ARRAY);

        var basicSpan = customTsd.addType(BASIC_SPAN_LAYER_NAME, null, TYPE_NAME_ANNOTATION);
        basicSpan.addFeature(LABEL_FEATURE, null, TYPE_NAME_STRING);

        var basicRelation = customTsd.addType(BASIC_RELATION_LAYER_NAME, null,
                TYPE_NAME_ANNOTATION);
        basicRelation.addFeature(LABEL_FEATURE, null, TYPE_NAME_STRING);
        basicRelation.addFeature(FEAT_REL_SOURCE, null, TYPE_NAME_ANNOTATION);
        basicRelation.addFeature(FEAT_REL_TARGET, null, TYPE_NAME_ANNOTATION);

        var merged = mergeTypeSystems(asList(createTypeSystemDescription(), customTsd));
        return CasFactory.createCas(merged);
    }
}
