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
package de.tudarmstadt.ukp.inception.io.bioc;

import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.dkpro.core.io.xmi.XmiWriter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BioCReaderWriterTest
{
    private static final String TYPESYSTEM_XML = "typesystem.xml";
    private static final String REFERENCE_XML = "reference.xml";
    private static final String DATA_XML = "data.xml";

    public static Iterable<File> testSuiteFiles()
    {
        return asList(new File("src/test/resources/bioc-suite/").listFiles(File::isDirectory));
    }

    @ParameterizedTest(name = "{index}: running on file {0}")
    @MethodSource("testSuiteFiles")
    public void runTest(File aReferenceFolder) throws Exception
    {
        TypeSystemDescription merged = createTestTypeSystem(aReferenceFolder);

        String targetFolder = "target/test-output/bioc-suite/" + aReferenceFolder.getName();

        CollectionReaderDescription reader = createReaderDescription( //
                BioCReader.class, merged, BioCReader.PARAM_SOURCE_LOCATION, aReferenceFolder, //
                BioCReader.PARAM_PATTERNS, DATA_XML);

        AnalysisEngineDescription writer = createEngineDescription( //
                BioCWriter.class, merged, //
                BioCWriter.PARAM_TARGET_LOCATION, targetFolder, //
                BioCWriter.PARAM_STRIP_EXTENSION, true, //
                BioCWriter.PARAM_OVERWRITE, true);

        AnalysisEngineDescription xmiWriter = createEngineDescription( //
                XmiWriter.class, merged, //
                XmiWriter.PARAM_TARGET_LOCATION, targetFolder, //
                XmiWriter.PARAM_STRIP_EXTENSION, true, //
                XmiWriter.PARAM_OVERWRITE, true);

        SimplePipeline.runPipeline(reader, writer, xmiWriter);

        var reference = contentOf(new File(aReferenceFolder, REFERENCE_XML), UTF_8).trim();

        var actual = contentOf(new File(targetFolder, DATA_XML), UTF_8).trim();

        assertThat(actual).isEqualToNormalizingNewlines(reference);
    }

    private TypeSystemDescription createTestTypeSystem(File aReferenceFolder)
        throws ResourceInitializationException
    {
        TypeSystemDescription global = createTypeSystemDescription();

        TypeSystemDescription local;
        if (new File(aReferenceFolder, TYPESYSTEM_XML).exists()) {
            local = createTypeSystemDescriptionFromPath(
                    new File(aReferenceFolder, TYPESYSTEM_XML).toString());
        }
        else {
            local = createTestTypeSystem();
        }

        return mergeTypeSystems(asList(global, local));
    }

    private TypeSystemDescription createTestTypeSystem() throws ResourceInitializationException
    {
        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var basicSpanType = tsd.addType(BASIC_SPAN_LAYER_NAME, null, TYPE_NAME_ANNOTATION);
        basicSpanType.addFeature(BASIC_SPAN_LABEL_FEATURE_NAME, null, TYPE_NAME_STRING);

        var basicRelationType = tsd.addType(BASIC_RELATION_LAYER_NAME, null, TYPE_NAME_ANNOTATION);
        basicRelationType.addFeature(FEAT_REL_SOURCE, null, CAS.TYPE_NAME_ANNOTATION);
        basicRelationType.addFeature(FEAT_REL_TARGET, null, CAS.TYPE_NAME_ANNOTATION);
        basicRelationType.addFeature(BASIC_RELATION_LABEL_FEATURE_NAME, null, TYPE_NAME_STRING);

        return mergeTypeSystems(asList(createTypeSystemDescription(), tsd));
    }
}
