/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.curation.merge;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_ROLE_AS_LABEL;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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
    public boolean equals(final Object other)
    {
        if (!(other instanceof AutomaticMergeStrategy)) {
            return false;
        }
        AutomaticMergeStrategy castOther = (AutomaticMergeStrategy) other;
        return new EqualsBuilder().append(uiName, castOther.uiName).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(uiName).toHashCode();
    }

    @Override
    /**
     * Merges annotations that the given users (excluding the current one) agree on 
     * into the current user's (might be CURATION_USER) CAS
     */
    public void merge(AnnotatorState aState, CAS aTargetCas, Map<String, CAS> aUserCasses)
    {
        // FIXME: should merging not overwrite the current users annos? (can result in deleting the
        // users annos!!!)
        // prepare merged cas
        List<AnnotationLayer> layers = aState.getAnnotationLayers();
        List<Type> entryTypes = SuggestionBuilder.getEntryTypes(aTargetCas, layers,
                annotationService);
        DiffResult diff = CasDiff
                .doDiffSingle(annotationService, aState.getProject(), entryTypes,
                        LINK_ROLE_AS_LABEL, aUserCasses, 0, aTargetCas.getDocumentText().length())
                .toResult();
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
