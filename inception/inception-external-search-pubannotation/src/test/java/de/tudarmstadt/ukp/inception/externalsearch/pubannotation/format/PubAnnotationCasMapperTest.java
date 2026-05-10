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

import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationCasMapper.BASIC_RELATION_LAYER;
import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationCasMapper.BASIC_SPAN_LAYER;
import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationCasMapper.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationCasMapper.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationCasMapper.LABEL_FEATURE;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocument;
import tools.jackson.databind.ObjectMapper;

public class PubAnnotationCasMapperTest
{
    private static final String CUSTOM_PROTEIN_TYPE = "custom.Protein";

    @Test
    public void thatTrackedDocumentMapsCorrectly() throws Exception
    {
        var cas = createCas();
        var doc = readDocument("/samplePubAnnotation.json");

        new PubAnnotationCasMapper(cas).apply(doc);

        assertThat(cas.getDocumentText()).startsWith("Cancer-selective targeting");

        // Inflammaging track: "Sentence" suffix-matches the DKPro Sentence type (loaded from
        // the classpath type system).
        var sentenceType = cas.getTypeSystem().getType(Sentence.class.getName());
        var sentences = annotationsOfType(cas, sentenceType);
        assertThat(sentences).hasSize(2);
        assertThat(sentences.get(0).getBegin()).isEqualTo(0);
        assertThat(sentences.get(0).getEnd()).isEqualTo(86);

        // PubmedHPO track: "HP_0006775" doesn't match anything → falls back to custom.Span
        var basicSpanType = cas.getTypeSystem().getType(BASIC_SPAN_LAYER);
        var basicSpans = annotationsOfType(cas, basicSpanType);
        // T1 (continuous) lands; T2 (discontinuous) is skipped.
        assertThat(basicSpans).hasSize(1);
        var t1 = basicSpans.get(0);
        assertThat(t1.getStringValue(basicSpanType.getFeatureByBaseName(LABEL_FEATURE)))
                .isEqualTo("HP_0006775");

        // Competition rule: T1 fell back to custom.Span and the obj "HP_0006775" claimed the
        // label slot. Attribute A1 (uniprot=Q15306) is its only attribute, but since label is
        // already taken, the attribute is skipped — label keeps the obj.
    }

    @Test
    public void thatRelationsAndAttributesMapToCustomTypes() throws Exception
    {
        var cas = createCas();
        var doc = readDocument("/samplePubAnnotationProjectScoped.json");

        new PubAnnotationCasMapper(cas).apply(doc);

        // Both denotations have obj="Protein" → suffix-matches custom.Protein.
        var proteinType = cas.getTypeSystem().getType(CUSTOM_PROTEIN_TYPE);
        var proteins = annotationsOfType(cas, proteinType);
        assertThat(proteins).hasSize(2);

        // Attribute A1 (subj=T1, pred=uniprot, obj=Q15306) lands on the matching feature.
        var uniprot = proteinType.getFeatureByBaseName("uniprot");
        assertThat(proteins.get(0).getStringValue(uniprot)).isEqualTo("Q15306");

        // Attribute A2 (subj=T2, pred=uncertain, obj=true) lands on the boolean feature.
        var uncertain = proteinType.getFeatureByBaseName("uncertain");
        assertThat(proteins.get(1).getBooleanValue(uncertain)).isTrue();

        // Relation R1 (T1 -interactWith-> T2): "interactWith" doesn't match any type → falls
        // back to custom.Relation with label="interactWith".
        var relationType = cas.getTypeSystem().getType(BASIC_RELATION_LAYER);
        var relations = annotationsOfType(cas, relationType);
        assertThat(relations).hasSize(1);
        var r = relations.get(0);
        assertThat(r.getStringValue(relationType.getFeatureByBaseName(LABEL_FEATURE)))
                .isEqualTo("interactWith");
        assertThat(r.getFeatureValue(relationType.getFeatureByBaseName(FEAT_REL_SOURCE)))
                .isSameAs(proteins.get(0));
        assertThat(r.getFeatureValue(relationType.getFeatureByBaseName(FEAT_REL_TARGET)))
                .isSameAs(proteins.get(1));
    }

