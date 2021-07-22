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
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;

public class RecommenderContextTest
{
    private final Key<String> KEY = new Key<>("Test");

    private RecommenderContext sut;

    @BeforeEach
    public void setUp()
    {
        sut = new RecommenderContext();
    }

    @Test
    public void thatPuttingWorks()
    {
        String value = "INCEpTION";

        sut.put(KEY, value);

        Optional<String> result = sut.get(KEY);
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).as("Correct value is returned").isEqualTo(value);
    }

    @Test
    public void thatOverwritingWorks()
    {
        String value = "INCEpTION";

        sut.put(KEY, "Dummy");
        sut.put(KEY, value);

        Optional<String> result = sut.get(KEY);
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).as("Correct value is returned").isEqualTo(value);
    }

    @Test
    public void thatGettingNonexistantKeyThrows()
    {
        Optional<String> result = sut.get(KEY);
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void thatContextStartsOutNotReady()
    {
        assertThat(sut.isClosed()).isFalse();
    }

    @Test
    public void thatContextCanBeMarkedAsReady()
    {
        sut.close();
        assertThat(sut.isClosed()).isTrue();
    }
}
