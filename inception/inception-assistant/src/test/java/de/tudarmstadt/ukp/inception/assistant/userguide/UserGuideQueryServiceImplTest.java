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
package de.tudarmstadt.ukp.inception.assistant.userguide;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.OllamaRecommenderTraits.DEFAULT_OLLAMA_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.assistant.config.AssistantDocumentIndexProperties;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantDocumentIndexPropertiesImpl;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantPropertiesImpl;
import de.tudarmstadt.ukp.inception.assistant.embedding.EmbeddingServiceImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClientImpl;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils;

@ExtendWith(MockitoExtension.class)
class UserGuideQueryServiceImplTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Mock SchedulingService schedulingService;
    private AssistantProperties assistantProperties;
    private AssistantDocumentIndexProperties assistantDocumentIndexProperties;
    private OllamaClient ollamaClient;
    private UserGuideQueryServiceImpl sut;
    private EmbeddingServiceImpl embeddingService;

    private static @TempDir Path applicationHome;

    @BeforeAll
    static void checkIfOllamaIsRunning()
    {
        assumeThat(HttpTestUtils.checkURL(DEFAULT_OLLAMA_URL)).isTrue();
        System.setProperty("inception.home", applicationHome.toString());
    }

    @BeforeEach
    void setup()
    {
        assistantDocumentIndexProperties = new AssistantDocumentIndexPropertiesImpl();
        assistantProperties = new AssistantPropertiesImpl(assistantDocumentIndexProperties);
        ollamaClient = new OllamaClientImpl();
        embeddingService = new EmbeddingServiceImpl(assistantProperties, ollamaClient);
        sut = new UserGuideQueryServiceImpl(assistantProperties, schedulingService,
                embeddingService);
    }

    @AfterEach
    void tearDown()
    {
        sut.destroy();
    }

    @Test
    void testSimpleIndexAndQuery() throws Exception
    {
        try (var iw = sut.getIndexWriter()) {
            sut.indexBlocks(iw, //
                    "Waldi is a dog.", //
                    "Miau is a cat.", //
                    "Tweety is a bird.");
        }
        sut.markIndexUpToDate();

        assertThat(sut.query("katze", 10, 0.0)) //
                .containsExactly( //
                        "Miau is a cat.", //
                        "Waldi is a dog.", //
                        "Tweety is a bird.");
    }
}
