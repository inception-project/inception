/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanCrossSentenceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanStackingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VCommentType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public class SpanRendererTest
{
    private FeatureSupportRegistry featureSupportRegistry;
    private Project project;
    private AnnotationLayer neLayer;
    private JCas jcas;

    @Before
    public void setup() throws Exception
    {
        if (jcas == null) {
            jcas = JCasFactory.createJCas();
        }
        else {
            jcas.reset();
        }
        
        project = new Project();
        project.setId(1l);
        project.setMode(PROJECT_TYPE_ANNOTATION);
        
        neLayer = new AnnotationLayer(NamedEntity.class.getName(), "NE", SPAN_TYPE, project, true,
                TOKENS);
        neLayer.setId(1l);

        featureSupportRegistry = new FeatureSupportRegistryImpl(asList());
    }
    
    @Test
    public void thatIllegalCrossSentenceSpansGenerateError()
    {
        neLayer.setCrossSentence(false);
        
        jcas.setDocumentText(StringUtils.repeat("a", 20));
        
        new Sentence(jcas, 0, 10).addToIndexes();
        new Sentence(jcas, 10, 20).addToIndexes();
        NamedEntity ne = new NamedEntity(jcas, 5, 15);
        ne.addToIndexes();
        
        SpanAdapter adapter = new SpanAdapter(featureSupportRegistry, null, neLayer, asList(),
                asList(new SpanCrossSentenceBehavior()));
        
        SpanRenderer sut = new SpanRenderer(adapter, featureSupportRegistry,
                asList(new SpanCrossSentenceBehavior()));
        
        VDocument vdoc = new VDocument();
        sut.render(jcas, asList(), vdoc, 0, jcas.getDocumentText().length());
        
        assertThat(vdoc.comments())
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(new VComment(ne, VCommentType.ERROR, 
                        "Crossing sentence bounardies is not permitted."));
    }

    @Test
    public void thatIllegalStackedSpansGenerateError()
    {
        neLayer.setAllowStacking(false);
        
        jcas.setDocumentText(StringUtils.repeat("a", 10));
        
        new Sentence(jcas, 0, 10).addToIndexes();
        NamedEntity ne1 = new NamedEntity(jcas, 3, 8);
        ne1.addToIndexes();
        NamedEntity ne2 = new NamedEntity(jcas, 3, 8);
        ne2.addToIndexes();
        
        SpanAdapter adapter = new SpanAdapter(featureSupportRegistry, null, neLayer, asList(),
                asList(new SpanStackingBehavior()));
        
        SpanRenderer sut = new SpanRenderer(adapter, featureSupportRegistry,
                asList(new SpanStackingBehavior()));
        
        VDocument vdoc = new VDocument();
        sut.render(jcas, asList(), vdoc, 0, jcas.getDocumentText().length());
        
        assertThat(vdoc.comments())
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        new VComment(ne1, VCommentType.ERROR, "Stacking is not permitted."),
                        new VComment(ne2, VCommentType.ERROR, "Stacking is not permitted."));
    }
}
