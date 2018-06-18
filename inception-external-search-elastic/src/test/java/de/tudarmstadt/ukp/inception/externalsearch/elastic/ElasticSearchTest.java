/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.externalsearch.elastic;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.ImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers.PartOfSpeechLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.export.ExportService;
import de.tudarmstadt.ukp.clarin.webanno.export.ExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.export.ImportService;
import de.tudarmstadt.ukp.clarin.webanno.export.ImportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextWriter;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderRegistry;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderRegistryImpl;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchServiceImpl;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.SearchServiceImpl;
import de.tudarmstadt.ukp.inception.search.index.IndexFactory;
import de.tudarmstadt.ukp.inception.search.index.IndexRegistry;
import de.tudarmstadt.ukp.inception.search.index.IndexRegistryImpl;

/**
 * The Class TestElasticSearch.
 */

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@EnableWebSecurity
@EntityScan({ "de.tudarmstadt.ukp.clarin.webanno.model",
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@TestPropertySource(locations = "classpath:ElasticSearchTest.properties")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class ElasticSearchTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private @Autowired UserDao userRepository;

    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired SearchService searchService;
    private @Autowired AnnotationSchemaService annotationSchemaService;
    private @Autowired ExternalSearchService externalSearchService;

    // If this is not static, for some reason the value is re-set to false before a
    // test method is invoked. However, the DB is not reset - and it should not be.
    // So we need to make this static to ensure that we really only create the user
    // in the DB and clean the test repository once!
    private static boolean initialized = false;

    @Before
    public void setUp()
    {
        if (!initialized) {
//            userRepository.create(new User("admin", Role.ROLE_ADMIN));
            initialized = true;

            FileSystemUtils.deleteRecursively(new File("target/ElasticSearchTest"));
        }
    }

    @Test
    public void testSimpleQuery() throws Exception
    {
        Project project = new Project();

        project.setName("TestProject");

        projectService.createProject(project);
//
//        annotationSchemaService.initializeProject(project);
//
//        User user = userRepository.get("admin");

        User user = new User();
        String query = "merck";

        externalSearchService.query(user, project, query);
    }

    @Configuration
    public static class TestContext
    {
        @Bean
        public ProjectService projectService()
        {
            return new ProjectServiceImpl();
        }

        @Bean
        public ExternalSearchProviderFactory ElasticSearchProviderFactory()
        {
            return new ElasticSearchProviderFactory();
        }

        @Lazy
        @Bean
        public NamedEntityLayerInitializer NamedEntityLayerInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationService)
        {
            return new NamedEntityLayerInitializer(aAnnotationService);
        }

        @Lazy
        @Bean
        public PartOfSpeechLayerInitializer PartOfSpeechLayerInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationSchemaService)
        {
            return new PartOfSpeechLayerInitializer(aAnnotationSchemaService);
        }

        @Lazy
        @Bean
        public TokenLayerInitializer TokenLayerInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationSchemaService)
        {
            return new TokenLayerInitializer(aAnnotationSchemaService);
        }

        @Lazy
        @Bean
        public IndexRegistry indexRegistry(
                @Lazy @Autowired(required = false) List<IndexFactory> aExtensions)
        {
            return new IndexRegistryImpl(aExtensions);
        }

        @Bean
        public SearchService searchService()
        {
            return new SearchServiceImpl();
        }

        @Lazy
        @Bean
        public ExternalSearchProviderRegistry externalSearchProviderRegistry(
                @Lazy @Autowired(required = false) List<ExternalSearchProviderFactory> aExtensions)
        {
            return new ExternalSearchProviderRegistryImpl(aExtensions);
        }

        @Bean
        public ExternalSearchService externalSearchService()
        {
            return new ExternalSearchServiceImpl();
        }

        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }

        @Bean
        public DocumentService documentService()
        {
            return new DocumentServiceImpl();
        }

        @Bean
        public AnnotationSchemaService annotationSchemaService()
        {
            return new AnnotationSchemaServiceImpl();
        }

        @Bean
        public FeatureSupportRegistry featureSupportRegistry()
        {
            return new FeatureSupportRegistryImpl(Collections.emptyList());
        }

        @Bean
        public CasStorageService casStorageService()
        {
            return new CasStorageServiceImpl();
        }

        @Bean
        public ImportExportService importExportService()
        {
            return new ImportExportServiceImpl();
        }

        @Bean
        public CurationDocumentService curationDocumentService()
        {
            return new CurationDocumentServiceImpl();
        }

        @Bean
        public ImportService importService()
        {
            return new ImportServiceImpl();
        }

        @Bean
        public ExportService exportService()
        {
            return new ExportServiceImpl();
        }

        @Bean
        public Properties formats()
        {
            Properties props = new Properties();
            props.put("text.label", "Plain text");
            props.put("text.reader", TextReader.class.getName());
            props.put("text.writer", TextWriter.class.getName());
            return props;
        }

        @Bean
        public ApplicationContextProvider contextProvider()
        {
            return new ApplicationContextProvider();
        }
    }
}
