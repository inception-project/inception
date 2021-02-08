package de.tudarmstadt.ukp.inception.recommendation.regexrecommender;

import org.apache.wicket.model.IModel;


import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.config.RegexRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.gazeteer.GazeteerServiceImpl;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.listener.RecommendationAcceptedListener;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.listener.RecommendationRejectedListener;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.settings.RegexRecommenderTraitsEditor;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RegexRecommenderAutoConfiguration#regexRecommenderFactory}.
 * </p>
 */
public class RegexRecommenderFactory
    extends RecommendationEngineFactoryImplBase<RegexRecommenderTraits>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID =
        "de.tudarmstadt.ukp.inception.recommendation.RegexRecommender";
 
    private final GazeteerServiceImpl gazeteerService;
    private final RecommendationAcceptedListener acceptedListener;
    private final RecommendationRejectedListener rejectedListener;
    private final RegexSet regexSet;
    
    public RegexRecommenderFactory(GazeteerServiceImpl aGazeteerService,
                                    RecommendationAcceptedListener aAcceptedListener,
                                    RecommendationRejectedListener aRejectedListener,
                                    RegexSet aRegexSet)
    {        
        gazeteerService = aGazeteerService;
        acceptedListener = aAcceptedListener;
        rejectedListener = aRejectedListener;
        regexSet = aRegexSet;
    }
    
    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {   
        
        RegexRecommenderTraits traits = new RegexRecommenderTraits();
        return new RegexRecommender(aRecommender, traits,
                this.acceptedListener,
                this.rejectedListener,
                this.gazeteerService,
                this.regexSet);
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
    
    @Override
    public RegexRecommenderTraitsEditor createTraitsEditor(String aId,
            IModel<Recommender> aModel)
    {
        return new RegexRecommenderTraitsEditor(aId, aModel);
    }

    @Override
    public RegexRecommenderTraits createTraits()
    {
        return new RegexRecommenderTraits();
    }
}

