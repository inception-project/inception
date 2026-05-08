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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.util;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;
import static org.apache.uima.cas.CAS.TYPE_NAME_FLOAT;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;

@ExtendWith(SoftAssertionsExtension.class)
public class TypeSystemAnalysisTest
{
    @BeforeEach
    public void setup()
    {
        ToStringBuilder.setDefaultStyle(SHORT_PREFIX_STYLE);
    }

    @Test
    public void testSpanWithArrayFeature(SoftAssertions softly) throws Exception
    {
        var tsd = createTypeSystemDescription("tsd/spanWithArrayFeatures");
        var analysis = TypeSystemAnalysis.of(tsd);

        var spanLayer = new AnnotationLayer();
        spanLayer.setName("webanno.custom.Span");
        spanLayer.setUiName("Span");
        spanLayer.setType(SpanLayerSupport.TYPE);
        spanLayer.setAnchoringMode(CHARACTERS);
        spanLayer.setOverlapMode(ANY_OVERLAP);
        spanLayer.setCrossSentence(true);

        var stringsFeature = new AnnotationFeature("stringsFeature", TYPE_NAME_STRING_ARRAY);
        stringsFeature.setMode(ARRAY);

        softly.assertThat(analysis.getLayers()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactly(spanLayer);
        softly.assertThat(analysis.getFeatures(spanLayer.getName()))
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrder(stringsFeature);
    }

    @Test
    public void testSpanWithPrimitiveFeatures(SoftAssertions softly) throws Exception
    {
        var tsd = createTypeSystemDescription("tsd/spanWithPrimitiveFeatures");
        var analysis = TypeSystemAnalysis.of(tsd);

        var spanLayer = new AnnotationLayer();
        spanLayer.setName("webanno.custom.Span");
        spanLayer.setUiName("Span");
        spanLayer.setType(SpanLayerSupport.TYPE);
        spanLayer.setAnchoringMode(CHARACTERS);
        spanLayer.setOverlapMode(ANY_OVERLAP);
        spanLayer.setCrossSentence(true);

        var stringFeature = new AnnotationFeature("stringFeature", TYPE_NAME_STRING);
        var intFeature = new AnnotationFeature("intFeature", TYPE_NAME_INTEGER);
        var booleanFeature = new AnnotationFeature("booleanFeature", TYPE_NAME_BOOLEAN);
        var floatFeature = new AnnotationFeature("floatFeature", TYPE_NAME_FLOAT);

        softly.assertThat(analysis.getLayers()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactly(spanLayer);
        softly.assertThat(analysis.getFeatures(spanLayer.getName()))
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrder(stringFeature, intFeature, booleanFeature, floatFeature);
    }

    @Test
    public void testSpanWithSlotFeatures() throws Exception
    {
        var tsd = createTypeSystemDescription("tsd/spanWithSlotFeatures");
        var analysis = TypeSystemAnalysis.of(tsd);

        var slotSpanLayer = new AnnotationLayer();
        slotSpanLayer.setName("webanno.custom.SlotSpan");
        slotSpanLayer.setUiName("SlotSpan");
        slotSpanLayer.setType(SpanLayerSupport.TYPE);
        slotSpanLayer.setAnchoringMode(CHARACTERS);
        slotSpanLayer.setOverlapMode(ANY_OVERLAP);
        slotSpanLayer.setCrossSentence(true);

        var freeSlot = AnnotationFeature.builder() //
                .withName("freeSlot") //
                .withUiName("freeSlot") //
                .withType(TYPE_NAME_ANNOTATION) //
                .withLinkMode(WITH_ROLE) //
                .withLinkTypeName("webanno.custom.SlotSpanFreeSlotLink") //
                .withLinkTypeRoleFeatureName("role") //
                .withLinkTypeTargetFeatureName("target") //
                .withMultiValueMode(ARRAY) //
                .build();

        var boundSlot = AnnotationFeature.builder() //
                .withName("boundSlot") //
                .withUiName("boundSlot") //
                .withType("webanno.custom.SlotSpan") //
                .withLinkMode(WITH_ROLE) //
                .withLinkTypeName("webanno.custom.SlotSpanBoundSlotLink") //
                .withLinkTypeRoleFeatureName("role") //
                .withLinkTypeTargetFeatureName("target") //
                .withMultiValueMode(ARRAY) //
                .build();

        assertThat(analysis.getLayers()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrder(slotSpanLayer);
        assertThat(analysis.getFeatures(slotSpanLayer.getName()))
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrder(freeSlot, boundSlot);
    }

    @Test
    public void testRelationWithPrimitiveFeatures() throws Exception
    {
        var tsd = createTypeSystemDescription("tsd/relationWithPrimitiveFeatures");
        var analysis = TypeSystemAnalysis.of(tsd);

        var relationTargetLayer = new AnnotationLayer();
        relationTargetLayer.setName("webanno.custom.RelationTarget");
        relationTargetLayer.setUiName("RelationTarget");
        relationTargetLayer.setType(SpanLayerSupport.TYPE);
        relationTargetLayer.setAnchoringMode(CHARACTERS);
        relationTargetLayer.setOverlapMode(ANY_OVERLAP);
        relationTargetLayer.setCrossSentence(true);

        var relationLayer = new AnnotationLayer();
        relationLayer.setName("webanno.custom.Relation");
        relationLayer.setUiName("Relation");
        relationLayer.setType(RelationLayerSupport.TYPE);
        relationLayer.setAnchoringMode(CHARACTERS);
        relationLayer.setOverlapMode(ANY_OVERLAP);
        relationLayer.setCrossSentence(true);
        // Layer attachment is handled in AnnotationSchemaServiceImpl!
        relationLayer.setAttachType(null);

        var stringFeature = new AnnotationFeature("stringFeature", TYPE_NAME_STRING);
        var intFeature = new AnnotationFeature("intFeature", TYPE_NAME_INTEGER);
        var booleanFeature = new AnnotationFeature("booleanFeature", TYPE_NAME_BOOLEAN);
        var floatFeature = new AnnotationFeature("floatFeature", TYPE_NAME_FLOAT);

        assertThat(analysis.getLayers()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrder(relationLayer, relationTargetLayer);
        assertThat(analysis.getFeatures(relationLayer.getName()))
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrder(stringFeature, intFeature, booleanFeature, floatFeature);
    }

    @Test
    public void testCrossLayerRelation() throws Exception
    {
        var tsd = createTypeSystemDescription("tsd/crossLayerRelation");
        var analysis = TypeSystemAnalysis.of(tsd);

        var relationLayer = new AnnotationLayer();
        relationLayer.setName("webanno.custom.Relation");
        relationLayer.setUiName("Relation");
        relationLayer.setType(RelationLayerSupport.TYPE);
        relationLayer.setAnchoringMode(CHARACTERS);
        relationLayer.setOverlapMode(ANY_OVERLAP);
        relationLayer.setCrossSentence(true);
        // Layer attachment is handled in AnnotationSchemaServiceImpl!
        relationLayer.setAttachType(null);

        var stringFeature = new AnnotationFeature("stringFeature", TYPE_NAME_STRING);
        var intFeature = new AnnotationFeature("intFeature", TYPE_NAME_INTEGER);
        var booleanFeature = new AnnotationFeature("booleanFeature", TYPE_NAME_BOOLEAN);
        var floatFeature = new AnnotationFeature("floatFeature", TYPE_NAME_FLOAT);

        assertThat(analysis.getLayers()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrder(relationLayer);

        assertThat(analysis.getFeatures(relationLayer.getName()))
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrder(stringFeature, intFeature, booleanFeature, floatFeature);
    }

    @Test
    public void testChain(SoftAssertions softly) throws Exception
    {
        var tsd = createTypeSystemDescription("tsd/chain");
        var analysis = TypeSystemAnalysis.of(tsd);

        var chainLayer = new AnnotationLayer();
        chainLayer.setName("webanno.custom.Chain");
        chainLayer.setUiName("Chain");
        chainLayer.setType(ChainLayerSupport.TYPE);
        chainLayer.setAnchoringMode(CHARACTERS);
        chainLayer.setOverlapMode(ANY_OVERLAP);
        chainLayer.setCrossSentence(true);

        var referenceRelationFeature = new AnnotationFeature("referenceRelation", TYPE_NAME_STRING);
        var referenceTypeFeature = new AnnotationFeature("referenceType", TYPE_NAME_STRING);

        softly.assertThat(analysis.getLayers()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrder(chainLayer);
        softly.assertThat(analysis.getFeatures(chainLayer.getName()))
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrder(referenceRelationFeature, referenceTypeFeature);
    }

    @Test
    public void testTheFullMonty() throws Exception
    {
        var tsd = createTypeSystemDescription("tsd/fullMonty");
        var analysis = TypeSystemAnalysis.of(tsd);

        assertThat(analysis.getLayers()) //
                .extracting(l -> l.getName() + ":" + l.getType()) //
                .containsExactlyInAnyOrder(
                        "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference:chain",
                        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.Morpheme:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.MetaDataStringField:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Div:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.LexicalPhrase:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.NGram:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.StopWord:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.TokenForm:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemanticArgument:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemanticField:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.WordSense:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.PennTree:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.Tag:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency:relation",
                        "de.tudarmstadt.ukp.dkpro.core.api.transform.type.SofaChangeAnnotation:span",
                        "webanno.custom.Chain:chain", //
                        "webanno.custom.Relation:relation", //
                        "webanno.custom.SlotSpan:span");
    }

    @Test
    public void testCTakes40() throws Exception
    {
        var tsd = createTypeSystemDescription("3rd-party-tsd/ctakes-type-system-4_0");
        var analysis = TypeSystemAnalysis.of(tsd);

        assertThat(analysis.getLayers()) //
                .extracting(l -> l.getName() + ":" + l.getType()) //
                .containsExactlyInAnyOrder(
                        "org.apache.ctakes.typesystem.type.textspan.Segment:span",
                        "org.apache.ctakes.typesystem.type.textspan.Sentence:span",
                        "org.apache.ctakes.typesystem.type.syntax.Chunk:span",
                        "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation:span",
                        "org.apache.ctakes.typesystem.type.temporary.assertion.AssertionCuePhraseAnnotation:span",
                        "org.apache.ctakes.typesystem.type.textspan.Paragraph:span",
                        "org.apache.ctakes.typesystem.type.textspan.ListEntry:span",
                        "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation:span");
    }

    @Test
    public void testCcpTypeSystem() throws Exception
    {
        var tsd = createTypeSystemDescription("3rd-party-tsd/CcpTypeSystem");
        var analysis = TypeSystemAnalysis.of(tsd);

        assertThat(analysis.getLayers()).isEmpty();
    }

    @Test
    public void testTtcTermSuite() throws Exception
    {
        var tsd = createTypeSystemDescription("3rd-party-tsd/ttc-term-suite");
        var analysis = TypeSystemAnalysis.of(tsd);

        assertThat(analysis.getLayers()) //
                .extracting(l -> l.getName() + ":" + l.getType()) //
                .containsExactlyInAnyOrder( //
                        "eu.project.ttc.types.WordAnnotation:span",
                        "eu.project.ttc.types.TermComponentAnnotation:span",
                        "eu.project.ttc.types.TranslationCandidateAnnotation:span",
                        "eu.project.ttc.types.FormAnnotation:span");
    }

    @Test
    public void testCreta() throws Exception
    {
        var tsd = createTypeSystemDescription("3rd-party-tsd/creta-typesystem");
        var analysis = TypeSystemAnalysis.of(tsd);

        assertThat(analysis.getLayers()) //
                .extracting(l -> l.getName() + ":" + l.getType()) //
                .containsExactlyInAnyOrder(
                        "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference:chain",
                        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.Morpheme:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.MetaDataStringField:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Div:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.NGram:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.StopWord:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemanticArgument:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemanticField:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.WordSense:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.PennTree:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.Tag:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk:span",
                        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency:relation",
                        "de.uni_potsdam.acl.type.Chunk:span",
                        "de.uni_potsdam.acl.type.ClusteredEventMention:span",
                        "de.uni_potsdam.acl.type.Mention:span",
                        "de.uni_potsdam.acl.type.Negation:span",
                        "de.uni_potsdam.acl.type.Sentiment:span",
                        "de.unihd.dbs.uima.types.heideltime.Dct:span",
                        "de.unihd.dbs.uima.types.heideltime.Sentence:span",
                        "de.unihd.dbs.uima.types.heideltime.SourceDocInfo:span",
                        "de.unihd.dbs.uima.types.heideltime.Timex3:span",
                        "de.unihd.dbs.uima.types.heideltime.Timex3Interval:span",
                        "de.unihd.dbs.uima.types.heideltime.Token:span",
                        "de.unistuttgart.ims.creta.api.Entity:span",
                        "de.unistuttgart.ims.creta.api.ExampleType:span",
                        "de.unistuttgart.ims.creta.api.Format:span",
                        "de.unistuttgart.ims.creta.api.Line:span",
                        "de.unistuttgart.ims.creta.api.Quotation:span",
                        "de.unistuttgart.ims.creta.api.Stage:span",
                        "de.unistuttgart.ims.creta.api.Utterance:span",
                        "de.unistuttgart.ims.type.Agenda:span",
                        "de.unistuttgart.ims.type.Chunk:span",
                        "de.unistuttgart.ims.type.HumanAnnotation:span",
                        "de.unistuttgart.ims.type.Keyword:span",
                        "de.unistuttgart.ims.type.Link:span",
                        "de.unistuttgart.ims.type.Markable:span",
                        "de.unistuttgart.ims.type.NE:span",
                        "de.unistuttgart.ims.type.Paragraph:span",
                        "de.unistuttgart.ims.type.Quotation:span",
                        "de.unistuttgart.ims.type.Section:span",
                        "de.unistuttgart.ims.type.Sentence:span",
                        "de.unistuttgart.ims.type.Speaker:span",
                        "de.unistuttgart.ims.type.Token:span",
                        "de.unistuttgart.ims.uimautil.WordListDescription:span",
                        "org.cleartk.ne.type.Ace2005Document:span",
                        "org.cleartk.ne.type.Chunk:span",
                        "org.cleartk.score.type.ScoredAnnotation:span",
                        "org.cleartk.srl.type.Chunk:span", //
                        "org.cleartk.timeml.type.Anchor:span", //
                        "org.cleartk.timeml.type.Event:span",
                        "org.cleartk.token.type.Subtoken:span",
                        "org.cleartk.token.type.Token:span");
    }
}
