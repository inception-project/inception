package de.tudarmstadt.ukp.inception.curation.merge;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_ROLE_AS_LABEL;

import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMerge;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.inception.curation.CurationService;

@Component("automaticStartegy")
public class AutomaticMergeStrategy
    implements MergeStrategy
{
    private String uiName = "Automatic";
    
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired CurationService curationService;
    private @Autowired AnnotationSchemaService annotationService;

    @Override
    public void merge(AnnotatorState aState, CAS aTargetCas, Map<String, CAS> aUserCasses)
    {
        // prepare merged cas
        List<AnnotationLayer> layers = aState.getAnnotationLayers();
        List<Type> entryTypes = SuggestionBuilder.getEntryTypes(aTargetCas, layers,
                annotationService);
        DiffResult diff = CasDiff.doDiffSingle(annotationService, aState.getProject(), entryTypes,
                LINK_ROLE_AS_LABEL, aUserCasses, 0, aTargetCas.getDocumentText().length());
        CasMerge casMerge = new CasMerge(annotationService);
        try {
            casMerge.reMergeCas(diff, aState.getDocument(), aState.getUser().getUsername(),
                    aTargetCas, aUserCasses);
        }
        catch (AnnotationException | UIMAException e) {
            log.warn(String.format("Could not merge CAS for user %s and document %d",
                    aState.getUser().getUsername(), aState.getDocument().getId()));
            e.printStackTrace();
        }
        // write back and update timestamp
        curationService.writeCurationCas(aTargetCas, aState, aState.getProject().getId());
    }

    @Override
    public String getUiName()
    {
        return uiName;
    }

    public void setUiName(String aUiName)
    {
        uiName = aUiName;
    }
}
