package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import de.tudarmstadt.ukp.inception.recommendation.imls.core.classificationtool.ClassificationTool;

import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.custom.CustomAnnotationObjectLoader;


//TODO: Assign meaningful ids

public class ExternalClassificationTool
    extends ClassificationTool<Object>

{
    public ExternalClassificationTool()
    {
        super(-1, ExternalClassificationTool.class.getName(),
                new ExternalTrainer(new BaseConfiguration()),
                new ExternalClassifier(new BaseConfiguration()), 
                new CustomAnnotationObjectLoader(),
                true);
    }

    public ExternalClassificationTool(long recommenderId, String feature)
    {
        super(recommenderId, ExternalClassificationTool.class.getName(),
                new ExternalTrainer(new BaseConfiguration()),
                new ExternalClassifier(new BaseConfiguration(feature)),
                new CustomAnnotationObjectLoader(), true);
    }

}
