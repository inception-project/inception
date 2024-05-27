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
package de.tudarmstadt.ukp.inception.active.learning.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningService;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningServiceImpl;
import de.tudarmstadt.ukp.inception.active.learning.log.ActiveLearningRecommendationEventAdapter;
import de.tudarmstadt.ukp.inception.active.learning.log.ActiveLearningSuggestionOfferedAdapter;
import de.tudarmstadt.ukp.inception.active.learning.sidebar.ActiveLearningSidebarFactory;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

@Configuration
@AutoConfigureAfter(RecommenderServiceAutoConfiguration.class)
@ConditionalOnBean(RecommendationService.class)
@ConditionalOnProperty( //
        prefix = "recommender.active-learning", //
        name = "enabled", //
        havingValue = "true", //
        matchIfMissing = true)
public class ActiveLearningAutoConfiguration
{
    @Bean
    public ActiveLearningService activeLearningService(DocumentService aDocumentService,
            RecommendationService aRecommendationService, UserDao aUserDao,
            LearningRecordService aLearningHistoryService, AnnotationSchemaService aSchemaService,
            ApplicationEventPublisher aApplicationEventPublisher,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        return new ActiveLearningServiceImpl(aDocumentService, aRecommendationService, aUserDao,
                aLearningHistoryService, aSchemaService, aApplicationEventPublisher,
                aFeatureSupportRegistry);
    }

    @Bean
    public ActiveLearningRecommendationEventAdapter activeLearningRecommendationEventAdapter()
    {
        return new ActiveLearningRecommendationEventAdapter();
    }

    @Bean
    public ActiveLearningSuggestionOfferedAdapter activeLearningSuggestionOfferedAdapter()
    {
        return new ActiveLearningSuggestionOfferedAdapter();
    }

    @Bean
    public ActiveLearningSidebarFactory activeLearningSidebarFactory(
            RecommendationService aRecommendationService, PreferencesService aPreferencesService,
            UserDao aUserService)
    {
        return new ActiveLearningSidebarFactory(aRecommendationService);
    }
}
