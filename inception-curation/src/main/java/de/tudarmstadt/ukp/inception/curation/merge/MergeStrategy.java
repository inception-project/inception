package de.tudarmstadt.ukp.inception.curation.merge;

import java.util.Map;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;

public interface MergeStrategy 
{

    void merge(AnnotatorState aState, CAS aCas, Map<String, CAS> aUserCases);

    String getUiName();
}
