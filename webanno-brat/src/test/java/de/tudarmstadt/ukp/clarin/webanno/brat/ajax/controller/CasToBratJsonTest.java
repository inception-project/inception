/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.brat.ajax.controller;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.linesOf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.brat.ajax.controller.CasToBratJsonTest.TestContext;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratRenderer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.tcf.TcfReader;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import mockit.Mock;
import mockit.MockUp;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CasToBratJsonTest
{
    private @Autowired Project project;
    private @Autowired AnnotationSchemaService annotationSchemaService;
    private @Autowired FeatureSupportRegistryImpl featureSupportRegistry;
    private @Autowired PreRenderer preRenderer;
    
    @BeforeClass
    public static void setupClass()
    {
        // Route logging through log4j
        System.setProperty("org.apache.uima.logger.class", 
                "org.apache.uima.util.impl.Log4jLogger_impl");
    }
    
    /**
     * generate BRAT JSON for the collection informations
     *
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Test
    public void testGenerateBratJsonGetCollection() throws IOException
    {
        String jsonFilePath = "target/test-output/output_cas_to_json_collection.json";

        GetCollectionInformationResponse collectionInformation = 
                new GetCollectionInformationResponse();

        List<AnnotationLayer> layerList = new ArrayList<>();

        AnnotationLayer layer = new AnnotationLayer();
        layer.setId(1l);
        layer.setDescription("span annoattion");
        layer.setName("pos");
        layer.setType(WebAnnoConst.SPAN_TYPE);

        TagSet tagset = new TagSet();
        tagset.setId(1l);
        tagset.setDescription("pos");
        tagset.setLanguage("de");
        tagset.setName("STTS");

        Tag tag = new Tag();
        tag.setId(1l);
        tag.setDescription("noun");
        tag.setName("NN");
        tag.setTagSet(tagset);

        layerList.add(layer);

        collectionInformation.addCollection("/Collection1/");
        collectionInformation.addCollection("/Collection2/");
        collectionInformation.addCollection("/Collection3/");

        collectionInformation.addDocument("/Collection1/doc1");
        collectionInformation.addDocument("/Collection2/doc1");
        collectionInformation.addDocument("/Collection3/doc1");
        collectionInformation.addDocument("/Collection1/doc2");
        collectionInformation.addDocument("/Collection2/doc2");
        collectionInformation.addDocument("/Collection3/doc2");

        collectionInformation.setSearchConfig(new ArrayList<>());

        List<String> tagSetNames = new ArrayList<>();
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.POS);
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.DEPENDENCY);
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.NAMEDENTITY);
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE);
        tagSetNames
                .add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFRELTYPE);

        JSONUtil.generatePrettyJson(collectionInformation, new File(jsonFilePath));

        assertThat(
                linesOf(new File("src/test/resources/output_cas_to_json_collection_expected.json"),
                        "UTF-8")).isEqualTo(linesOf(new File(jsonFilePath), "UTF-8"));
    }

    /**
     * generate brat JSON data for the document
     */
    @Test
    public void testGenerateBratJsonGetDocument() throws Exception
    {
        String jsonFilePath = "target/test-output/output_cas_to_json_document.json";
        String file = "src/test/resources/tcf04-karin-wl.xml";
        
        CAS cas = JCasFactory.createJCas().getCas();
        CollectionReader reader = CollectionReaderFactory.createReader(TcfReader.class,
                TcfReader.PARAM_SOURCE_LOCATION, file);
        reader.getNext(cas);
        JCas jCas = cas.getJCas();

        AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.getPreferences().setWindowSize(10);
        state.setFirstVisibleUnit(WebAnnoCasUtil.getFirstSentence(jCas));

        state.setProject(project);

        VDocument vdoc = new VDocument();
        preRenderer.render(vdoc, state, jCas, annotationSchemaService.listAnnotationLayer(project));

        GetDocumentResponse response = new GetDocumentResponse();
        BratRenderer.render(response, state, vdoc, jCas, annotationSchemaService);

        JSONUtil.generatePrettyJson(response, new File(jsonFilePath));

        assertThat(linesOf(new File("src/test/resources/output_cas_to_json_document_expected.json"),
                "UTF-8")).isEqualTo(linesOf(new File(jsonFilePath), "UTF-8"));
    }
    
    @Test
    public void testJsonDiff() throws Exception
    {
        String f_base = "src/test/resources/brat_normal.json";
        String f_addedMiddle = "src/test/resources/brat_added_entity_near_middle.json";
        String f_removedMiddle = "src/test/resources/brat_removed_entity_in_middle.json";
        String f_removedEnd = "src/test/resources/brat_removed_entity_near_end.json";

        MappingJackson2HttpMessageConverter jsonConverter = 
                new MappingJackson2HttpMessageConverter();
        
        ObjectMapper mapper = jsonConverter.getObjectMapper();
        
        JsonNode base = mapper.readTree(new File(f_base));
        JsonNode addedMiddle = mapper.readTree(new File(f_addedMiddle));
        JsonNode removedMiddle = mapper.readTree(new File(f_removedMiddle));
        JsonNode removedEnd = mapper.readTree(new File(f_removedEnd));
        
        JsonNode d_addedMiddle = JsonDiff.asJson(base, addedMiddle);
        JsonNode d_removedMiddle = JsonDiff.asJson(base, removedMiddle);
        JsonNode d_removedEnd = JsonDiff.asJson(base, removedEnd);
        
        System.out.println(d_addedMiddle);
        System.out.println(d_removedMiddle);
        System.out.println(d_removedEnd);
    }
    
    @Configuration
    @ComponentScan({
        "de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature", 
        "de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering"})
    public static class TestContext
    {
        private @Autowired Project project;
        private @Autowired AnnotationSchemaService annotationSchemaService;
        private @Autowired FeatureSupportRegistryImpl featureSupportRegistry;
        private @Autowired ApplicationEventPublisher applicationEventPublisher;

        private AnnotationLayer tokenLayer;
        private AnnotationFeature tokenPosFeature;
        private AnnotationLayer posLayer;
        private AnnotationFeature posFeature;

        {
            tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SPAN_TYPE, null, true);
            tokenLayer.setId(1l);

            tokenPosFeature = new AnnotationFeature();
            tokenPosFeature.setId(1l);
            tokenPosFeature.setName("pos");
            tokenPosFeature.setEnabled(true);
            tokenPosFeature.setType(POS.class.getName());
            tokenPosFeature.setUiName("pos");
            tokenPosFeature.setLayer(tokenLayer);
            tokenPosFeature.setProject(project);
            tokenPosFeature.setVisible(true);

            posLayer = new AnnotationLayer(POS.class.getName(), "POS", SPAN_TYPE, project, true);
            posLayer.setId(2l);
            posLayer.setAttachType(tokenLayer);
            posLayer.setAttachFeature(tokenPosFeature);

            posFeature = new AnnotationFeature();
            posFeature.setId(2l);
            posFeature.setName("PosValue");
            posFeature.setEnabled(true);
            posFeature.setType(CAS.TYPE_NAME_STRING);
            posFeature.setUiName("PosValue");
            posFeature.setLayer(posLayer);
            posFeature.setProject(project);
            posFeature.setVisible(true);
        }

        @Bean
        public Project project()
        {
            return new Project();
        }

        @Bean
        public AnnotationSchemaService annotationSchemaService()
        {
            return new MockUp<AnnotationSchemaService>()
            {
                @Mock
                List<AnnotationLayer> listAnnotationLayer(Project project)
                {
                    return asList(posLayer);
                }

                @Mock
                List<AnnotationFeature> listAnnotationFeature(AnnotationLayer type)
                {
                    if (type.getName().equals(POS.class.getName())) {
                        return asList(posFeature);
                    }
                    throw new IllegalStateException("Unknown layer type: " + type.getName());
                }

                @Mock
                TypeAdapter getAdapter(AnnotationLayer aLayer)
                {
                    return AnnotationSchemaServiceImpl.getAdapter(annotationSchemaService,
                            featureSupportRegistry, applicationEventPublisher, aLayer);
                }

            }.getMockInstance();
        }
    }
}
