/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;

public class WebAnnoTsv3WriterTest extends WebAnnoTsv3WriterTestBase
{
    @Override
    protected AnalysisEngineDescription makeWriter()
        throws ResourceInitializationException
    {        
        return createEngineDescription(WebannoTsv3Writer.class);
    }
    
    @Override
    protected String getSuiteName() throws ResourceInitializationException
    {
        return "tsv3-suite";
    }
    
    @Override
    protected boolean isKnownToFail(String aMethodName)
    {
        Set<String> failingTests = new HashSet<>();
        failingTests.add("testSubtokenChain");
        failingTests.add("testStackedSubMultiTokenSpanWithFeatureValue");
        failingTests.add("testSubMultiTokenSpanWithFeatureValue");
        failingTests.add("testSubMoltuTokenSpanWithoutFeatureValues");
        failingTests.add("testSubMultiTokenSpanWithoutFeatureValue");
        failingTests.add("testSubMultiTokenSpanWithoutFeatureValue2");
        failingTests.add("testSubMultiTokenSpanWithoutFeatureValue3");
        failingTests.add("testStackedComplexSlotFeatureWithoutValues");
        failingTests.add("testSingleStackedNonTokenRelationWithoutFeatureValue2");
        failingTests.add("testZeroLengthSlotFeature2");
        failingTests.add("testStackedComplexSlotFeatureWithoutSlotFillers");
        failingTests.add("testStackedSimpleSlotFeatureWithoutValues");
        failingTests.add("testZeroLengthSpanBetweenAdjacentTokens");
        failingTests.add("testUnsetSlotFeature");
        
        return failingTests.contains(aMethodName);
    }
}
