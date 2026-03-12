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
package de.tudarmstadt.ukp.inception.recommendation.service;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommenderPredictionSources.RECOMMENDER_SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.PredictionsSource;

/**
 * Tests for multi-source prediction management. Validates that predictions from different sources
 * (e.g., recommenders, assistant) can coexist without interfering with each other.
 */
@ContextConfiguration(classes = SpringConfig.class)
@Transactional
@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
class MultiSourcePredictionsTest
{
    private @Autowired TestEntityManager testEntityManager;

    private RecommendationServiceImpl sut;

    private Project project;
    private User user;

    // Placeholder for assistant source until AssistantPredictionSources enum is implemented
    private static final PredictionsSource TEST_ASSISTANT_SOURCE = new PredictionsSource()
    {
    };

    @BeforeEach
    void setUp()
    {
        sut = new RecommendationServiceImpl(null, null, null, null, null, null, null,
                testEntityManager.getEntityManager());

        project = createProject("Test Project");
        user = createUser("testUser");
    }

    @Test
    void switchPredictions_shouldMergeSourcesNotReplace()
    {
        // Setup: Put predictions for RECOMMENDER_SOURCE
        var recommenderPreds = new Predictions(user, user.getUsername(), project);
        sut.putIncomingPredictions(user, project, RECOMMENDER_SOURCE, recommenderPreds);
        sut.switchPredictions(user.getUsername(), project);

        // Verify recommender predictions are active
        var active1 = sut.getPredictions(user, project, RECOMMENDER_SOURCE);
        assertThat(active1).isEqualTo(recommenderPreds);

        // Add assistant predictions
        var assistantPreds = new Predictions(user, user.getUsername(), project);
        sut.putIncomingPredictions(user, project, TEST_ASSISTANT_SOURCE, assistantPreds);
        sut.switchPredictions(user.getUsername(), project);

        // Critical: Both should be active - switching assistant predictions must not clobber
        // recommender predictions
        var activeRecommender = sut.getPredictions(user, project, RECOMMENDER_SOURCE);
        var activeAssistant = sut.getPredictions(user, project, TEST_ASSISTANT_SOURCE);
        assertThat(activeRecommender).as("Recommender predictions should still be active")
                .isEqualTo(recommenderPreds);
        assertThat(activeAssistant).as("Assistant predictions should now be active")
                .isEqualTo(assistantPreds);
    }

    @Test
    void getPredictions_shouldReturnAllSources()
    {
        var recommenderPreds = new Predictions(user, user.getUsername(), project);
        var assistantPreds = new Predictions(user, user.getUsername(), project);

        sut.putIncomingPredictions(user, project, RECOMMENDER_SOURCE, recommenderPreds);
        sut.putIncomingPredictions(user, project, TEST_ASSISTANT_SOURCE, assistantPreds);
        sut.switchPredictions(user.getUsername(), project);

        var allPredictions = sut.getPredictions(user, project);
        assertThat(allPredictions).hasSize(2);
        assertThat(allPredictions).containsKeys(RECOMMENDER_SOURCE, TEST_ASSISTANT_SOURCE);
        assertThat(allPredictions.get(RECOMMENDER_SOURCE)).isEqualTo(recommenderPreds);
        assertThat(allPredictions.get(TEST_ASSISTANT_SOURCE)).isEqualTo(assistantPreds);
    }

    @Test
    void switchPredictions_withNoIncoming_shouldPreserveActive()
    {
        var recommenderPreds = new Predictions(user, user.getUsername(), project);
        sut.putIncomingPredictions(user, project, RECOMMENDER_SOURCE, recommenderPreds);
        sut.switchPredictions(user.getUsername(), project);

        // Switch again with no new incoming predictions
        var switched = sut.switchPredictions(user.getUsername(), project);

        assertThat(switched).as("Should return false when no incoming predictions").isFalse();
        assertThat(sut.getPredictions(user, project, RECOMMENDER_SOURCE))
                .as("Active predictions should be preserved").isEqualTo(recommenderPreds);
    }

    @Test
    void getPredictions_bySource_shouldReturnCorrectPredictions()
    {
        var recommenderPreds = new Predictions(user, user.getUsername(), project);
        var assistantPreds = new Predictions(user, user.getUsername(), project);

        sut.putIncomingPredictions(user, project, RECOMMENDER_SOURCE, recommenderPreds);
        sut.putIncomingPredictions(user, project, TEST_ASSISTANT_SOURCE, assistantPreds);
        sut.switchPredictions(user.getUsername(), project);

        assertThat(sut.getPredictions(user, project, RECOMMENDER_SOURCE))
                .as("Should return recommender predictions").isEqualTo(recommenderPreds);
        assertThat(sut.getPredictions(user, project, TEST_ASSISTANT_SOURCE))
                .as("Should return assistant predictions").isEqualTo(assistantPreds);
        assertThat(sut.getPredictions(user.getUsername(), project, RECOMMENDER_SOURCE))
                .as("String username overload should work").isEqualTo(recommenderPreds);
    }

