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
package de.tudarmstadt.ukp.inception.annotation.feature.string;

import static org.apache.uima.fit.factory.JCasFactory.createJCasFromPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

@ExtendWith(MockitoExtension.class)
public class StringFeatureSupportTest
{
    private @Mock AnnotationSchemaService schemaService;

    private StringFeatureSupport sut;

    private JCas jcas;
    private AnnotationFS spanFS;
    private AnnotationFeature valueFeature;
    private TagSet valueTagset;

    @BeforeEach
    public void setUp() throws Exception
    {
        sut = new StringFeatureSupport(new StringFeatureSupportPropertiesImpl(), schemaService);

        valueFeature = new AnnotationFeature("value", CAS.TYPE_NAME_STRING);

        valueTagset = new TagSet();

        jcas = createJCasFromPath("src/test/resources/desc/type/webannoTestTypes.xml");
        jcas.setDocumentText("label");

        Type spanType = jcas.getCas().getTypeSystem().getType("webanno.custom.Span");
        spanFS = jcas.getCas().createAnnotation(spanType, 0, jcas.getDocumentText().length());
    }

    @Test
    public void thatCreationOfMissingFeaturesIsAttempted() throws Exception
    {
        final String value = "someTag";

        valueFeature.setTagset(valueTagset);

        sut.setFeatureValue(jcas.getCas(), valueFeature, ICasUtil.getAddr(spanFS), value);

        verify(schemaService).createMissingTag(valueFeature, value);
    }

    @Test
    public void thatFeatureValueIsSet() throws Exception
    {
        final String value = "value";

        valueFeature.setTagset(valueTagset);
        valueTagset.setCreateTag(true);

        sut.setFeatureValue(jcas.getCas(), valueFeature, ICasUtil.getAddr(spanFS), value);

        assertThat(FSUtil.getFeature(spanFS, valueFeature.getName(), String.class))
                .isEqualTo(value);

        assertThat((String) sut.getFeatureValue(valueFeature, spanFS)) //
                .isEqualTo(value);
    }
}
