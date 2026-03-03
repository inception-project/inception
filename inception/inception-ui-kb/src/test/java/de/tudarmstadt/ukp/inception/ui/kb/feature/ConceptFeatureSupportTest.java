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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

@ExtendWith(MockitoExtension.class)
public class ConceptFeatureSupportTest
{
    private @Mock KnowledgeBaseService kbService;
    private ConceptFeatureSupport sut;

    @BeforeEach
    public void setUp()
    {
        sut = new ConceptFeatureSupport(
                new ConceptLabelCache(kbService, new KnowledgeBasePropertiesImpl()));
    }

    @Test
    public void testAccepts()
    {
        AnnotationFeature feat1 = new AnnotationFeature("Dummy feature",
                ConceptFeatureSupport.PREFIX + "someConcept");

        AnnotationFeature feat2 = new AnnotationFeature("Dummy feature", "someConcept");

        assertThat(sut.accepts(feat1)).isTrue();
        assertThat(sut.accepts(feat2)).isFalse();
    }

    @Test
    public void testWrapUnwrap() throws Exception
    {
        AnnotationFeature feat1 = new AnnotationFeature("Dummy feature",
                ConceptFeatureSupport.PREFIX + "someConcept");

        KBHandle referenceHandle = new KBHandle("id", "name");

        when(kbService.readHandle((Project) any(), anyString()))
                .thenReturn(Optional.of(new KBHandle("id", "name")));

        when(kbService.readHandle((Project) any(), anyString()))
                .thenReturn(Optional.of(new KBHandle("id", "name")));

        assertThat(sut.wrapFeatureValue(feat1, null, "id")).usingRecursiveComparison()
                .isEqualTo(referenceHandle);
        assertThat(sut.wrapFeatureValue(feat1, null, referenceHandle)).isSameAs(referenceHandle);
        assertThat(sut.wrapFeatureValue(feat1, null, null)).isNull();
        assertThatThrownBy(() -> sut.wrapFeatureValue(feat1, null, new Object()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(sut.unwrapFeatureValue(feat1, referenceHandle)).isEqualTo("id");
        assertThat(sut.unwrapFeatureValue(feat1, "id")).isEqualTo("id");
        assertThat(sut.unwrapFeatureValue(feat1, null)).isNull();
        ;
        assertThatThrownBy(() -> sut.unwrapFeatureValue(feat1, new Object()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
