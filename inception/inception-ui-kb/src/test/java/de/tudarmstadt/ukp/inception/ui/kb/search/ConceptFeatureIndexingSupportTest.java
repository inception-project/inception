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
package de.tudarmstadt.ukp.inception.ui.kb.search;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.feature.bool.BooleanFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.number.NumberFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.search.PrimitiveUimaIndexingSupport;
import de.tudarmstadt.ukp.inception.search.index.mtas.MtasUimaParser;
import de.tudarmstadt.ukp.inception.search.index.mtas.MtasUtils;
import de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState;
import de.tudarmstadt.ukp.inception.ui.kb.feature.ConceptFeatureSupport;
import de.tudarmstadt.ukp.inception.ui.kb.feature.ConceptLabelCache;
import mtas.analysis.token.MtasToken;
import mtas.analysis.token.MtasTokenCollection;

@ExtendWith(MockitoExtension.class)
public class ConceptFeatureIndexingSupportTest
{
    private Project project;
    private KnowledgeBase kb;
    private @Mock AnnotationSchemaService annotationSchemaService;
    private @Mock KnowledgeBaseService kbService;
    private FeatureSupportRegistryImpl featureSupportRegistry;
    private FeatureIndexingSupportRegistryImpl featureIndexingSupportRegistry;
    private JCas jcas;

    @BeforeEach
    public void setup() throws Exception
    {
        project = new Project();
        project.setId(1l);
        project.setName("test project");

        kb = new KnowledgeBase();

        featureSupportRegistry = new FeatureSupportRegistryImpl(asList(new StringFeatureSupport(),
                new BooleanFeatureSupport(), new NumberFeatureSupport(), new ConceptFeatureSupport(
                        new ConceptLabelCache(kbService, new KnowledgeBasePropertiesImpl()))));
        featureSupportRegistry.init();

        featureIndexingSupportRegistry = new FeatureIndexingSupportRegistryImpl(
                asList(new PrimitiveUimaIndexingSupport(featureSupportRegistry),
                        new ConceptFeatureIndexingSupport(featureSupportRegistry, kbService)));
        featureIndexingSupportRegistry.init();

        // Resetting the JCas is faster than re-creating it
        if (jcas == null) {
            jcas = JCasFactory.createJCas();
        }
        else {
            jcas.reset();
        }
    }

    @Test
    public void testConceptFeature() throws Exception
    {
        JCasBuilder builder = new JCasBuilder(jcas);
        builder.add("I", Token.class);
        builder.add(" ");
        builder.add("am", Token.class);
        builder.add(" ");
        int begin = builder.getPosition();
        builder.add("John", Token.class);
        builder.add(" ");
        builder.add("Smith", Token.class);
        NamedEntity ne = new NamedEntity(jcas, begin, builder.getPosition());
        ne.setIdentifier("urn:dummy-concept");
        ne.addToIndexes();
        builder.add(" ");
        builder.add(".", Token.class);

        AnnotationLayer layer = new AnnotationLayer(NamedEntity.class.getName(), "Named Entity",
                SpanLayerSupport.TYPE, project, true, TOKENS, NO_OVERLAP);

        KBHandle kbHandle = new KBHandle("urn:dummy-concept", "Dummy concept");
        kbHandle.setKB(kb);
        when(kbService.readHandle(any(Project.class), any(String.class)))
                .thenReturn(Optional.of(kbHandle));

        List<KBHandle> dummyValue = new ArrayList<KBHandle>();
        dummyValue.add(new KBHandle("urn:dummy-parent-concept", "Dummy Parent Concept"));

        when(kbService.getParentConceptList(any(KnowledgeBase.class), any(String.class),
                any(Boolean.class))).thenReturn(dummyValue);

        AnnotationSearchState prefs = new AnnotationSearchState();
        MtasUimaParser sut = new MtasUimaParser(
                asList(new AnnotationFeature(1l, layer, "value", CAS.TYPE_NAME_STRING),
                        new AnnotationFeature(2l, layer, "identifier", "kb:<ANY>")),
                annotationSchemaService, featureIndexingSupportRegistry, prefs);
        MtasTokenCollection tc = sut.createTokenCollection(jcas.getCas());
        MtasUtils.print(tc);

        List<MtasToken> tokens = new ArrayList<>();
        tc.iterator().forEachRemaining(tokens::add);

        assertThat(tokens).filteredOn(t -> t.getPrefix().startsWith("Named_Entity"))
                .extracting(MtasToken::getPrefix) //
                .contains("Named_Entity", "Named_Entity.identifier",
                        "Named_Entity.identifier-exact");

        assertThat(tokens).filteredOn(t -> t.getPrefix().startsWith("Named_Entity"))
                .extracting(MtasToken::getPostfix)
                .contains("", "urn:dummy-concept", "Dummy concept");
    }
}
