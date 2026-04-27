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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCasFromPath;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;

@ExtendWith(MockitoExtension.class)
public class MultiValueConceptFeatureSupportTest
{
    private @Mock KnowledgeBaseService kbService;

    private MultiValueConceptFeatureSupport sut;

    private JCas jcas;
    private AnnotationFS spanFS;
    private AnnotationFeature valueFeature;

    @BeforeEach
    public void setUp() throws Exception
    {
        sut = new MultiValueConceptFeatureSupport(
                new ConceptLabelCache(kbService, new KnowledgeBasePropertiesImpl()));

        valueFeature = new AnnotationFeature("values",
                MultiValueConceptFeatureSupport.TYPE_ANY_OBJECT);
        valueFeature.setMode(MultiValueMode.ARRAY);
        valueFeature.setLinkMode(LinkMode.NONE);

        jcas = createJCasFromPath("src/test/resources/desc/type/webannoTestTypes.xml");
        jcas.setDocumentText("label");

        var spanType = jcas.getCas().getTypeSystem().getType("webanno.custom.SpanMultiValue");
        spanFS = jcas.getCas().createAnnotation(spanType, 0, jcas.getDocumentText().length());
    }

    @Test
    public void thatClearFeatureValueDoesNotThrowOnMultiValuedConceptFeature() throws Exception
    {
        FSUtil.setFeature(spanFS, valueFeature.getName(), asList("kb1:id1", "kb1:id2"));

        sut.clearFeatureValue(valueFeature, spanFS);

        assertThat(FSUtil.getFeature(spanFS, valueFeature.getName(), String[].class)) //
                .isNull();
    }
}
