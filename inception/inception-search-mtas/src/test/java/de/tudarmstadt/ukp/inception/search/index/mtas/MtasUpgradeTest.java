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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUimaParserLuceneTest.createBinaryCasDocument;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.diag.config.CasDoctorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.io.xmi.XmiFormatSupport;
import de.tudarmstadt.ukp.inception.io.xmi.config.UimaFormatsPropertiesImpl.XmiFormatProperties;
import de.tudarmstadt.ukp.inception.preferences.config.PreferencesServiceAutoConfig;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistry;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.index.IndexRebuildRequiredException;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

@Transactional
@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off", //
                "debug.cas-doctor.force-release-behavior=true", //
                "document-import.run-cas-doctor-on-import=OFF" })
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration
@ImportAutoConfiguration( //
        classes = { //
                ConstraintsServiceAutoConfiguration.class, //
                PreferencesServiceAutoConfig.class, //
                ProjectServiceAutoConfiguration.class, //
                AnnotationSchemaServiceAutoConfiguration.class, //
                CasDoctorAutoConfiguration.class, //
                DocumentServiceAutoConfiguration.class, //
                CasStorageServiceAutoConfiguration.class, //
                DocumentImportExportServiceAutoConfiguration.class, //
                SchedulingServiceAutoConfiguration.class, //
                SecurityAutoConfiguration.class, //
                SearchServiceAutoConfiguration.class }, //
        exclude = LiquibaseAutoConfiguration.class)
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.security.model", //
        "de.tudarmstadt.ukp.inception.preferences.model", //
        "de.tudarmstadt.ukp.inception.kb.model", //
        "de.tudarmstadt.ukp.clarin.webanno.model" })
public class MtasUpgradeTest
{
    static final String REF_DIR = "src/test/resources/MtasUpgradeTest";
    static final String WORK_DIR = "target/test-output/MtasUpgradeTest";

    @Autowired
    ProjectService projectService;

    @Autowired
    DocumentService documentService;

    @Autowired
    RepositoryProperties repositoryProperties;

    @Autowired
    FeatureIndexingSupportRegistry featureIndexingSupportRegistry;

    @Autowired
    FeatureSupportRegistry featureSupportRegistry;

    Project project;
    SourceDocument srcDoc;
    AnnotationDocument annDoc;
    MtasDocumentIndex index;

    @BeforeEach
    void setup() throws Exception
    {
        deleteQuietly(new File(WORK_DIR));

        project = new Project("test");
        projectService.createProject(project);

        srcDoc = new SourceDocument("test.txt", project, TextFormatSupport.ID);
        annDoc = new AnnotationDocument("user", srcDoc);
        documentService.createSourceDocument(srcDoc);
        documentService.createOrUpdateAnnotationDocument(annDoc);

        var indexDir = repositoryProperties.getPath().toPath() //
                .resolve(PROJECT_FOLDER) //
                .resolve(Long.toString(project.getId())) //
                .resolve(MtasDocumentIndexFactory.INDEX) //
                .toFile();
        index = new MtasDocumentIndex(project, documentService, indexDir,
                featureIndexingSupportRegistry, featureSupportRegistry);
    }

    @AfterEach
    void teardown() throws Exception
    {
        if (index != null) {
            index.close();
        }
    }

    @Disabled("Only needed when we need to create new reference data")
    @Test
    void createIndex() throws Exception
    {
        var binCas = createBinaryCasDocument(0, "test.txt",
                "This is a test . This is sentence two .");
        index.indexDocument(annDoc, binCas);
    }

    @Test
    void thatExistingIndexCanBeRead() throws Exception
    {
        copyDirectory(new File(REF_DIR, "lucene-7.7.3"), new File(WORK_DIR));

        assertThatExceptionOfType(IndexRebuildRequiredException.class) //
                .isThrownBy(() -> index.open());

        // var results = index.executeQuery(new SearchQueryRequest(project, new User("user"),
        // "is"));
        //
        // assertThat(results).containsKey("test.txt");
        // assertThat(results.get("test.txt")) //
        // .extracting( //
        // SearchResult::getLeftContext, //
        // SearchResult::getText, //
        // SearchResult::getRightContext)
        // .containsExactlyInAnyOrder( //
        // tuple("This ", "is", " a test ."),
        // tuple("test . This ", "is", " sentence two ."));
    }

    @SpringBootConfiguration
    public static class SpringConfig
    {
        @Bean
        ApplicationContextProvider applicationContextProvider()
        {
            return new ApplicationContextProvider();
        }

        @Bean
        RepositoryProperties repositoryProperties()
        {
            var props = new RepositoryPropertiesImpl();
            props.setPath(new File(WORK_DIR));
            return props;
        }

        @Bean
        public XmiFormatSupport xmiFormatSupport()
        {
            return new XmiFormatSupport(new XmiFormatProperties());
        }
    }
}
