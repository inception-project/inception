package de.tudarmstadt.ukp.inception.recommendation.regexrecommender;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictions;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.dkpro.core.api.datasets.DatasetValidationPolicy.CONTINUE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.datasets.Dataset;
import org.dkpro.core.api.datasets.DatasetFactory;
import org.dkpro.core.io.conll.Conll2002Reader;
import org.dkpro.core.io.conll.Conll2002Reader.ColumnSeparators;
import org.dkpro.core.testing.DkproTestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.gazeteer.GazeteerServiceImpl;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.listener.*;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;

public class RegexrecommenderTest
{
    /*
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private RecommenderContext context;
    private Recommender recommender;
    private RegexRecommenderTraits traits;
    private CasStorageSession casStorageSession;
    private RecommendationRejectedListener rejectedListener;
    private RecommendationAcceptedListener acceptedListener;
    private GazeteerServiceImpl gazeteerService;
    
    @Before
    public void setUp()
    {
        casStorageSession = CasStorageSession.open();
        context = new RecommenderContext();
        recommender = buildRecommender();
        traits = new RegexRecommenderTraits();
        acceptedListener = new RecommendationAcceptedListener();
        rejectedListener = new RecommendationRejectedListener();
    }

    @After
    public void tearDown()
    {
        casStorageSession.close();
    }
    
    @Test
    public void thatTrainingWorks() throws Exception
    {
        RegexRecommender sut = new RegexRecommender(recommender, traits, acceptedListener, rejectedListener);
        List<CAS> casList = loadDevelopmentData();

        sut.train(context, casList);

        assertThat(context.get(StringMatchingRecommender.KEY_MODEL)).as("Model has been set")
                .isNotNull();
    }
    
    @Test
    public void thatPredictionWorks() throws Exception
    {
        RegexRecommender sut = new RegexRecommender(recommender, traits, acceptedListener, rejectedListener);
        List<CAS> casList = loadDevelopmentData();

        CAS cas = casList.get(0);
        RecommenderTestHelper.addScoreFeature(cas, NamedEntity.class, "value");

        sut.train(context, asList(cas));

        sut.predict(context, cas);

        List<NamedEntity> predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions).as("Predictions have been written to CAS").isNotEmpty();

        assertThat(predictions).as("Score is positive")
                .allMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) <= 1.0);

        assertThat(predictions).as("Some score is not perfect")
                .anyMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) < 1.0);

        assertThat(predictions).as("There is no score explanation")
                .allMatch(prediction -> getScoreExplanation(prediction) == null);
    }
    
    private List<CAS> loadDevelopmentData() throws IOException, UIMAException
    {
        try {
            Dataset ds = loader.load("germeval2014-de", CONTINUE);
            return loadData(ds, ds.getDefaultSplit().getDevelopmentFiles());
        }
        catch (Exception e) {
            // Workaround for https://github.com/dkpro/dkpro-core/issues/1469
            assumeThat(e).isNotInstanceOf(FileNotFoundException.class);
            throw e;
        }
    }
    
    private List<CAS> loadData(Dataset ds, File... files) throws UIMAException, IOException
    {
        CollectionReader reader = createReader( //
                Conll2002Reader.class, //
                Conll2002Reader.PARAM_PATTERNS, files, //
                Conll2002Reader.PARAM_LANGUAGE, ds.getLanguage(), //
                Conll2002Reader.PARAM_COLUMN_SEPARATOR, ColumnSeparators.TAB.getName(),
                Conll2002Reader.PARAM_HAS_TOKEN_NUMBER, true, //
                Conll2002Reader.PARAM_HAS_HEADER, true, //
                Conll2002Reader.PARAM_HAS_EMBEDDED_NAMED_ENTITY, true);

        List<CAS> casList = new ArrayList<>();
        int n = 1;
        while (reader.hasNext()) {
            JCas cas = JCasFactory.createJCas();
            reader.getNext(cas.getCas());
            casList.add(cas.getCas());
            casStorageSession.add("testDataCas" + n, EXCLUSIVE_WRITE_ACCESS, cas.getCas());
        }

        return casList;
    }
    
    
    private static Recommender buildRecommender()
    {
        AnnotationLayer layer = new AnnotationLayer();
        layer.setName(NamedEntity.class.getName());

        AnnotationFeature feature = new AnnotationFeature();
        feature.setName("value");

        Recommender recommender = new Recommender();
        recommender.setLayer(layer);
        recommender.setFeature(feature);
        recommender.setMaxRecommendations(3);
        // TODO: Delete this when Evaluation has been implemented
        recommender.setSkipEvaluation(true);

        return recommender;
    }
    */
}
