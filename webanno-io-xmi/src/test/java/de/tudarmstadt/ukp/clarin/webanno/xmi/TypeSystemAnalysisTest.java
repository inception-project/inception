/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.xmi;

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class TypeSystemAnalysisTest
{
    @Before
    public void setup()
    {
        ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
    }
    
    @Test
    public void testSpanWithPrimitiveFeatures() throws Exception
    {
        TypeSystemDescription tsd = createTypeSystemDescription("tsd/spanWithPrimitiveFeatures");
        TypeSystemAnalysis analysis = TypeSystemAnalysis.of(tsd);
        
        AnnotationLayer spanLayer = new AnnotationLayer();
        spanLayer.setName("webanno.custom.Span");
        spanLayer.setUiName("Span");
        spanLayer.setType(WebAnnoConst.SPAN_TYPE);
        spanLayer.setAnchoringMode(AnchoringMode.CHARACTERS);
        spanLayer.setAllowStacking(true);
        spanLayer.setCrossSentence(true);
        
        AnnotationFeature stringFeature = new AnnotationFeature(
                "stringFeature", CAS.TYPE_NAME_STRING);
        AnnotationFeature intFeature = new AnnotationFeature(
                "intFeature", CAS.TYPE_NAME_INTEGER);
        AnnotationFeature booleanFeature = new AnnotationFeature(
                "booleanFeature", CAS.TYPE_NAME_BOOLEAN);
        AnnotationFeature floatFeature = new AnnotationFeature(
                "floatFeature", CAS.TYPE_NAME_FLOAT);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(analysis.getLayers())
            .containsExactly(spanLayer)
            .usingFieldByFieldElementComparator();
        softly.assertThat(analysis.getFeatures(spanLayer.getName()))
            .containsExactlyInAnyOrder(stringFeature, intFeature, booleanFeature, floatFeature)
            .usingFieldByFieldElementComparator();
        softly.assertAll();
    }
    
    @Test
    public void testSpanWithSlotFeatures() throws Exception
    {
        TypeSystemDescription tsd = createTypeSystemDescription("tsd/spanWithSlotFeatures");
        TypeSystemAnalysis analysis = TypeSystemAnalysis.of(tsd);
        
        AnnotationLayer slotSpanLayer = new AnnotationLayer();
        slotSpanLayer.setName("webanno.custom.SlotSpan");
        slotSpanLayer.setUiName("SlotSpan");
        slotSpanLayer.setType(WebAnnoConst.SPAN_TYPE);
        slotSpanLayer.setAnchoringMode(AnchoringMode.CHARACTERS);
        slotSpanLayer.setAllowStacking(true);
        slotSpanLayer.setCrossSentence(true);
        
        AnnotationFeature freeSlot = new AnnotationFeature(
                "freeSlot", CAS.TYPE_NAME_ANNOTATION);

        AnnotationFeature boundSlot = new AnnotationFeature(
                "boundSlot", "webanno.custom.SlotSpan");

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(analysis.getLayers())
            .containsExactly(slotSpanLayer)
            .usingFieldByFieldElementComparator();
        softly.assertThat(analysis.getFeatures(slotSpanLayer.getName()))
            .containsExactlyInAnyOrder(freeSlot, boundSlot)
            .usingFieldByFieldElementComparator();
        softly.assertAll();
    }
    
    @Test
    public void testRelationWithPrimitiveFeatures() throws Exception
    {
        TypeSystemDescription tsd = createTypeSystemDescription(
                "tsd/relationWithPrimitiveFeatures");
        TypeSystemAnalysis analysis = TypeSystemAnalysis.of(tsd);
        
        AnnotationLayer relationLayer = new AnnotationLayer();
        relationLayer.setName("webanno.custom.Relation");
        relationLayer.setUiName("Relation");
        relationLayer.setType(WebAnnoConst.RELATION_TYPE);
        relationLayer.setAnchoringMode(AnchoringMode.CHARACTERS);
        relationLayer.setAllowStacking(true);
        relationLayer.setCrossSentence(true);

        AnnotationLayer relationTargetLayer = new AnnotationLayer();
        relationTargetLayer.setName("webanno.custom.RelationTarget");
        relationTargetLayer.setUiName("RelationTarget");
        relationTargetLayer.setType(WebAnnoConst.SPAN_TYPE);
        relationTargetLayer.setAnchoringMode(AnchoringMode.CHARACTERS);
        relationTargetLayer.setAllowStacking(true);
        relationTargetLayer.setCrossSentence(true);

        AnnotationFeature stringFeature = new AnnotationFeature(
                "stringFeature", CAS.TYPE_NAME_STRING);
        AnnotationFeature intFeature = new AnnotationFeature(
                "intFeature", CAS.TYPE_NAME_INTEGER);
        AnnotationFeature booleanFeature = new AnnotationFeature(
                "booleanFeature", CAS.TYPE_NAME_BOOLEAN);
        AnnotationFeature floatFeature = new AnnotationFeature(
                "floatFeature", CAS.TYPE_NAME_FLOAT);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(analysis.getLayers())
            .containsExactlyInAnyOrder(relationLayer, relationTargetLayer)
            .usingFieldByFieldElementComparator();
        softly.assertThat(analysis.getFeatures(relationLayer.getName()))
                .containsExactlyInAnyOrder(stringFeature, intFeature, booleanFeature, floatFeature)
                .usingFieldByFieldElementComparator();
        softly.assertAll();
    }
    
    @Test
    public void testChain() throws Exception
    {
        TypeSystemDescription tsd = createTypeSystemDescription(
                "tsd/chain");
        TypeSystemAnalysis analysis = TypeSystemAnalysis.of(tsd);
        
        AnnotationLayer chainLayer = new AnnotationLayer();
        chainLayer.setName("webanno.custom.Chain");
        chainLayer.setUiName("Chain");
        chainLayer.setType(WebAnnoConst.CHAIN_TYPE);
        chainLayer.setAnchoringMode(AnchoringMode.CHARACTERS);
        chainLayer.setAllowStacking(true);
        chainLayer.setCrossSentence(true);

        AnnotationFeature referenceRelationFeature = new AnnotationFeature(
                "referenceRelation", CAS.TYPE_NAME_STRING);
        AnnotationFeature referenceTypeFeature = new AnnotationFeature(
                "referenceType", CAS.TYPE_NAME_STRING);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(analysis.getLayers())
            .containsExactlyInAnyOrder(chainLayer)
            .usingFieldByFieldElementComparator();
        softly.assertThat(analysis.getFeatures(chainLayer.getName()))
            .containsExactlyInAnyOrder(referenceRelationFeature, referenceTypeFeature)
            .usingFieldByFieldElementComparator();
        softly.assertAll();
    }

    @Test
    public void testTheFullMonty() throws Exception
    {
        TypeSystemDescription tsd = createTypeSystemDescription(
                "tsd/fullMonty");
        TypeSystemAnalysis analysis = TypeSystemAnalysis.of(tsd);
        
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(analysis.getLayers())
            .extracting(l -> l.getName() + ":" + l.getType())
            .hasSize(27)
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
                    "webanno.custom.Chain:chain",
                    "webanno.custom.Relation:relation",
                    "webanno.custom.SlotSpan:span");
        softly.assertAll();
    }
    
    @Test
    public void testCTakes40() throws Exception
    {
        TypeSystemDescription tsd = createTypeSystemDescription(
                "3rd-party-tsd/ctakes-type-system-4_0");
        TypeSystemAnalysis analysis = TypeSystemAnalysis.of(tsd);
        
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(analysis.getLayers())
            .extracting(l -> l.getName() + ":" + l.getType())
            .hasSize(8)
            .containsExactlyInAnyOrder(
                    "org.apache.ctakes.typesystem.type.textspan.Segment:span",
                    "org.apache.ctakes.typesystem.type.textspan.Sentence:span",
                    "org.apache.ctakes.typesystem.type.syntax.Chunk:span",
                    "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation:span",
                    "org.apache.ctakes.typesystem.type.temporary.assertion.AssertionCuePhraseAnnotation:span",
                    "org.apache.ctakes.typesystem.type.textspan.Paragraph:span",
                    "org.apache.ctakes.typesystem.type.textspan.ListEntry:span",
                    "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation:span");
        softly.assertAll();
    }
    
    @Test
    public void testCcpTypeSystem() throws Exception
    {
        TypeSystemDescription tsd = createTypeSystemDescription(
                "3rd-party-tsd/CcpTypeSystem");
        TypeSystemAnalysis analysis = TypeSystemAnalysis.of(tsd);
        
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(analysis.getLayers())
            .hasSize(0);
        softly.assertAll();
    }
    
    @Test
    public void testTtcTermSuite() throws Exception
    {
        TypeSystemDescription tsd = createTypeSystemDescription(
                "3rd-party-tsd/ttc-term-suite");
        TypeSystemAnalysis analysis = TypeSystemAnalysis.of(tsd);
        
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(analysis.getLayers())
            .extracting(l -> l.getName() + ":" + l.getType())
            .hasSize(4)
            .containsExactlyInAnyOrder(
                    "eu.project.ttc.types.WordAnnotation:span",
                    "eu.project.ttc.types.TermComponentAnnotation:span",
                    "eu.project.ttc.types.TranslationCandidateAnnotation:span",
                    "eu.project.ttc.types.FormAnnotation:span");
        softly.assertAll();
    }
    
    @Test
    public void testCreta() throws Exception
    {
        TypeSystemDescription tsd = createTypeSystemDescription(
                "3rd-party-tsd/creta-typesystem");
        TypeSystemAnalysis analysis = TypeSystemAnalysis.of(tsd);
        
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(analysis.getLayers())
            .extracting(l -> l.getName() + ":" + l.getType())
            .hasSize(58)
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
                    "org.cleartk.srl.type.Chunk:span",
                    "org.cleartk.timeml.type.Anchor:span",
                    "org.cleartk.timeml.type.Event:span",
                    "org.cleartk.token.type.Subtoken:span",
                    "org.cleartk.token.type.Token:span");
        softly.assertAll();
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
