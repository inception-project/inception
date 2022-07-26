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
package de.tudarmstadt.ukp.inception.annotation.feature.multistring;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCasFromPath;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalFeatureValueException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupportPropertiesImpl;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
public class MultiValueStringFeatureSupportTest
{
    private @Mock AnnotationSchemaService schemaService;

    private MultiValueStringFeatureSupport sut;

    private JCas jcas;
    private AnnotationFS spanFS;
    private AnnotationFeature valueFeature;
    private TagSet valueTagset;

    @BeforeEach
    public void setUp() throws Exception
    {
        sut = new MultiValueStringFeatureSupport(new StringFeatureSupportPropertiesImpl(),
                schemaService);

        valueFeature = new AnnotationFeature("values", CAS.TYPE_NAME_STRING_ARRAY);
        valueFeature.setMode(MultiValueMode.ARRAY);

        valueTagset = new TagSet();

        jcas = createJCasFromPath("src/test/resources/desc/type/webannoTestTypes.xml");
        jcas.setDocumentText("label");

        Type spanType = jcas.getCas().getTypeSystem().getType("webanno.custom.SpanMultiValue");
        spanFS = jcas.getCas().createAnnotation(spanType, 0, jcas.getDocumentText().length());
    }

    @Test
    public void thatUsingOutOfTagsetValueInClosedTagsetProducesException() throws Exception
    {
        final List<String> values = asList("TAG-NOT-IN-LIST");

        valueFeature.setTagset(valueTagset);
        valueTagset.setCreateTag(false);

        when(schemaService.existsTag(values.get(0), valueTagset)).thenReturn(false);

        assertThatExceptionOfType(IllegalFeatureValueException.class) //
                .isThrownBy(() -> sut.setFeatureValue(jcas.getCas(), valueFeature,
                        ICasUtil.getAddr(spanFS), values))
                .withMessageContaining("is not in the tag list");
    }

    @Test
    public void thatUsingOutOfTagsetValueInOpenTagsetAddsNewValue() throws Exception
    {
        final List<String> values = asList("TAG-NOT-IN-LIST");

        valueFeature.setTagset(valueTagset);
        valueTagset.setCreateTag(true);

        when(schemaService.existsTag(values.get(0), valueTagset)).thenReturn(false);

        sut.setFeatureValue(jcas.getCas(), valueFeature, ICasUtil.getAddr(spanFS), values);

        verify(schemaService).createTag(new Tag(valueTagset, values.get(0)));
    }
}
