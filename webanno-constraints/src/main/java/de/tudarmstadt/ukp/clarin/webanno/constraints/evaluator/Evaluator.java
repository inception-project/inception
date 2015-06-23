package de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator;

import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;

/***
 * Interface for getting values from rules
 * @author aakash
 *
 */
public interface Evaluator
{
    /**
     * 
     * @param aContext
     * @param aFeature the 
     * @param parsedConstraints the object containing object generated after parsing rules
     * @return list of possible values based on rules
     * @throws UIMAException
     */
    List<PossibleValue> generatePossibleValues(FeatureStructure aContext, String aFeature,
            ParsedConstraints parsedConstraints)
        throws UIMAException;
}
