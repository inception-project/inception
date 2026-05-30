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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;

import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
public class KbLabelCachePrewarmStepTest
{
    private @Mock KnowledgeBaseService kbService;
    private @Mock AnnotationSchemaService schemaService;

    private ConceptLabelCache labelCache;
    private ConceptFeatureSupport conceptFeatureSupport;
    private MultiValueConceptFeatureSupport multiValueConceptFeatureSupport;
    private KbLabelCachePrewarmStep sut;

    private Project project;
    private JCas jcas;
    private AnnotationLayer singleValueLayer;
    private AnnotationLayer multiValueLayer;
    private AnnotationFeature singleValueFeature;
    private AnnotationFeature multiValueFeature;

    @BeforeEach
    public void setUp() throws Exception
    {
        labelCache = new ConceptLabelCache(kbService, new KnowledgeBasePropertiesImpl());
        conceptFeatureSupport = new ConceptFeatureSupport(labelCache);
        multiValueConceptFeatureSupport = new MultiValueConceptFeatureSupport(labelCache);

        sut = new KbLabelCachePrewarmStep(labelCache, conceptFeatureSupport,
                multiValueConceptFeatureSupport, schemaService);

        project = Project.builder() //
                .withId(1L) //
                .withName("test-project") //
                .build();

        jcas = createJCasFromPath("src/test/resources/desc/type/webannoTestTypes.xml");
        jcas.setDocumentText("0123456789");

        singleValueLayer = AnnotationLayer.builder() //
                .withName("webanno.custom.Span") //
                .withProject(project) //
                .build();

        multiValueLayer = AnnotationLayer.builder() //
                .withName("webanno.custom.SpanMultiValue") //
                .withProject(project) //
                .build();

        singleValueFeature = AnnotationFeature.builder() //
                .withName("value") //
                .withType(ConceptFeatureSupport.TYPE_ANY_OBJECT) //
                .withMultiValueMode(MultiValueMode.NONE) //
                .withLinkMode(LinkMode.NONE) //
                .withLayer(singleValueLayer) //
                .withProject(project) //
                .build();

        multiValueFeature = AnnotationFeature.builder() //
                .withName("values") //
                .withType(MultiValueConceptFeatureSupport.TYPE_ANY_OBJECT) //
                .withMultiValueMode(MultiValueMode.ARRAY) //
                .withLinkMode(LinkMode.NONE) //
                .withLayer(multiValueLayer) //
                .withProject(project) //
                .build();
    }

    @Test
    public void render_warmsSingleValueConceptIds() throws Exception
    {
        addSpan(jcas, "webanno.custom.Span", 0, 5, "value", "kb:id1");
        addSpan(jcas, "webanno.custom.Span", 5, 10, "value", "kb:id2");

        when(schemaService.listSupportedFeatures(singleValueLayer))
                .thenReturn(asList(singleValueFeature));
        when(kbService.readHandles(eq(project), any())) //
                .thenReturn(Map.of( //
                        "kb:id1", new KBHandle("kb:id1", "Alpha"), //
                        "kb:id2", new KBHandle("kb:id2", "Beta")));

        sut.render(new VDocument(), renderRequest(singleValueLayer));

        verify(kbService, times(1)).readHandles(eq(project), any());
        verify(kbService, never()).readHandle(any(Project.class), anyString());

        // After warming, get(...) must hit the cache and not trigger another DB call.
        labelCache.get(singleValueFeature, null, "kb:id1");
        labelCache.get(singleValueFeature, null, "kb:id2");
        verify(kbService, times(1)).readHandles(eq(project), any());
        verify(kbService, never()).readHandle(any(Project.class), anyString());
    }

    @Test
    public void render_warmsMultiValueConceptIds() throws Exception
    {
        var span = addSpan(jcas, "webanno.custom.SpanMultiValue", 0, 5, null, null);
        FSUtil.setFeature(span, "values", asList("kb:a", "kb:b", "kb:c"));

        when(schemaService.listSupportedFeatures(multiValueLayer))
                .thenReturn(asList(multiValueFeature));
        when(kbService.readHandles(eq(project), any())) //
                .thenReturn(Map.of( //
                        "kb:a", new KBHandle("kb:a", "A"), //
                        "kb:b", new KBHandle("kb:b", "B"), //
                        "kb:c", new KBHandle("kb:c", "C")));

        sut.render(new VDocument(), renderRequest(multiValueLayer));

        verify(kbService, times(1)).readHandles(eq(project), any());
    }

