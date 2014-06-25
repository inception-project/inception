/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.tcf;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.CasDumpWriter;
import org.junit.Before;
import org.junit.Test;

import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.tc.api.TextCorpus;
import eu.clarin.weblicht.wlfxb.tc.api.TextCorpusLayer;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;

public class TcfReaderWriterTest
{
    @Test
    public void test1()
            throws Exception
    {
        testOneWay("tcf-after.xml", "tcf-after-expected.xml");
    }

    @Test
    public void testWithCmdMetadata()
            throws Exception
    {
        testOneWay("tcf04-karin-wl.xml", "tcf04-karin-wl_expected.xml");
    }

    public void testOneWay(String aInputFile, String aExpectedFile)
        throws Exception
    {
        CollectionReaderDescription reader = createReaderDescription(TcfReader.class, 
                TcfReader.PARAM_SOURCE_LOCATION, "src/test/resources/",
                TcfReader.PARAM_PATTERNS, aInputFile);

        AnalysisEngineDescription writer = createEngineDescription(
                TcfWriter.class,
                TcfWriter.PARAM_TARGET_LOCATION, "target/test-output/oneway",
                TcfWriter.PARAM_FILENAME_SUFFIX, ".xml",
                TcfWriter.PARAM_STRIP_EXTENSION, true);

        AnalysisEngineDescription dumper = createEngineDescription(CasDumpWriter.class,
                CasDumpWriter.PARAM_OUTPUT_FILE, "target/test-output/oneway/dump.txt");

        runPipeline(reader, writer, dumper);

        InputStream isReference = new FileInputStream(new File("src/test/resources/"
                + aExpectedFile));

        InputStream isActual = new FileInputStream(new File("target/test-output/oneway/"
                + aInputFile));

        WLData wLDataReference = WLDObjector.read(isReference);
        TextCorpusStored aCorpusDataReference = wLDataReference.getTextCorpus();

        WLData wLDataActual = WLDObjector.read(isActual);
        TextCorpusStored aCorpusDataActual = wLDataActual.getTextCorpus();

        // check if layers maintained
        assertEquals(aCorpusDataReference.getLayers().size(), aCorpusDataActual.getLayers().size());

        // Check if every layers have the same number of annotations
        for (TextCorpusLayer layer : aCorpusDataReference.getLayers()) {
            assertEquals(
                    "Layer size mismatch in ["+layer.getClass().getName()+"]",
                    layer.size(), 
                    getLayer(aCorpusDataActual, layer.getClass()).size());
        }
        
        String reference = FileUtils.readFileToString(
                new File("src/test/resources/" + aExpectedFile), "UTF-8");
        String actual = FileUtils.readFileToString(
                new File("target/test-output/oneway/" + aInputFile), "UTF-8");
        assertEquals(reference, actual);
    }

    private static TextCorpusLayer getLayer(TextCorpus aCorpus, Class<? extends TextCorpusLayer> aLayerType)
    {
        for (TextCorpusLayer layer : aCorpus.getLayers()) {
            if (layer.getClass().equals(aLayerType)) {
                return layer;
            }
        }
        throw new IllegalArgumentException("No layer of type [" + aLayerType.getName() + "]");
    }
    
    @Test
    // @Ignore("The TCF library generates different xml namespaces and assertEquals fails on Jenkins ")
    public void testRoundtrip()
        throws Exception
    {
        CollectionReaderDescription reader = createReaderDescription(TcfReader.class, 
                TcfReader.PARAM_SOURCE_LOCATION, "src/test/resources/",
                TcfReader.PARAM_PATTERNS, "wlfxb.xml");

        AnalysisEngineDescription writer = createEngineDescription(
                TcfWriter.class,
                TcfWriter.PARAM_TARGET_LOCATION, "target/test-output/roundtrip",
                TcfWriter.PARAM_FILENAME_SUFFIX, ".xml",
                TcfWriter.PARAM_STRIP_EXTENSION, true);

        runPipeline(reader, writer);

        String reference = FileUtils.readFileToString(
                new File("src/test/resources/wlfxb.xml"), "UTF-8");
        String actual = FileUtils.readFileToString(
                new File("target/test-output/roundtrip/wlfxb.xml"), "UTF-8");
        assertEquals(reference, actual);
    }

    @Before
    public void setupLogging()
    {
        System.setProperty("org.apache.uima.logger.class", "org.apache.uima.util.impl.Log4jLogger_impl");
    }
}
