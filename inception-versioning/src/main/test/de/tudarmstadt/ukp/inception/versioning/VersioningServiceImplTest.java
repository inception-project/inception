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
package de.tudarmstadt.ukp.inception.versioning;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.junit4.SpringRunner;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.BooleanFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.NumberFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.StringFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;

@RunWith(SpringRunner.class)
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
public class VersioningServiceImplTest
{
    private @Autowired VersioningService sut;
    private @Autowired TestEntityManager testEntityManager;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired AnnotationSchemaService annotationSchemaService;
    private @Autowired ProjectService projectService;

    @Rule
    public TemporaryFolder repoDir = new TemporaryFolder();

    private Project testProject;

    @Before
    public void setUp() throws Exception
    {
        repositoryProperties.setPath(repoDir.getRoot());
        testProject = new Project("testProject");
    }

    @After
    public void tearDown()
    {
        testEntityManager.clear();
    }

    @Test
    public void creatingProject_ShouldCreateGitRepository() throws IOException
    {
        projectService.createProject(testProject);

        assertThat(sut.getRepoDir(testProject.getId()))
                .isDirectoryContaining(f -> f.getName().equals(".git") && f.isDirectory());
    }

    @Test
    public void deletingProject_ShouldRemoveGitRepository() throws IOException
    {
        projectService.createProject(testProject);
        assertThat(sut.getRepoDir(testProject.getId()))
                .isDirectoryContaining(f -> f.getName().equals(".git") && f.isDirectory());

        projectService.removeProject(testProject);

        assertThat(sut.getRepoDir(testProject.getId())).doesNotExist();
    }

    @Test
    public void snapshottingProject_ShouldCommitFiles() throws IOException
    {
        projectService.createProject(testProject);

        sut.snapshotCompleteProject(testProject);

        assertThat(sut.getRepoDir(testProject.getId())).isDirectoryContaining(
                f -> f.getName().equals(VersioningServiceImpl.LAYERS) && f.isFile());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = { "de.tudarmstadt.ukp.clarin.webanno.model" })

    public static class TestContext
    {
        private @Autowired ApplicationEventPublisher applicationEventPublisher;
        private @Autowired EntityManager entityManager;

        @Bean
        public RepositoryProperties repositoryProperties()
        {
            return new RepositoryProperties();
        }

        @Bean
        public AnnotationSchemaService annotationSchemaService(
                LayerSupportRegistry aLayerSupportRegistry,
                FeatureSupportRegistry aFeatureSupportRegistry)
        {
            return new AnnotationSchemaServiceImpl(aLayerSupportRegistry, aFeatureSupportRegistry,
                    entityManager);
        }

        @Bean
        public LayerSupportRegistry layerSupportRegistry(
                FeatureSupportRegistry aFeatureSupportRegistry)
        {
            return new LayerSupportRegistryImpl(
                    asList(new SpanLayerSupport(aFeatureSupportRegistry, null, null),
                            new RelationLayerSupport(aFeatureSupportRegistry, null, null),
                            new ChainLayerSupport(aFeatureSupportRegistry, null, null)));
        }

        @Bean
        public FeatureSupportRegistry featureSupportRegistry()
        {
            return new FeatureSupportRegistryImpl(asList(new NumberFeatureSupport(),
                    new BooleanFeatureSupport(), new StringFeatureSupport()));
        }

        @Bean
        public ProjectService projectService(UserDao aUserDao,
                RepositoryProperties aRepositoryProperties,
                @Lazy @Autowired(required = false) List<ProjectInitializer> aInitializerProxy)
        {
            return new ProjectServiceImpl(aUserDao, applicationEventPublisher,
                    aRepositoryProperties, aInitializerProxy);
        }

        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }

        @Bean
        public DocumentService documentService(RepositoryProperties aRepositoryProperties,
                CasStorageService aCasStorageService, ImportExportService aImportExportService,
                ProjectService aProjectService,
                ApplicationEventPublisher aApplicationEventPublisher)
        {
            return new DocumentServiceImpl(aRepositoryProperties, aCasStorageService,
                    aImportExportService, aProjectService, aApplicationEventPublisher);
        }

        @Bean
        VersioningService versioningService(RepositoryProperties aRepositoryProperties,
                AnnotationSchemaService aAnnotationSchemaService, DocumentService aDocumentService,
                ProjectService aProjectservice, CasStorageService aCasStorageService,
                UserDao aUserDao)
        {
            return new VersioningServiceImpl(aRepositoryProperties, aAnnotationSchemaService,
                    aDocumentService, aProjectservice, aCasStorageService, aUserDao);
        }
    }

}
