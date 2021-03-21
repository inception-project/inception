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
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.dkpro.core.io.xmi.XmiWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.clarin.webanno.tsv.WebAnnoTsv3XReaderWriterRoundTripTest.DKProCoreConventionsChecker;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * @deprecated These test were used during the development of the new {@link WebannoTsv3XReader} and
 *             {@link WebannoTsv3XWriter} code to check that the two are reasonably compatible. Now
 *             that the new code is done, these old tests are not really needed anymore.
 */
@Deprecated
@Disabled("Ignoring because these produce WebAnno TSV 3.2 headers and we are now at TSV 3.3")
public class WebAnnoTsv3ReaderWriterRoundTripTest
{
    public static Iterable<File> tsvFiles()
    {
        return asList(new File("src/test/resources/tsv3-suite/").listFiles(
                (FilenameFilter) new PrefixFileFilter(asList("test", "issue", "sample"))));
    }

    @BeforeEach
    public void testWatcher(TestInfo aTestInfo)
    {
        String methodName = aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
        System.out.printf("\n=== %s === %s=====================\n", methodName,
                aTestInfo.getDisplayName());
    }

    private boolean isKnownToFail(String aMethodName)
    {
        Set<String> failingTests = new HashSet<>();
        failingTests.add("testAnnotationWithLeadingWhitespaceAtStart");
        failingTests.add("testMultiTokenChain");
        failingTests.add("testSingleStackedNonTokenRelationWithoutFeatureValue2");
        failingTests.add("testSingleStackedNonTokenOverlappingRelationWithoutFeatureValue");
        failingTests.add("testSubtokenChain");
        failingTests.add("testStackedSubMultiTokenSpanWithFeatureValue");
        failingTests.add("testSubMultiTokenSpanWithFeatureValue");
        failingTests.add("testSubMultiTokenSpanWithoutFeatureValue");
        failingTests.add("testSubMultiTokenSpanWithoutFeatureValue2");
        failingTests.add("testSubMultiTokenSpanWithoutFeatureValue3");
        failingTests.add("testComplexSlotFeatureWithoutValues");
        failingTests.add("testStackedComplexSlotFeatureWithoutSlotFillers");
        failingTests.add("testStackedComplexSlotFeatureWithoutValues");
        failingTests.add("testStackedSimpleSlotFeatureWithoutValues");
        failingTests.add("testZeroLengthSlotFeature2");
        failingTests.add("testZeroLengthSpanBetweenAdjacentTokens");
        failingTests.add("sampleSlotAnnotation1");
        failingTests.add("sampleSlotAnnotation2");
        failingTests.add("testUnsetSlotFeature");
        failingTests.add("testZeroWidthAnnotationBeforeFirstTokenIsMovedToBeginOfFirstToken");
        failingTests.add("testZeroWidthAnnotationBetweenTokenIsMovedToEndOfPreviousToken");

        return failingTests.contains(aMethodName);
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

        String targetFolder = "target/test-output/WebAnnoTsv3ReaderWriterRoundTripTest/"
                + aReferenceFolder.getName();

        // @formatter:off
        CollectionReaderDescription reader = createReaderDescription(WebannoTsv3Reader.class,
                merged,
                WebannoTsv3Reader.PARAM_SOURCE_LOCATION, aReferenceFolder,
                WebannoTsv3Reader.PARAM_PATTERNS, "reference.tsv");

        AnalysisEngineDescription checker = createEngineDescription(
                DKProCoreConventionsChecker.class);
        // @formatter:on

        // WebannoTsv3Writer doesn't seem to like it if both "SimpleLinkHost" and
        // "ComplexLinkHost" are declared, so I comment out "ComplexLinkHost" which has
        // less tests.
        // @formatter:off
        AnalysisEngineDescription tsvWriter = createEngineDescription(WebannoTsv3Writer.class,
                merged,
                WebannoTsv3Writer.PARAM_TARGET_LOCATION, targetFolder,
                WebannoTsv3Writer.PARAM_STRIP_EXTENSION, true,
                WebannoTsv3Writer.PARAM_OVERWRITE, true,
                WebannoTsv3Writer.PARAM_CHAIN_LAYERS, asList(
                        "webanno.custom.Simple"),
                WebannoTsv3Writer.PARAM_SLOT_FEATS, asList(
                        "webanno.custom.SimpleLinkHost:links"
                // "webanno.custom.ComplexLinkHost:links"
                        ),
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(
                        NamedEntity.class.getName(), 
                        MorphologicalFeatures.class.getName(), 
                        POS.class.getName(), 
                        Lemma.class.getName(), 
                        Stem.class.getName(), 
                        "webanno.custom.Span", 
                        "webanno.custom.SimpleSpan", 
                        "webanno.custom.SimpleLinkHost"
                // "webanno.custom.ComplexLinkHost"
                        ),
                WebannoTsv3Writer.PARAM_LINK_TYPES, asList(
                        "webanno.custom.LinkType"
                // "webanno.custom.ComplexLinkType"
                        ),
                WebannoTsv3Writer.PARAM_SLOT_TARGETS, asList(
                        "webanno.custom.SimpleSpan"),
                WebannoTsv3Writer.PARAM_RELATION_LAYERS, asList(
                        "webanno.custom.SimpleRelation", 
                        "webanno.custom.Relation",
                        "webanno.custom.ComplexRelation", 
                        Dependency.class.getName())
                );

        AnalysisEngineDescription xmiWriter = createEngineDescription(XmiWriter.class,
                merged,
                XmiWriter.PARAM_TARGET_LOCATION, targetFolder,
                XmiWriter.PARAM_STRIP_EXTENSION, true,
                XmiWriter.PARAM_OVERWRITE, true);
        // @formatter:on

        try {
            SimplePipeline.runPipeline(reader, checker, tsvWriter, xmiWriter);
        }
        catch (Throwable e) {
            assumeFalse(isKnownToFail(aReferenceFolder.getName()), "This test is known to fail.");
            throw e;
        }

        String reference = FileUtils.readFileToString(new File(aReferenceFolder, "reference.tsv"),
                "UTF-8");

        String actual = FileUtils.readFileToString(new File(targetFolder, "reference.tsv"),
                "UTF-8");

        //
        // The XMI files here are not compared semantically but using their serialization which
        // is subject to minor variations depending e.g. on the order in which annotation are
        // created in the CAS. Thus, this code is commented out and should only be used on a
        // case-by-case base to compare XMIs during development.
        //
        // String referenceXmi = FileUtils.readFileToString(new File(referenceFolder,
        // "reference.xmi"),
        // "UTF-8");
        //
        // String actualXmi = FileUtils.readFileToString(new File(targetFolder, "reference.xmi"),
        // "UTF-8");

        assumeFalse(isKnownToFail(aReferenceFolder.getName()), "This test is known to fail.");
        assertThat(reference).isEqualToNormalizingNewlines(actual);
        // assertEquals(referenceXmi, actualXmi);
    }

}
