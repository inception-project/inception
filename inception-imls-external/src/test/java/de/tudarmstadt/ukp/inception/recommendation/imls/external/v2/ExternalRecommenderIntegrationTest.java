package de.tudarmstadt.ukp.inception.recommendation.imls.external.v2;


import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2002Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.ExternalRecommenderTraits;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class ExternalRecommenderIntegrationTest
{
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private Recommender recommender;
    private RecommenderContext context;
    private ExternalRecommender sut;
    private ExternalRecommenderTraits traits;
    private MockWebServer server;

    @Before
    public void setUp() throws Exception
    {
        recommender = buildRecommender();
        context = new RecommenderContext();

        traits = new ExternalRecommenderTraits();
        sut = new ExternalRecommender(recommender, traits);

        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws Exception
    {
        server.shutdown();
    }

    @Test
    public void thatPredictingWorks() throws Exception
    {
        String url = server.url("/predict").toString();
        traits.setRemoteUrl(url);
        server.enqueue(new MockResponse().setBody("test"));

        CAS cas = loadDevelopmentData().get(0);

        sut.predict(context, cas);
    }

    private List<CAS> loadDevelopmentData() throws IOException, UIMAException
    {
        Dataset ds = loader.load("germeval2014-de");
        return loadData(ds, ds.getDefaultSplit().getDevelopmentFiles());
    }

    private List<CAS> loadData(Dataset ds, File ... files) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(Conll2002Reader.class,
            Conll2002Reader.PARAM_PATTERNS, files,
            Conll2002Reader.PARAM_LANGUAGE, ds.getLanguage(),
            Conll2002Reader.PARAM_COLUMN_SEPARATOR, Conll2002Reader.ColumnSeparators.TAB.getName(),
            Conll2002Reader.PARAM_HAS_TOKEN_NUMBER, true,
            Conll2002Reader.PARAM_HAS_HEADER, true,
            Conll2002Reader.PARAM_HAS_EMBEDDED_NAMED_ENTITY, true);

        List<CAS> casList = new ArrayList<>();
        while (reader.hasNext()) {
            JCas cas = JCasFactory.createJCas();
            reader.getNext(cas.getCas());
            casList.add(cas.getCas());
        }
        return casList;
    }

    private static Recommender buildRecommender()
    {
        AnnotationLayer layer = new AnnotationLayer();
        layer.setName("TestLayer");

        Recommender recommender = new Recommender();
        recommender.setLayer(layer);
        recommender.setFeature("value");

        return recommender;
    }
}
