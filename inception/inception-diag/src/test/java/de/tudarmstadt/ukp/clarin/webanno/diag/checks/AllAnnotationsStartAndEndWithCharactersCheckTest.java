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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@ExtendWith(SpringExtension.class)
class AllAnnotationsStartAndEndWithCharactersCheckTest
{
    @Configuration
    @Import({ AnnotationSchemaService.class, AllAnnotationsStartAndEndWithCharactersCheck.class })
    static class Config
    {
    }

    @MockitoBean
    AnnotationSchemaService annotationService;

    @Autowired
    AllAnnotationsStartAndEndWithCharactersCheck sut;

    Project project;
    SourceDocument document;
    String dataOwner;
    JCas jCas;
    List<AnnotationLayer> layers;

    @BeforeEach
    void setup() throws Exception
    {
        project = Project.builder().build();
        document = SourceDocument.builder() //
                .withProject(project) //
                .build();
        jCas = JCasFactory.createJCas();

        var namedEntityLayer = new AnnotationLayer();
        namedEntityLayer.setName(NamedEntity._TypeName);
        layers = asList(namedEntityLayer);
    }

    @Test
    void test()
    {
        when(annotationService.listAnnotationLayer(project)).thenReturn(layers);

        jCas.setDocumentText(join("a", "\u00A0", "b", " ", "c", " ", "d"));

        var annotations = asList( //
                new Sentence(jCas, 0, 10), //
                new NamedEntity(jCas, 0, 2));
        annotations.forEach(Annotation::addToIndexes);

        var messages = new ArrayList<LogMessage>();

        var result = sut.check(document, dataOwner, jCas.getCas(), messages);

        assertThat(result).isFalse();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getMessage()).contains(
                "[de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity] [\\u00A0a]@[0-2] starts with whitespace");

    }
}
