package de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator;

import java.util.List;
import java.util.Map;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;

public class ConstraintsVerifier
    implements Verifiable
{

    @Override
    public boolean verify(FeatureStructure featureStructure, ParsedConstraints parsedConstraints)
    {

        boolean isOk = false;
        Type type = featureStructure.getType();
        for (Feature feature : type.getFeatures()) {
            if (feature.getRange().isPrimitive()) {
                String scopeName = featureStructure.getFeatureValueAsString(feature);
                List<Rule> rules = parsedConstraints.getScopeByName(scopeName).getRules();

                // Check if all the feature values are ok according to the
                // rules;
            }
            else {
                // Here some recursion would be in order
            }
        }
        return isOk;
    }

}
