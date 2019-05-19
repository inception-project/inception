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
package de.tudarmstadt.ukp.clarin.webanno.tcf;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.CasDumpWriter;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.InputSource;

import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
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
                TcfWriter.PARAM_FILENAME_EXTENSION, ".xml",
                TcfWriter.PARAM_STRIP_EXTENSION, true,
                TcfWriter.PARAM_OVERWRITE, true);

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
            assertEquals("Layer size mismatch in [" + layer.getClass().getName() + "]",
                    layer.size(), getLayer(aCorpusDataActual, layer.getClass()).size());
        }

        XMLAssert.assertXMLEqual(
                new InputSource("src/test/resources/" + aExpectedFile),
                new InputSource(new File("target/test-output/oneway/" + aInputFile).getPath()));
    }

    private static TextCorpusLayer getLayer(TextCorpus aCorpus,
            Class<? extends TextCorpusLayer> aLayerType)
    {
        for (TextCorpusLayer layer : aCorpus.getLayers()) {
            if (layer.getClass().equals(aLayerType)) {
                return layer;
            }
        }
        throw new IllegalArgumentException("No layer of type [" + aLayerType.getName() + "]");
    }
    
    @Test
    public void testRoundtrip()
        throws Exception
    {
        CollectionReaderDescription reader = createReaderDescription(TcfReader.class, 
                TcfReader.PARAM_SOURCE_LOCATION, "src/test/resources/",
                TcfReader.PARAM_PATTERNS, "wlfxb.xml");

        AnalysisEngineDescription writer = createEngineDescription(
                TcfWriter.class,
                TcfWriter.PARAM_TARGET_LOCATION, "target/test-output/roundtrip",
                TcfWriter.PARAM_FILENAME_EXTENSION, ".xml",
                TcfWriter.PARAM_STRIP_EXTENSION, true,
                TcfWriter.PARAM_OVERWRITE, true);

        runPipeline(reader, writer);

        String expected = contentOf(new File("src/test/resources/wlfxb.xml"), UTF_8);
        String actual = contentOf(new File("target/test-output/roundtrip/wlfxb.xml"), UTF_8);
        assertThat(expected).isXmlEqualTo(actual);
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
