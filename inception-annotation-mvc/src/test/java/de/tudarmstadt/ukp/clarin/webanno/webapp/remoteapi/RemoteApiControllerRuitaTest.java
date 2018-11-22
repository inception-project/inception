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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.PrimitiveUimaFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.SlotFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.ImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.ProjectExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.AnnotationLayerJSONObject;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.CreateOutputMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.FeatureInfo;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.TagSetJSONObject;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.TokenJSONObject;
import net.minidev.json.JSONArray;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@EnableWebSecurity
@Transactional
@EntityScan({ "de.tudarmstadt.ukp.clarin.webanno.model",
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@TestPropertySource(locations = "classpath:RemoteApiControllerRuitaTest.properties")
public class RemoteApiControllerRuitaTest
{
    private @Resource WebApplicationContext context;
    private @Resource UserDao userRepository;
    private @Resource FeatureSupportRegistryImpl featureSupportRegistry;
    private @Resource ProjectService projectService;
    private @Resource DocumentService documentService;
    private @Resource AnnotationSchemaService annotationService;

    private static int sleeptime = 1000;
    private MockMvc mvc;
    // If this is not static, for some reason the value is re-set to false before a
    // test method is invoked. However, the DB is not reset - and it should not be.
    // So we need to make this static to ensure that we really only create the user
    // in the DB and clean the test repository once!
    private static boolean initialized = false;

    @Before
    public void setup() throws Exception
    {
        mvc = MockMvcBuilders.webAppContextSetup(context).alwaysDo(print())
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();

        if (!initialized) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
            // userRepository.get(new User("admin", Role.ROLE_ADMIN))
            initialized = true;

            FileSystemUtils.deleteRecursively(new File("target/RemoteApiControllerRuitaTest"));
        }

        // Only set up project the first time a test is called

        // Set up a project, a sourcedocument and an annotationdocument for the tests
        // Set up project
        mvc.perform(
                post("/api/v2/projects").with(csrf().asHeader()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.MULTIPART_FORM_DATA).param("name", "project1"));
        // Set up sourcedoc
        mvc.perform(fileUpload("/api/v2/projects/1/documents")
                .file("content", "This is a test. Bla Bla".getBytes("UTF-8"))
                .with(csrf().asHeader()).with(user("admin").roles("ADMIN"))
                .param("name", "test.txt").param("format", "text"));
        // Set up annodoc
        mvc.perform(fileUpload("/api/v2/projects/1/documents/1/annotations/admin")
                .file("content", "This is a test. Bla Bla".getBytes("UTF-8"))
                .with(csrf().asHeader()).with(user("admin").roles("ADMIN"))
                .param("name", "test.txt").param("format", "text"));
        Thread.sleep(sleeptime);

    }

    @After
    public void cleanUp() throws Exception
    {

    }

    @Test
    public void t001_testTokens_shouldReturnListOfWordsFormSetup() throws Exception
    {
        mvc.perform(get("/api/ruita/projects/1/documents/1/tokens").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].coveredText").value("This"))
                .andExpect(jsonPath("$[0].begin").value(0)).andExpect(jsonPath("$[0].end").value(4))
                .andExpect(jsonPath("$[1].coveredText").value("is"))
                .andExpect(jsonPath("$[1].begin").value(5)).andExpect(jsonPath("$[1].end").value(7))
                .andExpect(jsonPath("$[2].coveredText").value("a"))
                .andExpect(jsonPath("$[2].begin").value(8)).andExpect(jsonPath("$[2].end").value(9))
                .andExpect(jsonPath("$[3].coveredText").value("test"))
                .andExpect(jsonPath("$[3].begin").value(10))
                .andExpect(jsonPath("$[3].end").value(14))
                .andExpect(jsonPath("$[4].coveredText").value("."))
                .andExpect(jsonPath("$[4].begin").value(14))
                .andExpect(jsonPath("$[4].end").value(15))
                .andExpect(jsonPath("$[5].coveredText").value("Bla"))
                .andExpect(jsonPath("$[5].begin").value(16))
                .andExpect(jsonPath("$[5].end").value(19))
                .andExpect(jsonPath("$[6].coveredText").value("Bla"))
                .andExpect(jsonPath("$[6].begin").value(20))
                .andExpect(jsonPath("$[6].end").value(23));

    }

    @Test
    public void t002_testTokensById_shouldReturnSpecifiedToken() throws Exception
    {

        MvcResult res = mvc
                .perform(get("/api/ruita/projects/1/documents/1/tokens").with(csrf().asHeader())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].coveredText").value("This"))
                .andExpect(jsonPath("$[1].coveredText").value("is"))
                .andExpect(jsonPath("$[2].coveredText").value("a"))
                .andExpect(jsonPath("$[3].coveredText").value("test"))
                .andExpect(jsonPath("$[4].coveredText").value("."))
                .andExpect(jsonPath("$[5].coveredText").value("Bla"))
                .andExpect(jsonPath("$[6].coveredText").value("Bla")).andReturn();

        // Get id of the a token
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = res.getResponse().getContentAsString();
        // JSON from String to Object
        List<TokenJSONObject> tokens = mapper.readValue(jsonInString,
                mapper.getTypeFactory().constructCollectionType(List.class, TokenJSONObject.class));
        long testTokenId = tokens.get(3).getTokenId();

        // Now try to fetch that specific token
        mvc.perform(get("/api/ruita/projects/1/documents/1/tokens/" + testTokenId)
                .with(csrf().asHeader()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.coveredText").value("test"))
                .andExpect(jsonPath("$.tokenId").value(testTokenId))
                .andExpect(jsonPath("$.begin").value(10)).andExpect(jsonPath("$.end").value(14));

    }

    @Test
    public void t003_testAnnotationPost_ShouldCreateLayer() throws Exception
    {
        // post annotation
        mvc.perform(post("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\n" + "  \"begin\": 0,\n" + "  \"end\": 4,\n" + "  \"layerId\": 7\n"
                        + "}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text").value("Create successful."));
        // Check if present
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].begin").value(0)).andExpect(jsonPath("$[0].end").value(4))
                .andExpect(jsonPath("$[0].layerId").value(7));

    }

    @Test
    public void t005_testAnnotationPostAndUpdate_ShouldCreateLayerAndSetFeatureValue()
        throws Exception
    {
        // post and update annotation in one request
        mvc.perform(post("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\n" + "  \"begin\": 5,\n" + "  \"end\": 7,\n" + "  \"layerId\": 7,\n"
                        + "  \"updateInfoList\": [\n" + "    {\n" + "      \"featureId\": 10,\n"
                        + "      \"value\": \"testFeatVal\"\n" + "    }\n" + "  ]\n" + "}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text").value("Create successful."));
        // Check if present
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].begin").value(5)).andExpect(jsonPath("$[0].end").value(7))
                .andExpect(jsonPath("$[0].layerId").value(7))
                .andExpect(jsonPath("$[0].features[0].value").value("testFeatVal"))
                .andExpect(jsonPath("$[0].features[0].id").value(10));

    }

    @Test
    public void t006_getAllAnnotations_shouldReturnAllAnnotations() throws Exception
    {
        /* -------- Create a few an annotations -------- */
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 0, 4, 9, "FirstTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 4, 0, 4, 5, "SecondTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 5, 7, 9, "ThirdTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 2, 8, 9, 2, "FourthTestValue");

        // Make sure the cas can write changes
        Thread.sleep(sleeptime);

        /* -------- Now check if they are fetched as expected -------- */

        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                // FirstTestVal
                .andExpect(jsonPath("$[0].begin").value(0)).andExpect(jsonPath("$[0].end").value(4))
                .andExpect(jsonPath("$[0].layerId").value(6))
                .andExpect(jsonPath("$[0].features[0].value").value("FirstTestValue"))
                .andExpect(jsonPath("$[0].features[0].id").value(9))
                // SecondTestVal
                .andExpect(jsonPath("$[1].begin").value(0)).andExpect(jsonPath("$[1].end").value(4))
                .andExpect(jsonPath("$[1].layerId").value(4))
                .andExpect(jsonPath("$[1].features[0].value").value("SecondTestValue"))
                .andExpect(jsonPath("$[1].features[0].id").value(5))
                // ThirdTestVal
                .andExpect(jsonPath("$[2].begin").value(5)).andExpect(jsonPath("$[2].end").value(7))
                .andExpect(jsonPath("$[2].layerId").value(6))
                .andExpect(jsonPath("$[2].features[0].value").value("ThirdTestValue"))
                .andExpect(jsonPath("$[2].features[0].id").value(9))
                // FourthTestVal
                .andExpect(jsonPath("$[3].begin").value(8)).andExpect(jsonPath("$[3].end").value(9))
                .andExpect(jsonPath("$[3].layerId").value(2))
                .andExpect(jsonPath("$[3].features[0].value").value("FourthTestValue"))
                .andExpect(jsonPath("$[3].features[0].id").value(2));

    }

    @Test
    public void t007_getFilteredAnnotations_shouldReturnOnlyMatchingAnnotations() throws Exception
    {
        /* -------- Create a few an annotations -------- */
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 0, 4, 9, "FirstTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 4, 0, 4, 5, "SecondTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 5, 7, 9, "ThirdTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 2, 8, 9, 2, "FourthTestValue");

        // Make sure the cas can write changes
        Thread.sleep(sleeptime);

        /* -------- Now check if they are fetched as expected -------- */

        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).param("layer", "Lemma"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                // FirstTestVal
                .andExpect(jsonPath("$[0].begin").value(0)).andExpect(jsonPath("$[0].end").value(4))
                .andExpect(jsonPath("$[0].layerId").value(6))
                .andExpect(jsonPath("$[0].features[0].value").value("FirstTestValue"))
                .andExpect(jsonPath("$[0].features[0].id").value(9))
                // ThirdTestVal
                .andExpect(jsonPath("$[1].begin").value(5)).andExpect(jsonPath("$[1].end").value(7))
                .andExpect(jsonPath("$[1].layerId").value(6))
                .andExpect(jsonPath("$[1].features[0].value").value("ThirdTestValue"))
                .andExpect(jsonPath("$[1].features[0].id").value(9))
                .andExpect(jsonPath("$").value(new BaseMatcher<Object>()
                {
                    @Override
                    public boolean matches(Object obj)
                    {
                        // Test if there are only these two annotations
                        return obj instanceof JSONArray && ((JSONArray) obj).size() == 2;
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        // nothing for now
                    }
                }));

    }

    @Test
    public void t008_getFirstTwoAnnotations_shouldReturnOnlyFirstTwoAnnotations() throws Exception
    {
        /* -------- Create a few an annotations -------- */
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 0, 4, 9, "FirstTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 4, 0, 4, 5, "SecondTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 5, 7, 9, "ThirdTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 2, 8, 9, 2, "FourthTestValue");

        // Make sure the cas can write changes
        Thread.sleep(sleeptime);

        /* -------- Now check if they are fetched as expected -------- */

        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).param("annotationFrom", "0")
                .param("annotationTo", "2")).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                // FirstTestVal
                .andExpect(jsonPath("$[0].begin").value(0)).andExpect(jsonPath("$[0].end").value(4))
                .andExpect(jsonPath("$[0].layerId").value(6))
                .andExpect(jsonPath("$[0].features[0].value").value("FirstTestValue"))
                .andExpect(jsonPath("$[0].features[0].id").value(9))
                // SecondTestVal
                .andExpect(jsonPath("$[1].begin").value(0)).andExpect(jsonPath("$[1].end").value(4))
                .andExpect(jsonPath("$[1].layerId").value(4))
                .andExpect(jsonPath("$[1].features[0].value").value("SecondTestValue"))
                .andExpect(jsonPath("$[1].features[0].id").value(5))
                .andExpect(jsonPath("$").value(new BaseMatcher<Object>()
                {
                    @Override
                    public boolean matches(Object obj)
                    {
                        // Test if there are only these two annotations
                        return obj instanceof JSONArray && ((JSONArray) obj).size() == 2;
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        // nothing for now
                    }
                }));

    }

    @Test
    public void t009_getAnnotationsInTRange_shouldReturnAnnotationsInTRange() throws Exception
    {
        /* -------- Create a few an annotations -------- */
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 0, 4, 9, "FirstTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 4, 0, 4, 5, "SecondTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 5, 7, 9, "ThirdTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 2, 8, 9, 2, "FourthTestValue");

        // Make sure the cas can write changes
        Thread.sleep(sleeptime);

        /* -------- Now check if they are fetched as expected -------- */

        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).param("textIndexFrom", "5")
                .param("textIndexTo", "10")).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                // ThirdTestVal
                .andExpect(jsonPath("$[0].begin").value(5)).andExpect(jsonPath("$[0].end").value(7))
                .andExpect(jsonPath("$[0].layerId").value(6))
                .andExpect(jsonPath("$[0].features[0].value").value("ThirdTestValue"))
                .andExpect(jsonPath("$[0].features[0].id").value(9))
                // FourthTestVal
                .andExpect(jsonPath("$[1].begin").value(8)).andExpect(jsonPath("$[1].end").value(9))
                .andExpect(jsonPath("$[1].layerId").value(2))
                .andExpect(jsonPath("$[1].features[0].value").value("FourthTestValue"))
                .andExpect(jsonPath("$[1].features[0].id").value(2))
                .andExpect(jsonPath("$").value(new BaseMatcher<Object>()
                {
                    @Override
                    public boolean matches(Object obj)
                    {
                        // Test if there are only these two annotations
                        return obj instanceof JSONArray && ((JSONArray) obj).size() == 2;
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        // nothing for now
                    }
                }));

    }

    @Test
    public void t010_getAnnotationsMultiParam_shouldReturnAnnotationsInTRangeAndOfType()
        throws Exception
    {
        /* -------- Create a few an annotations -------- */
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 0, 4, 9, "FirstTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 4, 0, 4, 5, "SecondTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 5, 7, 9, "ThirdTestValue");
        setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 2, 8, 9, 2, "FourthTestValue");

        // Make sure the cas can write changes
        Thread.sleep(sleeptime);

        /* -------- Now check if they are fetched as expected -------- */

        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).param("textIndexFrom", "5")
                .param("textIndexTo", "10").param("layer", "pos")).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                // FourthTestVal
                .andExpect(jsonPath("$[0].begin").value(8)).andExpect(jsonPath("$[0].end").value(9))
                .andExpect(jsonPath("$[0].layerId").value(2))
                .andExpect(jsonPath("$[0].features[0].value").value("FourthTestValue"))
                .andExpect(jsonPath("$[0].features[0].id").value(2))
                .andExpect(jsonPath("$").value(new BaseMatcher<Object>()
                {
                    @Override
                    public boolean matches(Object obj)
                    {
                        // Test if there are only these two annotations
                        return obj instanceof JSONArray && ((JSONArray) obj).size() == 1;
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        // nothing for now
                    }
                }));

    }

    @Test
    public void t011_deleteSpanAnnotation_shouldReturnListWithoutDeletedAnno() throws Exception
    {
        /* -------- Create a few an annotations -------- */
        int id1 = setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 0, 4, 9,
                "FirstTestValue");
        int id2 = setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 4, 0, 4, 5,
                "SecondTestValue");
        int id3 = setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 5, 7, 9,
                "ThirdTestValue");
        int id4 = setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 2, 8, 9, 2,
                "FourthTestValue");

        // Make sure the cas can write changes
        Thread.sleep(sleeptime);

        /* -------- Now check if they are fetched as expected -------- */

        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                // FirstTestVal
                .andExpect(jsonPath("$[0].begin").value(0)).andExpect(jsonPath("$[0].end").value(4))
                .andExpect(jsonPath("$[0].layerId").value(6))
                .andExpect(jsonPath("$[0].features[0].value").value("FirstTestValue"))
                .andExpect(jsonPath("$[0].features[0].id").value(9))
                // SecondTestVal
                .andExpect(jsonPath("$[1].begin").value(0)).andExpect(jsonPath("$[1].end").value(4))
                .andExpect(jsonPath("$[1].layerId").value(4))
                .andExpect(jsonPath("$[1].features[0].value").value("SecondTestValue"))
                .andExpect(jsonPath("$[1].features[0].id").value(5))
                // ThirdTestVal
                .andExpect(jsonPath("$[2].begin").value(5)).andExpect(jsonPath("$[2].end").value(7))
                .andExpect(jsonPath("$[2].layerId").value(6))
                .andExpect(jsonPath("$[2].features[0].value").value("ThirdTestValue"))
                .andExpect(jsonPath("$[2].features[0].id").value(9))
                // FourthTestVal
                .andExpect(jsonPath("$[3].begin").value(8)).andExpect(jsonPath("$[3].end").value(9))
                .andExpect(jsonPath("$[3].layerId").value(2))
                .andExpect(jsonPath("$[3].features[0].value").value("FourthTestValue"))
                .andExpect(jsonPath("$[3].features[0].id").value(2));

        /* -------- deleted one -------- */
        mvc.perform(delete("/api/ruita/projects/1/documents/1/annotations/" + id1)
                .with(csrf().asHeader()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text").value("Annotation was successfully deleted"));

        Thread.sleep(sleeptime);
        /* -------- Check again if they are fetched as expected -------- */
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                // SecondTestVal
                .andExpect(jsonPath("$[0].begin").value(0)).andExpect(jsonPath("$[0].end").value(4))
                .andExpect(jsonPath("$[0].layerId").value(4))
                .andExpect(jsonPath("$[0].features[0].value").value("SecondTestValue"))
                .andExpect(jsonPath("$[0].features[0].id").value(5))
                // ThirdTestVal
                .andExpect(jsonPath("$[1].begin").value(5)).andExpect(jsonPath("$[1].end").value(7))
                .andExpect(jsonPath("$[1].layerId").value(6))
                .andExpect(jsonPath("$[1].features[0].value").value("ThirdTestValue"))
                .andExpect(jsonPath("$[1].features[0].id").value(9))
                // FourthTestVal
                .andExpect(jsonPath("$[2].begin").value(8)).andExpect(jsonPath("$[2].end").value(9))
                .andExpect(jsonPath("$[2].layerId").value(2))
                .andExpect(jsonPath("$[2].features[0].value").value("FourthTestValue"))
                .andExpect(jsonPath("$[2].features[0].id").value(2))
                .andExpect(jsonPath("$").value(new BaseMatcher<Object>()
                {
                    @Override
                    public boolean matches(Object obj)
                    {
                        // Test if there are only these two annotations
                        return obj instanceof JSONArray && ((JSONArray) obj).size() == 3;
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        // nothing for now
                    }
                }));

        /* -------- deleted one -------- */
        mvc.perform(delete("/api/ruita/projects/1/documents/1/annotations/" + id3)
                .with(csrf().asHeader()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text").value("Annotation was successfully deleted"));

        Thread.sleep(sleeptime);
        /* -------- Check again if they are fetched as expected -------- */
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                // SecondTestVal
                .andExpect(jsonPath("$[0].begin").value(0)).andExpect(jsonPath("$[0].end").value(4))
                .andExpect(jsonPath("$[0].layerId").value(4))
                .andExpect(jsonPath("$[0].features[0].value").value("SecondTestValue"))
                .andExpect(jsonPath("$[0].features[0].id").value(5))
                // FourthTestVal
                .andExpect(jsonPath("$[1].begin").value(8)).andExpect(jsonPath("$[1].end").value(9))
                .andExpect(jsonPath("$[1].layerId").value(2))
                .andExpect(jsonPath("$[1].features[0].value").value("FourthTestValue"))
                .andExpect(jsonPath("$[1].features[0].id").value(2))
                .andExpect(jsonPath("$").value(new BaseMatcher<Object>()
                {
                    @Override
                    public boolean matches(Object obj)
                    {
                        // Test if there are only these two annotations
                        return obj instanceof JSONArray && ((JSONArray) obj).size() == 2;
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        // nothing for now
                    }
                }));

    }

    @Test
    public void t012_updateExistingAnnotation_shouldShowNewFeatureValue() throws Exception
    {
        /* -------- Create a few an annotations -------- */
        int id1 = setupSpanAnnotationInTestDocument(1, "test.txt", "admin", 6, 0, 4, 9,
                "FirstTestValue");

        // Make sure the cas can write changes
        Thread.sleep(sleeptime);

        /* -------- Now check if they are fetched as expected -------- */

        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                // FirstTestValue
                .andExpect(jsonPath("$[0].begin").value(0)).andExpect(jsonPath("$[0].end").value(4))
                .andExpect(jsonPath("$[0].layerId").value(6))
                .andExpect(jsonPath("$[0].features[0].value").value("FirstTestValue"))
                .andExpect(jsonPath("$[0].features[0].id").value(9));

        /* -------- Now update a FeatureValue -------- */
        mvc.perform(put("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\n" + "\"annotationId\": " + id1 + ",\n" + "\"featureId\": " + 9 + ",\n"
                        + "\"value\": \"newTestValue\"\n" + "}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text").value("Annotation updated"));

        Thread.sleep(sleeptime);
        /* -------- Now check if value was updated -------- */
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                // newTestValue
                .andExpect(jsonPath("$[0].begin").value(0)).andExpect(jsonPath("$[0].end").value(4))
                .andExpect(jsonPath("$[0].layerId").value(6))
                .andExpect(jsonPath("$[0].features[0].value").value("newTestValue"))
                .andExpect(jsonPath("$[0].features[0].id").value(9));

    }

    @Test
    public void t013_getTagSetById_shouldReturnExactTagSet() throws Exception
    {
        // TagSet of Named Entity Feature Value
        long testTagSetId = 4;

        // fetch tag names from the system to test against
        TagSet tagset = annotationService.getTagSet(testTagSetId);
        List<Tag> referenceList = annotationService.listTags(tagset);
        List<String> expectedTagNames = referenceList.stream().map(t -> t.getName())
                .collect(Collectors.toList());

        /* -------- Now check if the tagset is fetched as expected -------- */

        mvc.perform(get("/api/ruita/projects/1/tagSets/" + testTagSetId).with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.description").value(tagset.getDescription()))
                .andExpect(jsonPath("$.id").value(testTagSetId))
                .andExpect(jsonPath("$.name").value(tagset.getName()))
                .andExpect(jsonPath("$.tagNames").value(new BaseMatcher<Object>()
                {
                    @Override
                    public boolean matches(Object obj)
                    {
                        // Test if lists are identical
                        return ((JSONArray) obj).containsAll(expectedTagNames)
                                && expectedTagNames.containsAll(((JSONArray) obj));
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        // nothing for now
                    }
                }));
    }

    @Test
    public void t014_getFitlerdTagSetById_shouldReturnOnlyMatchingTagSets() throws Exception
    {
        // TagSet of Named Entity Feature Value
        String filterString = "LO";
        long testTagSetId = 4;

        // fetch tag names from the system to test against
        TagSet tagset = annotationService.getTagSet(testTagSetId);
        List<Tag> referenceList = annotationService.listTags(tagset);
        List<String> expectedTagNames = referenceList.stream().map(t -> t.getName())
                .filter(t -> t.startsWith(filterString)).collect(Collectors.toList());

        /* -------- Now check if the tagset is fetched as expected -------- */

        mvc.perform(get("/api/ruita/projects/1/tagSets/" + testTagSetId).with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).param("currentInput", filterString))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.description").value(tagset.getDescription()))
                .andExpect(jsonPath("$.id").value(testTagSetId))
                .andExpect(jsonPath("$.name").value(tagset.getName()))
                .andExpect(jsonPath("$.tagNames").value(new BaseMatcher<Object>()
                {
                    @Override
                    public boolean matches(Object obj)
                    {
                        // Test if lists are identical
                        return ((JSONArray) obj).containsAll(expectedTagNames)
                                && expectedTagNames.containsAll(((JSONArray) obj));
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        // nothing for now
                    }
                }));

        // Test "number" parameter
        mvc.perform(get("/api/ruita/projects/1/tagSets/" + testTagSetId).with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).param("number", "2")).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.description").value(tagset.getDescription()))
                .andExpect(jsonPath("$.id").value(testTagSetId))
                .andExpect(jsonPath("$.name").value(tagset.getName()))
                .andExpect(jsonPath("$.tagNames").value(new BaseMatcher<Object>()
                {
                    @Override
                    public boolean matches(Object obj)
                    {
                        // Test if lists are identical
                        return obj instanceof JSONArray && ((JSONArray) obj).size() == 2;
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        // nothing for now
                    }
                }));
    }

    @Test
    public void t015_getAllTagSets_shouldReturnAllTagSets() throws Exception
    {

        Project project = projectService.getProject(1);
        List<TagSet> referenceTagSets = annotationService.listTagSets(project);

        // Map to Ids and
        List<Long> referenceTagSetIds = referenceTagSets.stream().map(l -> l.getId())
                .collect(Collectors.toList());
        /* -------- Now check if the tagset is fetched as expected -------- */

        MvcResult res = mvc
                .perform(get("/api/ruita/projects/1/tagSets/").with(csrf().asHeader())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8")).andReturn();

        // Get Object out of return value to test features
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = res.getResponse().getContentAsString();
        // JSON from String to Object
        List<TagSetJSONObject> tagSetList = mapper.readValue(jsonInString, mapper.getTypeFactory()
                .constructCollectionType(List.class, TagSetJSONObject.class));

        // Filter out the ids to compare with reference
        List<Long> layerNames = tagSetList.stream().map(l -> l.getId())
                .collect(Collectors.toList());

        // Now compare with the reference layers
        assertTrue(layerNames.containsAll(referenceTagSetIds)
                && referenceTagSetIds.containsAll(layerNames));
    }

    @Test
    public void t016_getLayerById_shouldReturnExactLayer() throws Exception
    {
        // TagSet of Named Entity Feature Value
        long testLayerId = 4;
        // fetch tag names from the system to test against
        AnnotationLayer testLayer = annotationService.getLayer(testLayerId);
        List<AnnotationFeature> referenceFeatures = annotationService
                .listAnnotationFeature(testLayer);

        /* -------- Now check if the tagset is fetched as expected -------- */

        MvcResult res = mvc
                .perform(get("/api/ruita/projects/1/layers/" + testLayerId).with(csrf().asHeader())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.layerId").value(testLayerId))
                .andExpect(jsonPath("$.uiName").value(testLayer.getUiName()))
                .andExpect(jsonPath("$.type").value(testLayer.getType()))
                .andExpect(jsonPath("$.description").value(testLayer.getDescription()))
                .andExpect(jsonPath("$.readonly").value(testLayer.isReadonly()))
                .andExpect(jsonPath("$.crossSentence").value(testLayer.isCrossSentence()))
                .andExpect(jsonPath("$.allowStacking").value(testLayer.isAllowStacking()))
                .andExpect(jsonPath("$.multipleTokens").value(testLayer.getAnchoringMode()
                        .equals(AnchoringMode.TOKENS)))
                .andExpect(jsonPath("$.partialTokenCovering").value(!testLayer.getAnchoringMode()
                                .equals(AnchoringMode.CHARACTERS)))
                .andReturn();

        // Get Object out of return value to test features
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = res.getResponse().getContentAsString();
        // JSON from String to Object
        AnnotationLayerJSONObject layerObject = mapper.readValue(jsonInString,
                AnnotationLayerJSONObject.class);
        List<FeatureInfo> features = layerObject.getFeatures();
        // Now Check if all features have the correct values
        for (int i = 0; i < features.size(); i++) {
            assertEquals((long) referenceFeatures.get(i).getId(), features.get(i).getId());
            assertEquals(referenceFeatures.get(i).getDescription(),
                    features.get(i).getDescription());
            assertEquals(referenceFeatures.get(i).getUiName(), features.get(i).getUiName());
            assertEquals(referenceFeatures.get(i).getType(), features.get(i).getType());
            assertEquals(referenceFeatures.get(i).getName(), features.get(i).getName());
            assertEquals(
                    (MultiValueMode.ARRAY.equals(referenceFeatures.get(i).getMultiValueMode())),
                    features.get(i).isMulti());
            assertEquals(referenceFeatures.get(i).isRequired(), features.get(i).isRequired());
            // If tagset exists, compare id
            if (referenceFeatures.get(i).getTagset() != null) {
                assertEquals((long) referenceFeatures.get(i).getTagset().getId(),
                        features.get(i).getTagSetId());
            }
            else {
                assertEquals(0, features.get(i).getTagSetId());
            }

        }

    }

    @Test
    public void t017_getAllLayer_shouldReturnAllLayer() throws Exception
    {

        Project project = projectService.getProject(1);
        List<AnnotationLayer> referenceLayer = annotationService.listAnnotationLayer(project);

        // Map to Names and filter out tokens because the api should not return type token
        String tokenName = "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token";
        List<String> referenceLayerNames = referenceLayer.stream().map(l -> l.getName())
                .filter(t -> !tokenName.equals(t)).collect(Collectors.toList());
        /* -------- Now check if the tagset is fetched as expected -------- */

        MvcResult res = mvc
                .perform(get("/api/ruita/projects/1/layers/").with(csrf().asHeader())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8")).andReturn();

        // Get Object out of return value to test features
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = res.getResponse().getContentAsString();
        // JSON from String to Object
        List<AnnotationLayerJSONObject> layerList = mapper.readValue(jsonInString,
                mapper.getTypeFactory().constructCollectionType(List.class,
                        AnnotationLayerJSONObject.class));

        // Filter out the layerNames to compare with reference
        List<String> layerNames = layerList.stream().map(l -> l.getName())
                .collect(Collectors.toList());

        // Now compare with the reference layers
        assertTrue(layerNames.containsAll(referenceLayerNames)
                && referenceLayerNames.containsAll(layerNames));
    }

    /*
     * Exception tests
     */

    @Test
    public void t018_lockedToTokenOffset_shouldReturnExtendedMsg() throws Exception
    {
        // Post a lemma annotation (it can only cover exactly one token)
        mvc.perform(post("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\n" + "  \"begin\": 5,\n" + "  \"end\": 14,\n" + "  \"layerId\": 6\n"
                        + "}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text")
                        .value("Create successful. Note: Begin and End of the annotation have been"
                                + "changed. This can happen if the layer is locked to a token"
                                + " offset. New Values: begin:" + "5" + ",end:" + "7"));

        Thread.sleep(sleeptime);

        // Check if annotation is present
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].begin").value(5)).andExpect(jsonPath("$[0].end").value(7))
                .andExpect(jsonPath("$[0].layerId").value(6));
    }

    @Test
    public void t019_readOnly_shouldReturnError() throws Exception
    {

        Project project = projectService.getProject(1);
        SourceDocument sourceDoc = documentService.getSourceDocument(project, "test.txt");
        User user = new User("admin", Role.ROLE_ADMIN);
        AnnotationDocument annoDoc = documentService.getAnnotationDocument(sourceDoc, user);

        AnnotationLayer newLayer = new AnnotationLayer("ReadOnlyLayer", "ReadOnlyLayer", "span",
                project, false);
        newLayer.setReadonly(true);
        annotationService.createLayer(newLayer);

        JCas jcas = documentService.readAnnotationCas(annoDoc);
        // Upgrade the cas
        try (AutoCloseable AutoJcas = (AutoCloseable) jcas) {
            annotationService.upgradeCas(jcas.getCas(), sourceDoc, "admin");
        }
        long newLayerId = annotationService.getLayer("ReadOnlyLayer", project).getId();

        // Post a new readonly annotation
        MvcResult res = mvc
                .perform(post("/api/ruita/projects/1/documents/1/annotations")
                        .with(csrf().asHeader()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content("{\n" + "  \"begin\": 5,\n" + "  \"end\": 7,\n" + "  \"layerId\": "
                                + newLayerId + "\n" + "}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text").value("Create successful.")).andReturn();

        Thread.sleep(sleeptime);
        // Get Object out of return value to test features
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = res.getResponse().getContentAsString();
        // JSON from String to Object
        CreateOutputMessage outMsg = mapper.readValue(jsonInString, (CreateOutputMessage.class));

        // Try to delete the annotation
        mvc.perform(delete(
                "/api/ruita/projects/1/documents/1/annotations/" + outMsg.getCreatedAnnotationId())
                        .with(csrf().asHeader()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text")
                        .value("Cannot delete an annotation on a read-only layer."));

        // Check if there is still an annotation
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].begin").value(5)).andExpect(jsonPath("$[0].end").value(7))
                .andExpect(jsonPath("$[0].layerId").value(newLayerId));
    }

    @Test
    public void t020_layerDoesNotExist_shouldReturnProperError() throws Exception
    {
        // Post a pos annotation
        MvcResult res = mvc
                .perform(post("/api/ruita/projects/1/documents/1/annotations")
                        .with(csrf().asHeader()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content("{\n" + "  \"begin\": 5,\n" + "  \"end\": 7,\n" + "  \"layerId\": "
                                + "2" + "\n" + "}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text").value("Create successful.")).andReturn();
        Thread.sleep(sleeptime);

        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = res.getResponse().getContentAsString();
        // JSON from String to Object
        CreateOutputMessage outMsg = mapper.readValue(jsonInString, (CreateOutputMessage.class));

        // Try to update the annotation
        mvc.perform(put("/api/ruita/projects/1/documents/1/annotations/").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\n" + "\"annotationId\": " + outMsg.getCreatedAnnotationId() + ",\n"
                        + "\"featureId\": " + 9 + ",\n" + "\"value\": \"newTestValue\"\n" + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text")
                        .value("Feature with Id " + 9 + " does not exist for this annotation"));

        // Check if there is a value
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].features[0].value").doesNotExist());
    }

    @Test
    public void t021_NoCrossSentence_shouldReturnProperError() throws Exception
    {
        // Post a chunck annotation and check if correct error msg is send
        mvc.perform(post("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\n" + "  \"begin\": 5,\n" + "  \"end\": 19,\n" + "  \"layerId\": " + 7
                        + "\n" + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text")
                        .value("Layer does not allow cross sentence annotations."));

        // Check if annotation has not been created
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$").value(new BaseMatcher<Object>()
                {
                    @Override
                    public boolean matches(Object obj)
                    {
                        // Test if there are only these two annotations
                        return obj instanceof JSONArray && ((JSONArray) obj).size() == 0;
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        // nothing for now
                    }
                }));
    }

    @Test
    public void t022_testreadOnlyUpdate_ShouldReturnErrorAndNotUpdate() throws Exception
    {

        Project project = projectService.getProject(1);
        SourceDocument sourceDoc = documentService.getSourceDocument(project, "test.txt");
        User user = new User("admin", Role.ROLE_ADMIN);
        AnnotationDocument annoDoc = documentService.getAnnotationDocument(sourceDoc, user);

        // Create readonly Layer
        AnnotationLayer newLayer = new AnnotationLayer("ReadOnlyLayer", "ReadOnlyLayer", "span",
                project, false);
        newLayer.setReadonly(true);
        annotationService.createLayer(newLayer);

        // Create Feature
        AnnotationFeature newFeature = new AnnotationFeature();
        newFeature.setLayer(newLayer);
        newFeature.setName("MyFeature");
        newFeature.setUiName("MyFeature");
        newFeature.setType(CAS.TYPE_NAME_STRING);
        annotationService.createFeature(newFeature);

        JCas jcas = documentService.readAnnotationCas(annoDoc);
        // Upgrade the cas
        try (AutoCloseable AutoJcas = (AutoCloseable) jcas) {
            annotationService.upgradeCas(jcas.getCas(), sourceDoc, "admin");
        }
        long newLayerId = annotationService.getLayer("ReadOnlyLayer", project).getId();
        long newFeatureId = annotationService.getFeature("MyFeature", newLayer).getId();

        int id1 = setupSpanAnnotationInTestDocument(1, "test.txt", "admin", (int) newLayerId, 5, 7,
                newFeatureId, "FirstTestValue");
        System.out.println(annotationService.getLayer(newLayerId).isReadonly());

        /* -------- Now try to update a FeatureValue -------- */
        mvc.perform(put("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\n" + "\"annotationId\": " + id1 + ",\n" + "\"featureId\": "
                        + newFeatureId + ",\n" + "\"value\": \"newTestValue\"\n" + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json;charset=UTF-8")).andExpect(
                        jsonPath("$.text").value("Layer of the specified Annotation is Readonly."));

        // Check if present
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].begin").value(5)).andExpect(jsonPath("$[0].end").value(7))
                .andExpect(jsonPath("$[0].layerId").value(newLayerId))
                .andExpect(jsonPath("$[0].features[0].value").value("FirstTestValue"))
                .andExpect(jsonPath("$[0].features[0].id").value(newFeatureId));
    }

    @Test
    public void t004_testLayerNotStackable_shouldReturnErrorMSG() throws Exception
    {
        // Post an annotation
        mvc.perform(post("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\n" + "  \"begin\": 5,\n" + "  \"end\": 7,\n" + "  \"layerId\": 7\n"
                        + "}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text").value("Create successful."));

        // Check if annotation is present
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].begin").value(5)).andExpect(jsonPath("$[0].end").value(7))
                .andExpect(jsonPath("$[0].layerId").value(7));

        // Try another post with the same layer on same token and expect error message
        mvc.perform(post("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\n" + "  \"begin\": 5,\n" + "  \"end\": 7,\n" + "  \"layerId\": 7\n"
                        + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.text").value("Layer not stackable."));

        // Check if there is still only one annotation
        mvc.perform(get("/api/ruita/projects/1/documents/1/annotations").with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$").value(new BaseMatcher<Object>()
                {
                    @Override
                    public boolean matches(Object obj)
                    {
                        return obj instanceof JSONArray && ((JSONArray) obj).size() == 1;
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        // nothing for now
                    }
                })).andExpect(jsonPath("$[0].begin").value(5))
                .andExpect(jsonPath("$[0].end").value(7))
                .andExpect(jsonPath("$[0].layerId").value(7));

    }

    /**
     * Method used to set up annotations for testing purposes.
     * 
     * @param projectId
     *            id of the project that contains the annodoc and sourcedoc .
     * @param documentName
     *            sourceDocument name.
     * @param username
     *            name of the User
     * @param testLayerId
     *            id of the layer that is created.
     * @param begin
     *            begin of the annotation.
     * @param end
     *            end of the annotation.
     * @param featId
     *            feature id.
     * @param simpleFeatValue
     *            value to set.
     * @return id of the new created annotation.
     * @throws Exception
     *             any errors caused inside of the function.
     */
    public int setupSpanAnnotationInTestDocument(long projectId, String documentName,
            String username, int testLayerId, int begin, int end, long featId,
            String simpleFeatValue)
        throws Exception
    {
        // get annotation document etc.
        Project project = projectService.getProject(projectId);
        SourceDocument doc = documentService.getSourceDocument(project, documentName);
        AnnotationDocument anno = documentService.getAnnotationDocument(doc,
                userRepository.get(username));
        
        // create layer and Adapter
        AnnotationLayer layer = annotationService.getLayer(testLayerId);
        SpanAdapter ad = (SpanAdapter) annotationService.getAdapter(layer);
        
        // getJCas
        // JCas jcas = documentService.readAnnotationCas(anno, 'w');
        JCas jcas = documentService.readAnnotationCas(anno);

        AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setDocument(doc, asList(doc));
        state.setUser(new User("username"));
        
        // add annotations
        int newAnnotationId = -1;
        try (AutoCloseable AutoJcas = (AutoCloseable) jcas) {
            newAnnotationId = ad.add(state, jcas, begin, end);
            AnnotationFeature aFeature = annotationService.getFeature(featId);
            ad.setFeatureValue(state, jcas, newAnnotationId, aFeature, simpleFeatValue);
            Collection<Annotation> col = JCasUtil.select(jcas, Annotation.class);
            System.out.println(col);
        }
        return newAnnotationId;

    }

    @Configuration
    public static class TestContext {
        @Autowired ApplicationEventPublisher applicationEventPublisher;
        
        @Bean
        public RemoteApiController2 remoteApiV2()
        {
            return new RemoteApiController2();
        }
        
        @Bean
        public ProjectService projectService()
        {
            return new ProjectServiceImpl();
        }
        
        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }
        
        @Bean
        public DocumentService documentService()
        {
            return new DocumentServiceImpl(repositoryProperties(), userRepository(),
                    casStorageService(), importExportService(), projectService(),
                    applicationEventPublisher);
        }
        
        @Bean
        public AnnotationSchemaService annotationService()
        {
            return new AnnotationSchemaServiceImpl();
        }
        
        @Bean
        public FeatureSupportRegistry featureSupportRegistry()
        {
            return new FeatureSupportRegistryImpl(asList(
                    new PrimitiveUimaFeatureSupport(),
                    new SlotFeatureSupport(annotationService())));
        }
        
        @Bean
        public CasStorageService casStorageService()
        {
            return new CasStorageServiceImpl(null, repositoryProperties(), backupProperties());
        }
        
        @Bean
        public ImportExportService importExportService()
        {
            return new ImportExportServiceImpl(repositoryProperties(),
                    asList(new TextFormatSupport()), casStorageService(), annotationService());
        }
        
        @Bean
        public CurationDocumentService curationDocumentService()
        {
            return new CurationDocumentServiceImpl();
        }

        @Bean
        public ProjectExportService exportService()
        {
            return new ProjectExportServiceImpl(null, projectService());
        }
        
        @Bean
        public RepositoryProperties repositoryProperties()
        {
            return new RepositoryProperties();
        }

        @Bean 
        public BackupProperties backupProperties()
        {
            return new BackupProperties();
        }

        @Bean
        public ApplicationContextProvider contextProvider()
        {
            return new ApplicationContextProvider();
        }
        
        @Bean
        public LayerSupportRegistry layerSupportRegistry()
        {
            return new LayerSupportRegistryImpl(asList(
                    new SpanLayerSupport(featureSupportRegistry(), null, annotationService()),
                    new RelationLayerSupport(featureSupportRegistry(), null, annotationService()),
                    new ChainLayerSupport(featureSupportRegistry(), null, annotationService())));
        }
    }
}