    @Test
    public void render_skipsAnnotationsOutsideWindow() throws Exception
    {
        // Replace the default 10-char doc text with a 60-char one so we can place an annotation
        // outside the rendering window.
        jcas.reset();
        jcas.setDocumentText("0123456789012345678901234567890123456789012345678901234567890");

        addSpan(jcas, "webanno.custom.Span", 0, 5, "value", "kb:in-window");
        addSpan(jcas, "webanno.custom.Span", 50, 55, "value", "kb:outside");

        when(schemaService.listSupportedFeatures(singleValueLayer))
                .thenReturn(asList(singleValueFeature));
        when(kbService.readHandles(eq(project), any())) //
                .thenReturn(Map.of("kb:in-window", new KBHandle("kb:in-window", "In")));

        var request = RenderRequest.builder() //
                .withCas(jcas.getCas()) //
                .withWindow(0, 10) //
                .withAllLayers(asList(singleValueLayer)) //
                .withVisibleLayers(asList(singleValueLayer)) //
                .build();

        sut.render(new VDocument(), request);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        var captor = org.mockito.ArgumentCaptor
                .forClass((Class<Collection<String>>) (Class) Collection.class);
        verify(kbService, times(1)).readHandles(eq(project), captor.capture());
        assertThat(captor.getValue()).containsExactly("kb:in-window");
    }

    @Test
    public void render_skipsHiddenFeatures() throws Exception
    {
        singleValueFeature.setId(42L);
        addSpan(jcas, "webanno.custom.Span", 0, 5, "value", "kb:id1");

        when(schemaService.listSupportedFeatures(singleValueLayer))
                .thenReturn(asList(singleValueFeature));

        var request = RenderRequest.builder() //
                .withCas(jcas.getCas()) //
                .withWindow(0, 10) //
                .withAllLayers(asList(singleValueLayer)) //
                .withVisibleLayers(asList(singleValueLayer)) //
                .withHiddenFeatures(asList(42L)) //
                .build();

        sut.render(new VDocument(), request);

        verify(kbService, never()).readHandles(any(Project.class), any());
    }

    @Test
    public void render_doesNothingWhenNoConceptFeaturesPresent() throws Exception
    {
        addSpan(jcas, "webanno.custom.Span", 0, 5, "value", "kb:id1");

        // Layer reports no features at all.
        when(schemaService.listSupportedFeatures(singleValueLayer)).thenReturn(java.util.List.of());

        sut.render(new VDocument(), renderRequest(singleValueLayer));

        verify(kbService, never()).readHandles(any(Project.class), any());
    }

    @Test
    public void render_doesNothingWhenAllIdentifiersAreBlank() throws Exception
    {
        addSpan(jcas, "webanno.custom.Span", 0, 5, "value", "");
        addSpan(jcas, "webanno.custom.Span", 5, 10, "value", null);

        when(schemaService.listSupportedFeatures(singleValueLayer))
                .thenReturn(asList(singleValueFeature));

        sut.render(new VDocument(), renderRequest(singleValueLayer));

        verify(kbService, never()).readHandles(any(Project.class), any());
    }

    private org.apache.uima.cas.text.AnnotationFS addSpan(JCas aJCas, String aType, int aBegin,
            int aEnd, String aFeatureName, String aValue)
    {
        var type = aJCas.getCas().getTypeSystem().getType(aType);
        var fs = aJCas.getCas().createAnnotation(type, aBegin, aEnd);
        if (aFeatureName != null && aValue != null) {
            FSUtil.setFeature(fs, aFeatureName, aValue);
        }
        aJCas.getCas().addFsToIndexes(fs);
        return fs;
    }

    private RenderRequest renderRequest(AnnotationLayer aLayer)
    {
        return RenderRequest.builder() //
                .withCas(jcas.getCas()) //
                .withWindow(0, jcas.getDocumentText().length()) //
                .withAllLayers(asList(aLayer)) //
                .withVisibleLayers(asList(aLayer)) //
                .build();
    }
}
