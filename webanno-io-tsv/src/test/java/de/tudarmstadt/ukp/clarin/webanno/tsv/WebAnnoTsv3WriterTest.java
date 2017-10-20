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

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;

public class WebAnnoTsv3WriterTest
    extends WebAnnoTsv3WriterTestBase
{

    @Override
    protected AnalysisEngineDescription makeWriter() throws ResourceInitializationException
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
        List<String> failing = asList(
                "testStackedSubMultiTokenSpanWithFeatureValue",
                "testSubMultiTokenSpanWithFeatureValue",
                "testSubMoltuTokenSpanWithoutFeatureValues",
                "testSubMultiTokenSpanWithoutFeatureValue2",
                "testSubMultiTokenSpanWithoutFeatureValue3",
                "testSubMultiTokenSpanWithoutFeatureValue");
        
        return failing.contains(aMethodName);
    }
}
