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
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.loadWebAnnoTsv3;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createCasCopy;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.uima.cas.CAS;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3XWriter;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

@Execution(CONCURRENT)
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
        var casByUser = new HashMap<String, CAS>();

        var inputFiles = aReferenceFolder
                .listFiles((FilenameFilter) new RegexFileFilter("user.*\\.tsv"));

        for (var inputFile : inputFiles) {
            casByUser.put(inputFile.getName(), loadWebAnnoTsv3(inputFile).getCas());
        }

        var curatorCas = createCasCopy(casByUser.values().stream().findFirst().get());

        var result = doDiff(diffAdapters, casByUser).toResult();

        sut.clearAndMergeCas(result, document, "dummyTargetUser", curatorCas, casByUser);

        writeAndAssertEquals(curatorCas, aReferenceFolder);
    }

    private void writeAndAssertEquals(CAS curatorCas, File aReferenceFolder) throws Exception
    {
        var targetFolder = "target/test-output/" + getClass().getSimpleName() + "/"
                + aReferenceFolder.getName();

        var dmd = DocumentMetaData.get(curatorCas);
        dmd.setDocumentId("curator");
        runPipeline(curatorCas, createEngineDescription( //
                WebannoTsv3XWriter.class, //
                WebannoTsv3XWriter.PARAM_USE_DOCUMENT_ID, true, //
                WebannoTsv3XWriter.PARAM_TARGET_LOCATION, targetFolder, //
                WebannoTsv3XWriter.PARAM_OVERWRITE, true));

        var referenceFile = new File(aReferenceFolder, "curator.tsv");
        assumeTrue(referenceFile.exists(), "No reference data available for this test.");

        var actualFile = new File(targetFolder, "curator.tsv");

        var reference = contentOf(referenceFile, UTF_8);
        var actual = contentOf(actualFile, UTF_8);

        assertThat(actual).isEqualToNormalizingNewlines(reference);
    }
}
