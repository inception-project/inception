/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;

public class ConceptFeatureSupportTest
{
    private @Mock KnowledgeBaseService kbService;
    
    @Before
    public void setUp()
    {
        initMocks(this);
    }
    
    @Test
    public void testAccepts()
    {
        ConceptFeatureSupport sut = new ConceptFeatureSupport(kbService);
        
        AnnotationFeature feat1 = new AnnotationFeature("Dummy feature",
                ConceptFeatureSupport.PREFIX + "someConcept");
        
        AnnotationFeature feat2 = new AnnotationFeature("Dummy feature", "someConcept");
        
        assertThat(sut.accepts(feat1)).isTrue();
        assertThat(sut.accepts(feat2)).isFalse();
    }
    
    @Test
    public void testWrapUnwrap() throws Exception
    {
        ConceptFeatureSupport sut = new ConceptFeatureSupport(kbService);
        
        AnnotationFeature feat1 = new AnnotationFeature("Dummy feature",
                ConceptFeatureSupport.PREFIX + "someConcept");
        
        KBHandle referenceHandle = new KBHandle("id", "name");
        
        when(kbService.readItem((Project) any(), anyString()))
                .thenReturn(Optional.of(new KBInstance("id", "name")));
        
        when(kbService.readItem((Project) any(), anyString()))
                .thenReturn(Optional.of(new KBInstance("id", "name")));
        
        
        assertThat(sut.wrapFeatureValue(feat1, null, "id"))
                .isEqualToComparingFieldByField(referenceHandle);
        assertThat(sut.wrapFeatureValue(feat1, null, referenceHandle)).isSameAs(referenceHandle);
        assertThat(sut.wrapFeatureValue(feat1, null, null)).isNull();
        assertThatThrownBy(() -> sut.wrapFeatureValue(feat1, null, new Object()))
                .isInstanceOf(IllegalArgumentException.class);
        
        assertThat(sut.unwrapFeatureValue(feat1, null, referenceHandle)).isEqualTo("id");
        assertThat(sut.unwrapFeatureValue(feat1, null, "id")).isEqualTo("id");
        assertThat(sut.unwrapFeatureValue(feat1, null, null)).isNull();;
        assertThatThrownBy(() -> sut.unwrapFeatureValue(feat1, null, new Object()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
