package de.tudarmstadt.ukp.inception.recommendation.api;

import java.util.List;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommendationEngineFactory;

@Component
public interface RecommendationEngineFactoryRegistry
{
    List<RecommendationEngineFactory> getFactories(AnnotationLayer aLayer,
                                                   AnnotationFeature aFeature);
    RecommendationEngineFactory getFactory(String aId);
}
