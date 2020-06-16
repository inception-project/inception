/*
 * Copyright 2020
 * ENP-China, Aix-Marseille University
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.solr.SolrSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.solr.SolrSearchProviderFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import de.tudarmstadt.ukp.inception.externalsearch.solr.traits.SolrSearchProviderTraits;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Server not publicly accessible")
public class SolrSearchProviderTest {
    private SolrSearchProvider sut;
    private DocumentRepository repo;
    private SolrSearchProviderTraits traits;

    @Before
    public void setup()
    {
        sut = new SolrSearchProvider();

        repo = new DocumentRepository("test", null);

        traits = new SolrSearchProviderTraits();
        traits.setRemoteUrl("http://localhost:8983/solr");
        traits.setIndexName("techproducts");
        traits.setSearchPath("/select");
        traits.setDefaultField("id");
        traits.setTextField("features");
    }

    @Test
    public void thatQueryWorks() throws Exception
    {
        String query = "0*";

        List<ExternalSearchResult> results = sut.executeQuery(repo, traits, query);

        //System.out.println(results.get(0).getDocumentTitle());

        assertThat(results).isNotEmpty();
    }

    @Test
    public void thatDocumentTextCanBeRetrieved() throws Exception
    {
        String documentText = sut.getDocumentText(repo, traits,
            traits.getIndexName(), "SP2514N");
        //System.out.println(documentText);
        assertThat(documentText).isNotNull();
    }

    @Test
    public void randomOrderWork() throws Exception
    {
        String query = "*";
        traits.setRandomOrder(true);

        traits.setSeed(2);
        List<ExternalSearchResult> results = sut.executeQuery(repo, traits, query);
        //System.out.println(results.get(0).getDocumentTitle());
        String result1 = results.get(0).getDocumentTitle();

        traits.setSeed(3);
        results = sut.executeQuery(repo, traits, query);
        //System.out.println(results.get(0).getDocumentTitle());
        String result2 = results.get(0).getDocumentTitle();

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    public void highlightingWork() throws Exception
    {
        String query = "the";
        traits.setDefaultField(traits.getTextField());
        traits.setResultSize(5);

        List<ExternalSearchResult> results = sut.executeQuery(repo, traits, query);
       // System.out.println(results.get(0).getHighlights().get(0).getHighlight());

        assertThat(results.get(0).getHighlights().get(0).getHighlight()).isNotNull();
    }
}