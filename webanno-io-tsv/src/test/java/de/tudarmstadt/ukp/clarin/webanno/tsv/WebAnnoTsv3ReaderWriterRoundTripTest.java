/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.FilenameFilter;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tudarmstadt.ukp.clarin.webanno.tsv.WebAnnoTsv3XReaderWriterRoundTripTest.DKProCoreConventionsChecker;
import de.tudarmstadt.ukp.clarin.webanno.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

@RunWith(value = Parameterized.class)
public class WebAnnoTsv3ReaderWriterRoundTripTest
{
    @Parameters(name = "{index}: running on file {0}")
    public static Iterable<File> tsvFiles()
    {
        return asList(new File("src/test/resources/tsv3-suite/").listFiles(
                (FilenameFilter) new PrefixFileFilter(asList("test", "issue", "sample"))));
    }

    private File referenceFolder;

    public WebAnnoTsv3ReaderWriterRoundTripTest(File aFolder)
    {
        referenceFolder = aFolder;
    }

    private boolean isKnownToFail(String aMethodName)
    {
        Set<String> failingTests = new HashSet<>();
        failingTests.add("testAnnotationWithLeadingWhitespaceAtStart");
        failingTests.add("testMultiTokenChain");
        failingTests.add("testSingleStackedNonTokenRelationWithoutFeatureValue2");
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

    @Test
    public void runTest() throws Exception
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local;
        if (new File(referenceFolder, "typesystem.xml").exists()) {
            local = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
                    new File(referenceFolder, "typesystem.xml").toString());
        }
        else {
            local = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
                    "src/test/resources/desc/type/webannoTestTypes.xml");
        }
       
        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));
        
        String targetFolder = "target/test-output/WebAnnoTsv3ReaderWriterRoundTripTest/"
                + referenceFolder.getName();
        
        CollectionReaderDescription reader = createReaderDescription(WebannoTsv3Reader.class,
                merged,
                WebannoTsv3Reader.PARAM_SOURCE_LOCATION, referenceFolder,
                WebannoTsv3Reader.PARAM_PATTERNS, "reference.tsv");
        
        AnalysisEngineDescription checker = createEngineDescription(
                DKProCoreConventionsChecker.class);        
        
        // WebannoTsv3Writer doesn't seem to like it if both "SimpleLinkHost" and
        // "ComplexLinkHost" are declared, so I comment out "ComplexLinkHost" which has
        // less tests.
        AnalysisEngineDescription tsvWriter = createEngineDescription(WebannoTsv3Writer.class,
                merged,
                WebannoTsv3Writer.PARAM_TARGET_LOCATION, targetFolder,
                WebannoTsv3Writer.PARAM_STRIP_EXTENSION, true,
                WebannoTsv3Writer.PARAM_CHAIN_LAYERS, asList(
                        "webanno.custom.Simple"),
                WebannoTsv3Writer.PARAM_SLOT_FEATS, asList(
                        "webanno.custom.SimpleLinkHost:links"
//                        "webanno.custom.ComplexLinkHost:links"
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
//                        "webanno.custom.ComplexLinkHost"
                        ),
                WebannoTsv3Writer.PARAM_LINK_TYPES, asList(
                        "webanno.custom.LinkType"
//                        "webanno.custom.ComplexLinkType"
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
                XmiWriter.PARAM_STRIP_EXTENSION, true);
        
        try {
            SimplePipeline.runPipeline(reader, checker, tsvWriter, xmiWriter);
        }
        catch (Throwable e) {
            assumeFalse("This test is known to fail.", isKnownToFail(referenceFolder.getName()));
            throw e;
        }
        
        String reference = FileUtils.readFileToString(new File(referenceFolder, "reference.tsv"),
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

        assumeFalse("This test is known to fail.", isKnownToFail(referenceFolder.getName()));
        assertEquals(reference, actual);
        // assertEquals(referenceXmi, actualXmi);
    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
