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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectSingleAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.dkpro.core.io.xmi.XmiWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class WebAnnoTsv3XReaderWriterRoundTripTest
{
    public static Iterable<File> tsvFiles()
    {
        return asList(new File("src/test/resources/tsv3-suite/").listFiles(
                (FilenameFilter) new PrefixFileFilter(asList("test", "issue", "sample"))));
    }

    private boolean isKnownToFail(String aMethodName)
    {
        Set<String> failingTests = new HashSet<>();
        // TODO With UIMAv3 the order seems to change between read and write - REC
        failingTests.add("testStackedChain");

        return failingTests.contains(aMethodName);
    }

    @BeforeEach
    public void testWatcher(TestInfo aTestInfo)
    {
        String methodName = aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
        System.out.printf("\n=== %s === %s=====================\n", methodName,
                aTestInfo.getDisplayName());
    }

    @ParameterizedTest(name = "{index}: running on file {0}")
    @MethodSource("tsvFiles")
    public void runTest(File aReferenceFolder) throws Exception
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local;
        if (new File(aReferenceFolder, "typesystem.xml").exists()) {
            local = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
                    new File(aReferenceFolder, "typesystem.xml").toString());
        }
        else {
            local = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
                    "src/test/resources/desc/type/webannoTestTypes.xml");
        }

        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));

        String targetFolder = "target/test-output/WebAnnoTsv3XReaderWriterRoundTripTest/"
                + aReferenceFolder.getName();

        // @formatter:off
        CollectionReaderDescription reader = createReaderDescription(WebannoTsv3XReader.class,
                merged,
                WebannoTsv3XReader.PARAM_SOURCE_LOCATION, aReferenceFolder,
                WebannoTsv3XReader.PARAM_PATTERNS, "reference.tsv");

        AnalysisEngineDescription checker = createEngineDescription(
                DKProCoreConventionsChecker.class);

        AnalysisEngineDescription tsvWriter = createEngineDescription(WebannoTsv3XWriter.class,
                merged,
                WebannoTsv3XWriter.PARAM_TARGET_LOCATION, targetFolder,
                WebannoTsv3XWriter.PARAM_STRIP_EXTENSION, true,
                WebannoTsv3XWriter.PARAM_OVERWRITE, true);

        AnalysisEngineDescription xmiWriter = createEngineDescription(XmiWriter.class,
                merged,
                XmiWriter.PARAM_TARGET_LOCATION, targetFolder,
                XmiWriter.PARAM_STRIP_EXTENSION, true,
                XmiWriter.PARAM_OVERWRITE, true);
        // @formatter:on

        SimplePipeline.runPipeline(reader, checker, tsvWriter, xmiWriter);

        String referenceTsv = FileUtils
                .readFileToString(new File(aReferenceFolder, "reference.tsv"), "UTF-8");

        String actualTsv = FileUtils.readFileToString(new File(targetFolder, "reference.tsv"),
                "UTF-8");

        //
        // The XMI files here are not compared semantically but using their serialization which
        // is subject to minor variations depending e.g. on the order in which annotation are
        // created in the CAS. Thus, this code is commented out and should only be used on a
        // case-by-case base to compare XMIs during development.
        //
        // String referenceXmi = FileUtils.readFileToString(new File(aReferenceFolder,
        // "reference.xmi"),
        // "UTF-8");
        //
        // String actualXmi = FileUtils.readFileToString(new File(targetFolder, "reference.xmi"),
        // "UTF-8");

        assumeFalse(isKnownToFail(aReferenceFolder.getName()), "This test is known to fail.");
        assertThat(referenceTsv).isEqualToNormalizingNewlines(actualTsv);
        // assertEquals(referenceXmi, actualXmi);
    }

    public static class DKProCoreConventionsChecker
        extends JCasAnnotator_ImplBase
    {
        @Override
        public void process(JCas aJCas) throws AnalysisEngineProcessException
        {
            for (Lemma lemma : select(aJCas, Lemma.class)) {
                Token t = selectSingleAt(aJCas, Token.class, lemma.getBegin(), lemma.getEnd());
                assert t.getLemma() == lemma;
            }

            for (Stem stem : select(aJCas, Stem.class)) {
                Token t = selectSingleAt(aJCas, Token.class, stem.getBegin(), stem.getEnd());
                assert t.getStem() == stem;
            }

            for (MorphologicalFeatures morph : select(aJCas, MorphologicalFeatures.class)) {
                Token t = selectSingleAt(aJCas, Token.class, morph.getBegin(), morph.getEnd());
                assert t.getMorph() == morph;
            }

            for (POS pos : select(aJCas, POS.class)) {
                Token t = selectSingleAt(aJCas, Token.class, pos.getBegin(), pos.getEnd());
                assert t.getPos() == pos;
            }

            for (Dependency dep : select(aJCas, Dependency.class)) {
                assert dep.getBegin() == dep.getDependent().getBegin();
                assert dep.getEnd() == dep.getDependent().getEnd();
            }
        }
    }

}