    @Test
    void putIncomingPredictions_concurrentSources_shouldBeThreadSafe() throws Exception
    {
        var latch = new CountDownLatch(2);
        var recommenderPreds = new Predictions(user, user.getUsername(), project);
        var assistantPreds = new Predictions(user, user.getUsername(), project);

        var t1 = new Thread(() -> {
            sut.putIncomingPredictions(user, project, RECOMMENDER_SOURCE, recommenderPreds);
            latch.countDown();
        });

        var t2 = new Thread(() -> {
            sut.putIncomingPredictions(user, project, TEST_ASSISTANT_SOURCE, assistantPreds);
            latch.countDown();
        });

        t1.start();
        t2.start();
        assertThat(latch.await(5, TimeUnit.SECONDS))
                .as("Both threads should complete within timeout").isTrue();

        sut.switchPredictions(user.getUsername(), project);

        var allPredictions = sut.getPredictions(user, project);
        assertThat(allPredictions).as("Both sources should be present after concurrent updates")
                .hasSize(2);
        assertThat(allPredictions).containsKeys(RECOMMENDER_SOURCE, TEST_ASSISTANT_SOURCE);
    }

    @Test
    void switchPredictions_multipleSourcesInSequence_shouldAccumulateAll()
    {
        // First batch: recommender predictions
        var recommenderPreds1 = new Predictions(user, user.getUsername(), project);
        sut.putIncomingPredictions(user, project, RECOMMENDER_SOURCE, recommenderPreds1);
        sut.switchPredictions(user.getUsername(), project);

        // Second batch: assistant predictions
        var assistantPreds1 = new Predictions(user, user.getUsername(), project);
        sut.putIncomingPredictions(user, project, TEST_ASSISTANT_SOURCE, assistantPreds1);
        sut.switchPredictions(user.getUsername(), project);

        // Third batch: new recommender predictions (update)
        var recommenderPreds2 = new Predictions(user, user.getUsername(), project);
        sut.putIncomingPredictions(user, project, RECOMMENDER_SOURCE, recommenderPreds2);
        sut.switchPredictions(user.getUsername(), project);

        var allPredictions = sut.getPredictions(user, project);
        assertThat(allPredictions).hasSize(2);
        assertThat(allPredictions.get(RECOMMENDER_SOURCE))
                .as("Recommender predictions should be updated to latest")
                .isEqualTo(recommenderPreds2);
        assertThat(allPredictions.get(TEST_ASSISTANT_SOURCE))
                .as("Assistant predictions should remain unchanged").isEqualTo(assistantPreds1);
    }

    @Test
    void getIncomingPredictions_bySource_shouldReturnCorrectPredictions()
    {
        var recommenderPreds = new Predictions(user, user.getUsername(), project);
        var assistantPreds = new Predictions(user, user.getUsername(), project);

        sut.putIncomingPredictions(user, project, RECOMMENDER_SOURCE, recommenderPreds);
        sut.putIncomingPredictions(user, project, TEST_ASSISTANT_SOURCE, assistantPreds);

        assertThat(sut.getIncomingPredictions(user, project, RECOMMENDER_SOURCE))
                .as("Should return incoming recommender predictions").isEqualTo(recommenderPreds);
        assertThat(sut.getIncomingPredictions(user, project, TEST_ASSISTANT_SOURCE))
                .as("Should return incoming assistant predictions").isEqualTo(assistantPreds);
    }

    @Test
    void switchPredictions_shouldClearIncomingAfterMerge()
    {
        var recommenderPreds = new Predictions(user, user.getUsername(), project);
        sut.putIncomingPredictions(user, project, RECOMMENDER_SOURCE, recommenderPreds);

        assertThat(sut.getIncomingPredictions(user, project, RECOMMENDER_SOURCE))
                .as("Incoming predictions should exist before switch").isNotNull();

        sut.switchPredictions(user.getUsername(), project);

        assertThat(sut.getIncomingPredictions(user, project, RECOMMENDER_SOURCE))
                .as("Incoming predictions should be cleared after switch").isNull();
        assertThat(sut.getPredictions(user, project, RECOMMENDER_SOURCE))
                .as("Predictions should now be active").isEqualTo(recommenderPreds);
    }

    private Project createProject(String aName)
    {
        return testEntityManager.persist(Project.builder().withName(aName).build());
    }

    private User createUser(String aUsername)
    {
        return testEntityManager.persist(User.builder().withUsername(aUsername).build());
    }
}
