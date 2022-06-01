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
package de.tudarmstadt.ukp.inception.curation.merge;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.loadWebAnnoTsv3;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CasFactory.createText;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3XWriter;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class CasMergeSuiteTest
    extends CasMergeTestBase
{

    public static Iterable<File> tsvFiles()
    {
        return asList(new File("src/test/resources/testsuite/")
                .listFiles((FilenameFilter) new SuffixFileFilter(asList("Test"))));
    }

    @ParameterizedTest(name = "{index}: running on data {0}")
    @MethodSource("tsvFiles")
    public void runTest(File aReferenceFolder) throws Exception
    {
        Map<String, List<CAS>> casByUser = new HashMap<>();

        File[] inputFiles = aReferenceFolder
                .listFiles((FilenameFilter) new RegexFileFilter("user.*\\.tsv"));

        for (File inputFile : inputFiles) {
            casByUser.put(inputFile.getName(), List.of(loadWebAnnoTsv3(inputFile).getCas()));
        }

        CAS curatorCas = createText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        DiffResult result = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        sut.reMergeCas(result, document, "dummyTargetUser", curatorCas,
                getSingleCasByUser(casByUser));

        writeAndAssertEquals(curatorCas, aReferenceFolder);
    }

    private Map<String, CAS> getSingleCasByUser(Map<String, List<CAS>> aCasByUserSingle)
    {
        Map<String, CAS> casByUserSingle = new HashMap<>();
        for (String user : aCasByUserSingle.keySet()) {
            casByUserSingle.put(user, aCasByUserSingle.get(user).get(0));
        }

        return casByUserSingle;
    }

    private void writeAndAssertEquals(CAS curatorCas, File aReferenceFolder) throws Exception
    {
        String targetFolder = "target/test-output/" + getClass().getSimpleName() + "/"
                + aReferenceFolder.getName();

        DocumentMetaData dmd = DocumentMetaData.get(curatorCas);
        dmd.setDocumentId("curator");
        runPipeline(curatorCas,
                createEngineDescription(WebannoTsv3XWriter.class,
                        WebannoTsv3XWriter.PARAM_TARGET_LOCATION, targetFolder,
                        WebannoTsv3XWriter.PARAM_OVERWRITE, true));

        File referenceFile = new File(aReferenceFolder, "curator.tsv");
        assumeTrue(referenceFile.exists(), "No reference data available for this test.");

        File actualFile = new File(targetFolder, "curator.tsv");

        String reference = FileUtils.readFileToString(referenceFile, "UTF-8");
        String actual = FileUtils.readFileToString(actualFile, "UTF-8");

        assertThat(actual).isEqualToNormalizingNewlines(reference);
    }
}
