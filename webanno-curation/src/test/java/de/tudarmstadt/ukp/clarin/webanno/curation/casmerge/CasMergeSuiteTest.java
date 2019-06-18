/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.curation.casmerge;

import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.loadWebAnnoTsv3;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3XWriter;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

@RunWith(value = Parameterized.class)
public class CasMergeSuiteTest
    extends CasMergeTestBase
{
    @Parameters(name = "{index}: running on data {0}")
    public static Iterable<File> tsvFiles()
    {
        return asList(new File("src/test/resources/testsuite/").listFiles(
                (FilenameFilter) new SuffixFileFilter(asList("Test"))));
    }
    
    private File referenceFolder;

    public CasMergeSuiteTest(File aFolder) throws Exception
    {
        referenceFolder = aFolder;
    }
    
    @Test
    public void runTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = new HashMap<>();
        
        List<File> inputFiles = asList(
                referenceFolder.listFiles((FilenameFilter) new RegexFileFilter("user.*\\.tsv")));
        
        for (File inputFile : inputFiles) {
            casByUser.put(inputFile.getName(), asList(loadWebAnnoTsv3(inputFile).getCas()));
        }
        
        JCas curatorCas = createText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        writeAndAssertEquals(curatorCas);
    }

    private Map<String, CAS> getSingleCasByUser(Map<String, List<CAS>> aCasByUserSingle)
    {
        Map<String, CAS> casByUserSingle = new HashMap<>();
        for (String user : aCasByUserSingle.keySet()) {
            casByUserSingle.put(user, aCasByUserSingle.get(user).get(0));
        }

        return casByUserSingle;
    }
    
    private void writeAndAssertEquals(JCas curatorCas)
        throws Exception
    {
        String targetFolder = "target/test-output/" + testContext.getClassName() + "/"
                + referenceFolder.getName();
        
        DocumentMetaData dmd = DocumentMetaData.get(curatorCas);
        dmd.setDocumentId("curator");
        runPipeline(curatorCas, createEngineDescription(WebannoTsv3XWriter.class,
                WebannoTsv3XWriter.PARAM_TARGET_LOCATION, targetFolder,
                WebannoTsv3XWriter.PARAM_OVERWRITE, true));
        
        File referenceFile = new File(referenceFolder, "curator.tsv");
        assumeTrue("No reference data available for this test.", referenceFile.exists());
        
        File actualFile = new File(targetFolder, "curator.tsv");
        
        String reference = FileUtils.readFileToString(referenceFile, "UTF-8");
        String actual = FileUtils.readFileToString(actualFile, "UTF-8");
        
        assertEquals(reference, actual);
    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
