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
package de.tudarmstadt.ukp.inception.recommendation.config;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.session.SessionRegistry;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.recommendation.RecommendationEditorExtension;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.evaluation.EvaluationSimulationPageMenuItem;
import de.tudarmstadt.ukp.inception.recommendation.exporter.RecommenderExporter;
import de.tudarmstadt.ukp.inception.recommendation.log.RecommendationAcceptedEventAdapter;
import de.tudarmstadt.ukp.inception.recommendation.log.RecommendationRejectedEventAdapter;
import de.tudarmstadt.ukp.inception.recommendation.log.RecommenderDeletedEventAdapter;
import de.tudarmstadt.ukp.inception.recommendation.log.RecommenderEvaluationResultEventAdapter;
import de.tudarmstadt.ukp.inception.recommendation.metrics.RecommendationMetricsImpl;
import de.tudarmstadt.ukp.inception.recommendation.project.RecommenderProjectSettingsPanelFactory;
import de.tudarmstadt.ukp.inception.recommendation.service.LearningRecordServiceImpl;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationServiceImpl;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommenderFactoryRegistryImpl;
import de.tudarmstadt.ukp.inception.recommendation.sidebar.RecommendationSidebarFactory;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;

/**
 * Provides all back-end Spring beans for the external search functionality.
 */
@Configuration
@ConditionalOnProperty(prefix = "recommender", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecommenderServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    @Autowired
    public RecommendationService recommendationService(SessionRegistry aSessionRegistry,
            UserDao aUserRepository, RecommenderFactoryRegistry aRecommenderFactoryRegistry,
            SchedulingService aSchedulingService, AnnotationSchemaService aAnnoService,
            DocumentService aDocumentService, LearningRecordService aLearningRecordService,
            ProjectService aProjectService, ApplicationEventPublisher aApplicationEventPublisher)
    {
        return new RecommendationServiceImpl(aSessionRegistry, aUserRepository,
                aRecommenderFactoryRegistry, aSchedulingService, aAnnoService, aDocumentService,
                aLearningRecordService, aProjectService, entityManager, aApplicationEventPublisher);
    }

    @Bean
    public LearningRecordService learningRecordService()
    {
        return new LearningRecordServiceImpl(entityManager);
    }

    @ConditionalOnProperty(prefix = "recommender.evaluation-page", //
            name = "enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public EvaluationSimulationPageMenuItem evaluationSimulationPageMenuItem()
    {
        return new EvaluationSimulationPageMenuItem();
    }

    @Bean
    @Autowired
    public RecommenderExporter recommenderExporter(AnnotationSchemaService aAnnotationService,
            RecommendationService aRecommendationService)
    {
        return new RecommenderExporter(aAnnotationService, aRecommendationService);
    }

    @Bean
    public RecommendationAcceptedEventAdapter recommendationAcceptedEventAdapter()
    {
        return new RecommendationAcceptedEventAdapter();
    }

    @Bean
    public RecommendationRejectedEventAdapter recommendationRejectedEventAdapter()
    {
        return new RecommendationRejectedEventAdapter();
    }

    @Bean
    public RecommenderDeletedEventAdapter recommenderDeletedEventAdapter()
    {
        return new RecommenderDeletedEventAdapter();
    }

    @Bean
    public RecommenderEvaluationResultEventAdapter recommenderEvaluationResultEventAdapter()
    {
        return new RecommenderEvaluationResultEventAdapter();
    }

    @Bean
    public RecommenderProjectSettingsPanelFactory recommenderProjectSettingsPanelFactory()
    {
        return new RecommenderProjectSettingsPanelFactory();
    }

    @Bean
    public RecommendationSidebarFactory recommendationSidebarFactory()
    {
        return new RecommendationSidebarFactory();
    }

    @Bean(name = RecommendationEditorExtension.BEAN_NAME)
    @Autowired
    public RecommendationEditorExtension recommendationEditorExtension(
            AnnotationSchemaService aAnnotationService,
            RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            FeatureSupportRegistry aFsRegistry, DocumentService aDocumentService,
            UserDao aUserService)
    {
        return new RecommendationEditorExtension(aAnnotationService, aRecommendationService,
                aLearningRecordService, aApplicationEventPublisher, aFsRegistry, aDocumentService,
                aUserService);
    }

    @Bean
    public RecommenderFactoryRegistry recommenderFactoryRegistry(
            @Lazy @Autowired(required = false) List<RecommendationEngineFactory> aExtensions)
    {
        return new RecommenderFactoryRegistryImpl(aExtensions);
    }

    @Bean
    @Autowired
    @ConditionalOnProperty(prefix = "monitoring.metrics", name = "enabled", havingValue = "true")
    public RecommendationMetricsImpl recommendationMetricsImpl(RecommendationService aRecService)
    {
        return new RecommendationMetricsImpl(aRecService);

    }
}
