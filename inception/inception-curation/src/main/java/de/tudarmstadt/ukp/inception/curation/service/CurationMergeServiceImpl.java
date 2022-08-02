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
package de.tudarmstadt.ukp.inception.curation.service;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiffSingle;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getDiffAdapters;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_ROLE_AS_LABEL;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.StopWatch;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.merge.CasMerge;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationServiceAutoConfiguration#curationMergeService}.
 * </p>
 */
public class CurationMergeServiceImpl
    implements CurationMergeService
{
    private final static Logger LOG = getLogger(lookup().lookupClass());

    private final AnnotationSchemaService annotationService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public CurationMergeServiceImpl(AnnotationSchemaService aAnnotationService,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        annotationService = aAnnotationService;
        applicationEventPublisher = aApplicationEventPublisher;
    }

    @Override
    public Set<LogMessage> mergeCasses(SourceDocument aDocument, String aTargetCasUserName,
            CAS aTargetCas, Map<String, CAS> aCassesToMerge, MergeStrategy aMergeStrategy)
        throws UIMAException
    {
        List<AnnotationLayer> layers = annotationService.listSupportedLayers(aDocument.getProject())
                .stream() //
                .filter(AnnotationLayer::isEnabled) //
                .collect(toList());

        return mergeCasses(aDocument, aTargetCasUserName, aTargetCas, aCassesToMerge,
                aMergeStrategy, layers);
    }

    @Override
    public Set<LogMessage> mergeCasses(SourceDocument aDocument, String aTargetCasUserName,
            CAS aTargetCas, Map<String, CAS> aCassesToMerge, MergeStrategy aMergeStrategy,
            List<AnnotationLayer> aLayers)
        throws UIMAException
    {
        List<DiffAdapter> adapters = getDiffAdapters(annotationService, aLayers);

        // If the token/sentence layer is not editable, we do not offer curation of the tokens.
        // Instead the tokens are obtained from a random template CAS when initializing the CAS - we
        // assume here that the tokens have never been modified.
        if (!annotationService.isSentenceLayerEditable(aDocument.getProject())) {
            adapters.removeIf(adapter -> Sentence._TypeName.equals(adapter.getType()));
        }

        if (!annotationService.isTokenLayerEditable(aDocument.getProject())) {
            adapters.removeIf(adapter -> Token._TypeName.equals(adapter.getType()));
        }

        DiffResult diff;
        try (StopWatch watch = new StopWatch(LOG, "CasDiff")) {
            diff = doDiffSingle(adapters, LINK_ROLE_AS_LABEL, aCassesToMerge, 0, MAX_VALUE)
                    .toResult();
        }

        try (StopWatch watch = new StopWatch(LOG, "CasMerge")) {
            CasMerge casMerge = new CasMerge(annotationService, applicationEventPublisher);
            casMerge.setMergeStrategy(aMergeStrategy);
            return casMerge.reMergeCas(diff, aDocument, aTargetCasUserName, aTargetCas,
                    aCassesToMerge);
        }
    }
}
