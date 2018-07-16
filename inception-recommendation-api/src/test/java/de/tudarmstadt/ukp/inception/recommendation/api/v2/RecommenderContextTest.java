/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.api.v2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommenderContext.Key;

public class RecommenderContextTest {
    private final Key<String> KEY = new Key<>("Test");

    private RecommenderContext sut;

    @Before
    public void setUp()
    {
        sut = new RecommenderContext();
    }

    @Test
    public void thatPuttingWorks()
    {
        String value = "INCEpTION";

        sut.put(KEY, value);

        String result = sut.get(KEY);
        assertThat(result).as("Correct value is returned")
            .isEqualTo(value);
    }

    @Test
    public void thatOverwritingWorks()
    {
        String value = "INCEpTION";

        sut.put(KEY, "Dummy");
        sut.put(KEY, value);

        String result = sut.get(KEY);
        assertThat(result).as("Correct value is returned")
            .isEqualTo(value);
    }

    @Test(expected = NoSuchElementException.class)
    public void thatGettingNonexistantKeyThrows()
    {
        sut.get(KEY);
    }

    @Test
    public void thatNameSpacingWorks()
    {
        String value1 = "INCEpTION";
        String value2 = "Tyrannotator";

        sut.put(KEY, value1);
        RecommenderContext namespaced = sut.getView("TestNameSpace");
        namespaced.put(KEY, value2);

        assertThat(sut.get(KEY)).as("Correct value is returned for global context")
            .isEqualTo(value1);
        assertThat(namespaced.get(KEY))
            .as("Correct value is returned for namespaced context")
            .isEqualTo(value2);
    }

}
