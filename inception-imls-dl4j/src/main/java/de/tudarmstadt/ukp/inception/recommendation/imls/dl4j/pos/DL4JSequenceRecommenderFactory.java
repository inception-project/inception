package de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos;

import org.apache.uima.cas.CAS;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommendationEngineFactory;

@Component
public class DL4JSequenceRecommenderFactory
    implements RecommendationEngineFactory
{

    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to nonexisting recommendation tools.
    public static final String ID = 
        "de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos.DL4JPosClassificationTool";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Token Sequence Classifier (DL4J)";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aLayer == null || aFeature == null) {
            return false;
        }
        
        return aLayer.isLockToTokenOffset() && "span".equals(aLayer.getType())
            && CAS.TYPE_NAME_STRING.equals(aFeature.getType());
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender) {
        DL4JSequenceRecommenderTraits traits = new DL4JSequenceRecommenderTraits();
        return new DL4JSequenceRecommender(aRecommender, traits);
    }
}
