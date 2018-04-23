package de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.custom;

import java.io.IOException; 
import java.io.UnsupportedEncodingException; 
 
import java.util.LinkedList; 
import java.util.List; 
import java.util.function.Function; 
 
import org.apache.uima.jcas.JCas; 
import org.xml.sax.SAXException; 
 
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS; 
 
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject; 
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.AnnotationObjectLoader; 
import de.tudarmstadt.ukp.inception.recommendation.imls.util.CasUtil; 
 
import org.apache.commons.io.output.ByteArrayOutputStream; 
import org.apache.uima.cas.impl.XmiCasSerializer; 
import org.apache.uima.cas.CAS; 
import org.apache.uima.cas.Feature; 
import org.apache.uima.cas.Type; 
 
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 

public class CustomAnnotationObjectLoader
    implements AnnotationObjectLoader
{
    private static Logger LOG = LoggerFactory.getLogger(CustomAnnotationObjectLoader.class);

    @Override
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

}
