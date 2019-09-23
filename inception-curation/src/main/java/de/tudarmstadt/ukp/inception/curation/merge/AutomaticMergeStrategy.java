package de.tudarmstadt.ukp.inception.curation.merge;

import java.util.Map;

import org.apache.uima.cas.CAS;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;

@Component
public class AutomaticMergeStrategy
    implements MergeStrategy
{
    
    private String uiName = "Automatic";

    @Override
    public void merge(AnnotatorState aState, CAS aTargetCas, Map<String, CAS> aUserCases)
    {
        // TODO Auto-generated method stub

        // write back
        // update timestamp
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
