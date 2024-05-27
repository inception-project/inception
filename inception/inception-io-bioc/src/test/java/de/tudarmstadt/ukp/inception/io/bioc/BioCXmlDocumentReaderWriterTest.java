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
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.xmlunit.assertj3.XmlAssert.assertThat;
import static org.xmlunit.builder.Input.fromFile;

import java.io.File;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.dkpro.core.testing.IOTestRunner;
import org.dkpro.core.testing.TestOptions;
import org.junit.jupiter.api.Test;

/**
 * @deprecated Experimental code that was deprecated.
 */
@Deprecated
public class BioCXmlDocumentReaderWriterTest
{
    @Test
    public void testOneWay() throws Exception
    {
        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var basicSpanType = tsd.addType(BASIC_SPAN_LAYER_NAME, null, TYPE_NAME_ANNOTATION);
        basicSpanType.addFeature(BASIC_SPAN_LABEL_FEATURE_NAME, null, TYPE_NAME_STRING);

        var basicRelationType = tsd.addType(BASIC_RELATION_LAYER_NAME, null, TYPE_NAME_ANNOTATION);
        basicRelationType.addFeature(FEAT_REL_SOURCE, null, CAS.TYPE_NAME_ANNOTATION);
        basicRelationType.addFeature(FEAT_REL_TARGET, null, CAS.TYPE_NAME_ANNOTATION);
        basicRelationType.addFeature(BASIC_RELATION_LABEL_FEATURE_NAME, null, TYPE_NAME_STRING);

        var fullTsd = mergeTypeSystems(asList(createTypeSystemDescription(), tsd));

        IOTestRunner.testOneWay( //
                createReaderDescription( //
                        BioCXmlDocumentReader.class, fullTsd), //
                createEngineDescription( //
                        BioCXmlDocumentWriter.class, fullTsd, //
                        BioCXmlDocumentWriter.PARAM_INDENT, true),
                "bioc/example-with-sentences-ref.xml", //
                "bioc/example-with-sentences.xml", //
                new TestOptions().resultAssertor(this::assertXmlEquals));
    }

    private void assertXmlEquals(File expected, File actual)
    {
        assertThat(fromFile(expected.getPath())) //
                .and(fromFile(actual.getPath())) //
                .areSimilar();
    }
}
