package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import java.util.HashMap;
import java.util.List;

import de.tudarmstadt.ukp.inception.recommendation.imls.conf.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.trainer.Trainer;

public class ExternalTrainer
    extends Trainer<Object>
{

    private HashMap<String, String> trainedModel;
    
    public ExternalTrainer(ClassifierConfiguration<Object> conf)
    {
        super(conf);
    }

    @Override
    public Object train(List<List<AnnotationObject>> trainingDataIncrement)
    {
        if (trainedModel == null) {
            reconfigure();
        }
        return trainedModel;
    }

    @Override
    public boolean saveModel()
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public Object loadModel()
    {
        if (trainedModel == null) {
            reconfigure();
        }
        return trainedModel;
    }

    @Override
    public void reconfigure()
    {
        trainedModel = new HashMap<>();

    }
}
