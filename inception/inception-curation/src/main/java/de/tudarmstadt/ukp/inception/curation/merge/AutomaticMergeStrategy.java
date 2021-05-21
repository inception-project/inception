/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMerge;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.curation.CurationService;
import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationServiceAutoConfiguration#automaticMergeStrategy}.
 * </p>
 */
public class AutomaticMergeStrategy
    implements MergeStrategy
{
    public static final String BEAN_NAME = "automaticStrategy";

    private final static String UI_NAME = "Automatic";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CurationService curationService;
    private final AnnotationSchemaService annotationService;

    public AutomaticMergeStrategy(CurationService aCurationService,
            AnnotationSchemaService aAnnotationService)
    {
        curationService = aCurationService;
        annotationService = aAnnotationService;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof AutomaticMergeStrategy)) {
            return false;
        }
        AutomaticMergeStrategy castOther = (AutomaticMergeStrategy) other;
        return new EqualsBuilder().append(UI_NAME, castOther.UI_NAME).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(UI_NAME).toHashCode();
    }

    @Override
    /**
     * Merges annotations that the given users (excluding the current one) agree on into the current
     * user's (might be CURATION_USER) CAS
     */
    public void merge(AnnotatorState aState, CAS aTargetCas, Map<String, CAS> aUserCasses,
            boolean aMergeIncomplete)
    {
        // FIXME: should merging not overwrite the current users annos? (can result in deleting the
        // users annos!!!), currently fixed by warn message to user
        // prepare merged cas
        List<AnnotationLayer> layers = aState.getAnnotationLayers();
        List<DiffAdapter> adapters = CasDiff.getDiffAdapters(annotationService, layers);
        DiffResult diff = CasDiff.doDiffSingle(adapters, LINK_ROLE_AS_LABEL, aUserCasses, 0,
                aTargetCas.getDocumentText().length()).toResult();
        CasMerge casMerge = new CasMerge(annotationService);
        try {
            casMerge.setMergeIncompleteAnnotations(aMergeIncomplete);
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
        return UI_NAME;
    }
}
