package de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.custom;

import java.util.LinkedList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.AnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.imls.util.CasUtil; 

public class CustomAnnotationObjectLoader
    implements AnnotationObjectLoader
{
    public List<List<AnnotationObject>> loadAnnotationObjects(JCas aJCas, String featureName)
    {
        List<List<AnnotationObject>> result = new LinkedList<>();

        if (aJCas == null) {
            return result;
        }
        
        CAS cas = aJCas.getCas();
        //TODO: Pass Layer as parameter
        Type annotationType = org.apache.uima.fit.util.CasUtil.getType(cas , "webanno.custom.ArgLayer");
        Feature feature = annotationType.getFeatureByBaseName(featureName);
        
        result = CasUtil.loadCustomAnnotatedSentences(aJCas, annotationType, feature);

        return result;
    }

    @Override
    public List<List<AnnotationObject>> loadAnnotationObjects(JCas aJCas)
    {
        return loadAnnotationObjects(aJCas, "ArgF");
    }

    @Override
    public List<List<AnnotationObject>> loadAnnotationObjectsForTesting(JCas aJCas)
    {
        throw new UnsupportedOperationException("Write unit tests please!");
    }
}
