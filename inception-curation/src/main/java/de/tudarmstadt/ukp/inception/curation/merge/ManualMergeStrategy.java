package de.tudarmstadt.ukp.inception.curation.merge;

import java.util.Map;

import org.apache.uima.cas.CAS;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;

@Component
public class ManualMergeStrategy implements MergeStrategy
{
    
    private String uiName = "Manual";

    @Override
    public void merge(AnnotatorState aState, CAS aCas, Map<String, CAS> aUserCases)
    {
        // Do nothing
    }
    
    public String getUiName()
    {
        return uiName;
    }

    public void setUiName(String aUiName)
    {
        uiName = aUiName;
    }

}
