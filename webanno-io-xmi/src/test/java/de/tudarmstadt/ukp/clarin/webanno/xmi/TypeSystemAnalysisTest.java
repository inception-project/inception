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
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.xmi.TypeSystemAnalysis;

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
        spanLayer.setLockToTokenOffset(false);
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
        slotSpanLayer.setLockToTokenOffset(false);
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
        relationLayer.setLockToTokenOffset(false);
        relationLayer.setAllowStacking(true);
        relationLayer.setCrossSentence(true);

        AnnotationLayer relationTargetLayer = new AnnotationLayer();
        relationTargetLayer.setName("webanno.custom.RelationTarget");
        relationTargetLayer.setUiName("RelationTarget");
        relationTargetLayer.setType(WebAnnoConst.SPAN_TYPE);
        relationTargetLayer.setLockToTokenOffset(false);
        relationTargetLayer.setAllowStacking(true);
        relationTargetLayer.setCrossSentence(true);

        AnnotationFeature sourceFeature = new AnnotationFeature(
                WebAnnoConst.FEAT_REL_SOURCE, "webanno.custom.RelationTarget");
        AnnotationFeature targetFeature = new AnnotationFeature(
                WebAnnoConst.FEAT_REL_TARGET, "webanno.custom.RelationTarget");
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
            .containsExactlyInAnyOrder(sourceFeature, targetFeature, stringFeature, intFeature, 
                    booleanFeature, floatFeature)
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
        chainLayer.setLockToTokenOffset(false);
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
            .hasSize(28);
        softly.assertAll();
    }
}

