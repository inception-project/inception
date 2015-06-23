package de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator;

import java.util.List;
import java.util.Map;

import org.apache.uima.cas.FeatureStructure;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;

public interface Verifiable
{

    boolean verify(FeatureStructure featureStructure, ParsedConstraints parsedConstraints);

}
