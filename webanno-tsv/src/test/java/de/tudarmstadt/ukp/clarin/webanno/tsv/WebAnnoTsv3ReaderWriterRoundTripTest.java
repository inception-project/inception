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
import org.apache.uima.fit.pipeline.SimplePipeline;
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
        String targetFolder = "target/test-output/WebAnnoTsv3ReaderWriterRoundTripTest/"
                + referenceFolder.getName();
        
        CollectionReaderDescription reader = createReaderDescription(WebannoTsv3Reader.class, 
                WebannoTsv3Reader.PARAM_SOURCE_LOCATION, referenceFolder,
                WebannoTsv3Reader.PARAM_PATTERNS, "reference.tsv");
        
        AnalysisEngineDescription writer = createEngineDescription(WebannoTsv3Writer.class, 
                WebannoTsv3Writer.PARAM_TARGET_LOCATION, targetFolder,
                WebannoTsv3Writer.PARAM_STRIP_EXTENSION, true,
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
        
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