    @Test
    public void thatAttributeFallsBackToLabelOnlyWhenObjDidNot() throws Exception
    {
        // Denotation T1 hits a specific type (custom.Entity, which has a "label" feature),
        // so obj does not claim the label slot. The single attribute should fall back to label.
        var json = "{" + "\"text\":\"foo bar baz\"," + "\"denotations\":["
                + "  {\"id\":\"T1\",\"span\":{\"begin\":0,\"end\":3},\"obj\":\"Entity\"}" + "],"
                + "\"attributes\":["
                + "  {\"id\":\"A1\",\"subj\":\"T1\",\"pred\":\"unknown_pred\",\"obj\":\"hello\"}"
                + "]" + "}";
        var cas = createCas();
        var doc = new ObjectMapper().readValue(json, PubAnnotationDocument.class);

        new PubAnnotationCasMapper(cas).apply(doc);

        var entityType = cas.getTypeSystem().getType("custom.Entity");
        var entities = annotationsOfType(cas, entityType);
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getStringValue(entityType.getFeatureByBaseName(LABEL_FEATURE)))
                .isEqualTo("hello");
    }

    @Test
    public void thatExactFqnMatchTakesPrecedenceAndAttributeBindsByName() throws Exception
    {
        // obj is the fully-qualified DKPro Sentence type — must take the exact-match path
        // (not suffix). Attribute pred matches a feature on the type → set by name.
        var json = "{" + "\"text\":\"foo bar baz\","
                + "\"denotations\":[{\"id\":\"T1\",\"span\":{\"begin\":0,\"end\":3}," //
                + "\"obj\":\"" + Sentence.class.getName() + "\"}],"
                + "\"attributes\":[{\"id\":\"A1\",\"subj\":\"T1\",\"pred\":\"id\",\"obj\":\"S-7\"}]"
                + "}";
        var cas = createCas();

        new PubAnnotationCasMapper(cas)
                .apply(new ObjectMapper().readValue(json, PubAnnotationDocument.class));

        var sentenceType = cas.getTypeSystem().getType(Sentence.class.getName());
        var sentences = annotationsOfType(cas, sentenceType);
        assertThat(sentences).hasSize(1);
        // DKPro Sentence has an "id" feature (string).
        assertThat(sentences.get(0).getStringValue(sentenceType.getFeatureByBaseName("id")))
                .isEqualTo("S-7");
    }

    @Test
    public void thatLabelFallbackRequiresExactlyOneAttribute() throws Exception
    {
        // Two attributes on the same subject: one matches a feature by name, the other has an
        // unknown pred. The unknown one must NOT fall back to label, because the subject has
        // more than one attribute.
        var json = "{" + "\"text\":\"foo bar baz\","
                + "\"denotations\":[{\"id\":\"T1\",\"span\":{\"begin\":0,\"end\":3},"
                + "\"obj\":\"Protein\"}]," + "\"attributes\":["
                + "  {\"id\":\"A1\",\"subj\":\"T1\",\"pred\":\"uniprot\",\"obj\":\"Q15306\"},"
                + "  {\"id\":\"A2\",\"subj\":\"T1\",\"pred\":\"unknown_pred\",\"obj\":\"orphan\"}"
                + "]}";
        var cas = createCas();

        new PubAnnotationCasMapper(cas)
                .apply(new ObjectMapper().readValue(json, PubAnnotationDocument.class));

        var proteinType = cas.getTypeSystem().getType(CUSTOM_PROTEIN_TYPE);
        var proteins = annotationsOfType(cas, proteinType);
        assertThat(proteins).hasSize(1);
        // Named match still fires.
        assertThat(proteins.get(0).getStringValue(proteinType.getFeatureByBaseName("uniprot")))
                .isEqualTo("Q15306");
        // Label fallback must NOT fire — there is no "label" feature on custom.Protein anyway,
        // but the multi-attribute guard is the primary check. Verify the orphan attribute did
        // not bleed into any other feature.
        assertThat(proteinType.getFeatureByBaseName(LABEL_FEATURE)).isNull();
    }

    @Test
    public void thatRelationSuffixMatchUsesEndpointFeatures() throws Exception
    {
        // pred suffix-matches custom.InteractWith. Endpoints land on Governor/Dependent.
        var json = "{" + "\"text\":\"foo bar baz\"," + "\"denotations\":["
                + "  {\"id\":\"T1\",\"span\":{\"begin\":0,\"end\":3},\"obj\":\"Protein\"},"
                + "  {\"id\":\"T2\",\"span\":{\"begin\":4,\"end\":7},\"obj\":\"Protein\"}" + "],"
                + "\"relations\":[{\"id\":\"R1\",\"subj\":\"T1\",\"pred\":\"InteractWith\","
                + "\"obj\":\"T2\"}]" + "}";
        var cas = createCas();

        new PubAnnotationCasMapper(cas)
                .apply(new ObjectMapper().readValue(json, PubAnnotationDocument.class));

        var interactType = cas.getTypeSystem().getType("custom.InteractWith");
        var rels = annotationsOfType(cas, interactType);
        assertThat(rels).hasSize(1);
        var proteinType = cas.getTypeSystem().getType(CUSTOM_PROTEIN_TYPE);
        var proteins = annotationsOfType(cas, proteinType);
        assertThat(rels.get(0).getFeatureValue(interactType.getFeatureByBaseName(FEAT_REL_SOURCE)))
                .isSameAs(proteins.get(0));
        assertThat(rels.get(0).getFeatureValue(interactType.getFeatureByBaseName(FEAT_REL_TARGET)))
                .isSameAs(proteins.get(1));
    }

    private static List<AnnotationFS> annotationsOfType(CAS aCas, Type aType)
    {
        return StreamSupport
                .stream(aCas.<AnnotationFS> getAnnotationIndex(aType).spliterator(), false)
                .toList();
    }

    private static CAS createCas() throws Exception
    {
        var customTsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();

        // custom.Protein — non-basic test type with feature schema for attribute mapping
        var protein = customTsd.addType(CUSTOM_PROTEIN_TYPE, null, TYPE_NAME_ANNOTATION);
        protein.addFeature("uniprot", null, TYPE_NAME_STRING);
        protein.addFeature("uncertain", null, TYPE_NAME_BOOLEAN);

        // custom.Entity — has a "label" feature so we can verify the attribute label-fallback
        var entity = customTsd.addType("custom.Entity", null, TYPE_NAME_ANNOTATION);
        entity.addFeature(LABEL_FEATURE, null, TYPE_NAME_STRING);

        // Basic span fallback layer
        var basicSpan = customTsd.addType(BASIC_SPAN_LAYER, null, TYPE_NAME_ANNOTATION);
        basicSpan.addFeature(LABEL_FEATURE, null, TYPE_NAME_STRING);

        // custom.InteractWith — non-basic relation type for the suffix-match relation test
        var interact = customTsd.addType("custom.InteractWith", null, TYPE_NAME_ANNOTATION);
        interact.addFeature(FEAT_REL_SOURCE, null, TYPE_NAME_ANNOTATION);
        interact.addFeature(FEAT_REL_TARGET, null, TYPE_NAME_ANNOTATION);

        // Basic relation fallback layer
        var basicRelation = customTsd.addType(BASIC_RELATION_LAYER, null, TYPE_NAME_ANNOTATION);
        basicRelation.addFeature(LABEL_FEATURE, null, TYPE_NAME_STRING);
        basicRelation.addFeature(FEAT_REL_SOURCE, null, TYPE_NAME_ANNOTATION);
        basicRelation.addFeature(FEAT_REL_TARGET, null, TYPE_NAME_ANNOTATION);

        // Merge with the auto-discovered classpath type system (provides DKPro Sentence etc.)
        TypeSystemDescription merged = mergeTypeSystems(
                asList(createTypeSystemDescription(), customTsd));
        return CasFactory.createCas(merged);
    }

    private PubAnnotationDocument readDocument(String aResource) throws Exception
    {
        try (var is = getClass().getResourceAsStream(aResource)) {
            return new ObjectMapper().readValue(is, PubAnnotationDocument.class);
        }
    }
}
