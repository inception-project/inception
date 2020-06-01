import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.solr.SolrSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.solr.SolrSearchProviderFactory;
import org.junit.Before;
import org.junit.Test;
import de.tudarmstadt.ukp.inception.externalsearch.solr.traits.SolrSearchProviderTraits;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


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
        traits.setIndexName("boorman");
        traits.setDefaultField("id");
        traits.setTextField("text");
    }

    @Test
    public void thatQueryWorks() throws Exception
    {
        String query = "*";

        List<ExternalSearchResult> results = sut.executeQuery(repo, traits, query);

        //System.out.println(results.get(0).getDocumentTitle());

        assertThat(results).isNotEmpty();
    }

    @Test
    public void thatDocumentTextCanBeRetrieved() throws Exception
    {
        String documentText = sut.getDocumentText(repo, traits,
            "boorman", "B003");
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
        traits.setDefaultField("text");
        traits.setResultSize(5);

        List<ExternalSearchResult> results = sut.executeQuery(repo, traits, query);
       // System.out.println(results.get(0).getHighlights().get(0).getHighlight());

        assertThat(results.get(0).getHighlights().get(0).getHighlight()).isNotNull();
    }

}