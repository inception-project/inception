/*
 * Copyright 2017
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
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectSingleAt;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FilenameFilter;

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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

@RunWith(value = Parameterized.class)
public class WebAnnoTsv3XReaderWriterRoundTripTest
{
    @Parameters(name = "{index}: running on file {0}")
    public static Iterable<File> tsvFiles()
    {
        return asList(new File("src/test/resources/tsv3-suite/").listFiles(
                (FilenameFilter) new PrefixFileFilter(asList("test", "issue", "sample"))));
    }

    private File referenceFolder;

    public WebAnnoTsv3XReaderWriterRoundTripTest(File aFolder)
    {
        referenceFolder = aFolder;
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
        
        String targetFolder = "target/test-output/WebAnnoTsv3XReaderWriterRoundTripTest/"
                + referenceFolder.getName();
        
        CollectionReaderDescription reader = createReaderDescription(WebannoTsv3XReader.class,
                merged,
                WebannoTsv3XReader.PARAM_SOURCE_LOCATION, referenceFolder,
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

        SimplePipeline.runPipeline(reader, checker, tsvWriter, xmiWriter);
        
        String referenceTsv = FileUtils.readFileToString(new File(referenceFolder, "reference.tsv"),
                "UTF-8");

        String actualTsv = FileUtils.readFileToString(new File(targetFolder, "reference.tsv"),
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

        assertEquals(referenceTsv, actualTsv);
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
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
