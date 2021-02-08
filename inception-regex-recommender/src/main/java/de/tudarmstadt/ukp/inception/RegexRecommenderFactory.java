package de.tudarmstadt.ukp.inception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;

// tag::classDefinition[]
@Component
public class RegexRecommenderFactory
    extends RecommendationEngineFactoryImplBase<RegexRecommenderTraits>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID =
        "org.RegexRecommender";
    
    @Autowired
    private RecommendationAcceptedListener acceptedListener;
    @Autowired
    private RecommendationRejectedListener rejectedListener;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {    
        RegexRecommender regexRecommender = new RegexRecommender(aRecommender,
                                                                 this.acceptedListener,
                                                                 this.rejectedListener);
        return regexRecommender;
    }

    @Override
    public String getName()
    {
        return "RegexRecommender";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {    
        return true;
    }
}
// end::classDefinition[]

