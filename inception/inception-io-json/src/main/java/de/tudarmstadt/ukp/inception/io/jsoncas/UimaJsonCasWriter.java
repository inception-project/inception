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
package de.tudarmstadt.ukp.inception.io.jsoncas;

import java.io.OutputStream;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.json.jsoncas2.JsonCas2Serializer;
import org.apache.uima.json.jsoncas2.mode.FeatureStructuresMode;
import org.apache.uima.json.jsoncas2.mode.OffsetConversionMode;
import org.apache.uima.json.jsoncas2.mode.SofaMode;
import org.apache.uima.json.jsoncas2.mode.TypeSystemMode;
import org.apache.uima.json.jsoncas2.ref.FullyQualifiedTypeRefGenerator;
import org.apache.uima.json.jsoncas2.ref.SequentialIdRefGenerator;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;

/**
 * UIMA JSON CAS format writer.
 */
public class UimaJsonCasWriter
    extends JCasFileWriter_ImplBase
{
    private JsonCas2Serializer jcs;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        jcs = new JsonCas2Serializer();
        jcs.setFsMode(FeatureStructuresMode.AS_ARRAY);
        jcs.setSofaMode(SofaMode.AS_REGULAR_FEATURE_STRUCTURE);
        jcs.setTypeRefGeneratorSupplier(FullyQualifiedTypeRefGenerator::new);
        jcs.setIdRefGeneratorSupplier(SequentialIdRefGenerator::new);
        jcs.setOffsetConversionMode(OffsetConversionMode.UTF_16);
        jcs.setTypeSystemMode(TypeSystemMode.MINIMAL);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException
    {
        try (OutputStream docOS = getOutputStream(aJCas, ".json")) {
            jcs.serialize(aJCas.getCas(), docOS);
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
    }
}
