/*******************************************************************************
 * Copyright 2016
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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

@RunWith(value = Parameterized.class)
public class WebAnnoTsv3ReaderWriterRoundTripTest
{
    @Parameters(name = "{index}: running on file {0}")
    public static Iterable<File> tsvFiles()
    {
        return asList(new File("src/test/resources/")
                .listFiles((FilenameFilter) new PrefixFileFilter("WebAnnoTsv3ReaderWriterTest-")));
    }

    private File referenceFolder;

    public WebAnnoTsv3ReaderWriterRoundTripTest(File aFolder)
    {
        referenceFolder = aFolder;
    }

    @Test
    public void runTest() throws Exception
    {
        if (referenceFolder.getPath().contains("testSimpleChain")) {
            return;
        }
        
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");
       
        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));
        
        String targetFolder = "target/test-output/WebAnnoTsv3ReaderWriterRoundTripTest/"
                + referenceFolder.getName();
        
        CollectionReaderDescription reader = createReaderDescription(WebannoTsv3Reader.class,
                merged,
                WebannoTsv3Reader.PARAM_SOURCE_LOCATION, referenceFolder,
                WebannoTsv3Reader.PARAM_PATTERNS, "reference.tsv");
        
        AnalysisEngineDescription writer = createEngineDescription(WebannoTsv3Writer.class,
                merged,
                WebannoTsv3Writer.PARAM_TARGET_LOCATION, targetFolder,
                WebannoTsv3Writer.PARAM_STRIP_EXTENSION, true,
                WebannoTsv3Writer.PARAM_CHAIN_LAYERS, asList("webanno.custom.Simple"),
                WebannoTsv3Writer.PARAM_SLOT_FEATS, asList("webanno.custom.SimpleLinkHost:links"),
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class,"webanno.custom.SimpleSpan", "webanno.custom.SimpleLinkHost"), 
                WebannoTsv3Writer.PARAM_LINK_TYPES, asList("webanno.custom.LinkType"),
                WebannoTsv3Writer.PARAM_SLOT_TARGETS, asList("webanno.custom.SimpleSpan"),
                WebannoTsv3Writer.PARAM_RELATION_LAYERS, asList("webanno.custom.SimpleRelation",
                        "webanno.custom.Relation"));
        
        
        SimplePipeline.runPipeline(reader, writer);
        
        String reference = FileUtils.readFileToString(new File(referenceFolder, "reference.tsv"),
                "UTF-8");
        
        String actual = FileUtils.readFileToString(new File(targetFolder, "reference.tsv"),
                "UTF-8");
        
        assertEquals(reference, actual);
    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
