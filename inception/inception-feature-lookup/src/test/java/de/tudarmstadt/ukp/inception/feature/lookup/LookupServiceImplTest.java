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
package de.tudarmstadt.ukp.inception.feature.lookup;

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.feature.lookup.config.LookupServicePropertiesImpl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class LookupServiceImplTest
{
    private MockWebServer remoteLookupService;

    private LookupService sut;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new LookupServiceImpl(new LookupServicePropertiesImpl());

        remoteLookupService = new MockWebServer();
        remoteLookupService.start();
    }

    @AfterEach
    void teardown() throws Exception
    {
        remoteLookupService.close();
    }

    @Test
    void thatLookupExistingWorks() throws Exception
    {
        var entry = new LookupEntry("1", "Item 1", "Desc 1");

        remoteLookupService.enqueue(new MockResponse() //
                .setResponseCode(200) //
                .setBody(toJsonString(entry)));

        var traits = new LookupFeatureTraits();
        traits.setRemoteUrl(remoteLookupService.url("/").toString());

        var response = sut.lookup(traits, entry.getIdentifier());

        assertThat(response).get() //
                .isEqualTo(entry);
    }

    @Test
    void thatLookupNonExistingWorks() throws Exception
    {
        remoteLookupService.enqueue(new MockResponse() //
                .setResponseCode(404));

        var traits = new LookupFeatureTraits();
        traits.setRemoteUrl(remoteLookupService.url("/").toString());

        var response = sut.lookup(traits, "does-not-exist");

        assertThat(response).isEmpty();
    }

    @Test
    void thatQueryWorks() throws Exception
    {
        var entries = asList( //
                new LookupEntry("1", "Item 1", "Desc 1"), //
                new LookupEntry("2", "Item 2", "Desc 2"), //
                new LookupEntry("3", "Item 3", "Desc 3"), //
                new LookupEntry("4", "Item 4", "Desc 4"), //
                new LookupEntry("5", "Item 5", "Desc 5"));

        remoteLookupService.enqueue(new MockResponse() //
                .setResponseCode(200) //
                .setBody(toJsonString(entries)));

        var traits = new LookupFeatureTraits();
        traits.setRemoteUrl(remoteLookupService.url("/").toString());

        var response = sut.query(traits, "Item", null);

        assertThat(response).isEqualTo(entries);
    }
}
