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
package de.tudarmstadt.ukp.inception.externalsearch.opensearch;

import static java.lang.String.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;

import java.util.List;

import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.codelibs.opensearch.runner.OpenSearchRunnerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.LegacyESVersion;
import org.opensearch.client.Requests;
import org.opensearch.cluster.health.ClusterHealthStatus;
import org.opensearch.common.Priority;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.Settings.Builder;
import org.opensearch.common.unit.TimeValue;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.opensearch.traits.OpenSearchProviderTraits;

public class OpenSearchProviderTest
{
    static {
        // Disable asserts for LegacyESVersion because we use a slightly higher version of Lucene
        // than OpenSearch expects
        LegacyESVersion.class.getClassLoader()
                .setClassAssertionStatus(LegacyESVersion.class.getName(), false);
    }

    private OpenSearchProvider sut;
    private DocumentRepository repo;
    private OpenSearchProviderTraits traits;
    private OpenSearchRunner osRunner;

    @BeforeEach
    public void setup()
    {
        osRunner = new OpenSearchRunner()
        {
            @Override
            public ClusterHealthStatus ensureYellow(final String... indices)
            {
                var actionGet = client().admin().cluster() //
                        .health(Requests.clusterHealthRequest(indices)
                                .waitForNoRelocatingShards(true).waitForYellowStatus()
                                .waitForEvents(Priority.LANGUID))
                        .actionGet(TimeValue.timeValueSeconds(60));
                if (actionGet.isTimedOut()) {
                    throw new OpenSearchRunnerException("ensureYellow timed out, cluster state:\n"
                            + "\n" + client().admin().cluster().prepareState().get().getState()
                            + "\n" + client().admin().cluster().preparePendingClusterTasks().get(),
                            actionGet);
                }
                return actionGet.getStatus();
            }
        };
        osRunner.onBuild(new OpenSearchRunner.Builder()
        {
            @Override
            public void build(int aIndex, Builder aBuilder)
            {
                aBuilder.put("logger.level", "WARN");
                // This should hopefully allow running tests on machines with less than the
                // default 90% high watermark disk space free (e.g on a 1TB drive less than 100 GB).
                aBuilder.put("cluster.routing.allocation.disk.threshold_enabled", false);
            }
        }).build(newConfigs());

        var port = osRunner.client().settings().get("http.port");
        osRunner.ensureYellow();

        osRunner.createIndex("test", (Settings) null);
        osRunner.insert("test", "1", join("\n", //
                "{", //
                "  'metadata': {", //
                "    'language': 'en',", //
                "    'source': 'My favourite document collection',", //
                "    'timestamp': '2011/11/11 11:11',", //
                "    'uri': 'http://the.internet.com/my/document/collection/document1.txt',", //
                "    'title': 'Cool Document Title'", //
                "  },", //
                "  'doc': {", //
                "    'text': 'This is a test document'", //
                "  }", //
                "}").replace('\'', '"'));

        sut = new OpenSearchProvider();

        repo = new DocumentRepository("dummy", null);

        traits = new OpenSearchProviderTraits();
        traits.setRemoteUrl("http://localhost:" + port);
        traits.setIndexName("test");
        traits.setSearchPath("_search");
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        try {
            osRunner.close();
        }
        finally {
            osRunner.clean();
        }
    }

    @Test
    public void thatQueryWorks() throws Exception
    {
        List<ExternalSearchResult> results = sut.executeQuery(repo, traits, "document");

        System.out.println(results);

        assertThat(results).isNotEmpty();
    }

    @Test
    public void thatDocumentTextCanBeRetrieved() throws Exception
    {
        String documentText = sut.getDocumentText(repo, traits, "test", "1");
        assertThat(documentText).isNotNull();
    }
}
