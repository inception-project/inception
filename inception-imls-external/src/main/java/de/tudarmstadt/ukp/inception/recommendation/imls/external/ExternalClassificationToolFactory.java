package de.tudarmstadt.ukp.inception.recommendation.imls.external;


import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.classificationtool.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.classificationtool.ClassificationToolFactory;

@Component
public class ExternalClassificationToolFactory
    implements ClassificationToolFactory<Object>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = 
            "de.tudarmstadt.ukp.inception.recommendation.imls.external.ExternalClassificationTool";
    
    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public ClassificationTool<Object> createTool(long aRecommenderId, String aFeature,
            int aMaxPredictions)
    {
        return new ExternalClassificationTool(aRecommenderId, aFeature);
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aLayer == null || aFeature == null) {
            return false;
        }
        
        return (aLayer.isLockToTokenOffset() || aLayer.isMultipleTokens())
                && aLayer.isCrossSentence() && "span".equals(aLayer.getType());
    }

    @Override
    public String getName()
    {
        return ExternalClassificationTool.class.getName();
    }
}
