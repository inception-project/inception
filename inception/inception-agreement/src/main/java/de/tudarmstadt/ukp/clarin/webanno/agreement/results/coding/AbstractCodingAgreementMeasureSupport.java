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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SENTENCES;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.NONE;
import static java.util.Arrays.asList;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementResult_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PerDocumentAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.perdoc.PerDocumentAgreementTable;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;

public abstract class AbstractCodingAgreementMeasureSupport<T extends DefaultAgreementTraits>
    extends AgreementMeasureSupport_ImplBase<T, FullCodingAgreementResult, ICodingAnnotationStudy>
{
    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aFeature == null) {
            return false;
        }

        if (!asList(SpanLayerSupport.TYPE, RelationLayerSupport.TYPE,
                DocumentMetadataLayerSupport.TYPE).contains(aLayer.getType())) {
            return false;
        }

        if (!asList(SINGLE_TOKEN, TOKENS, SENTENCES).contains(aLayer.getAnchoringMode())) {
            return false;
        }

        if (aFeature != null) {
            // Link features are supported (because the links generate sub-positions in the diff
            // but multi-value primitives (e.g. multi-value strings) are not supported
            if (aFeature.getMultiValueMode() != MultiValueMode.NONE
                    && aFeature.getLinkMode() == NONE) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Panel createResultsPanel(String aId, IModel<? extends AgreementResult_ImplBase> aResults,
            DefaultAgreementTraits aDefaultAgreementTraits)
    {
        if (aResults.getObject() instanceof PairwiseAgreementResult) {
            return new PairwiseCodingAgreementTable(aId, (IModel) aResults,
                    aDefaultAgreementTraits);
        }

        if (aResults.getObject() instanceof PerDocumentAgreementResult) {
            return new PerDocumentAgreementTable(aId, (IModel) aResults, aDefaultAgreementTraits);
        }

        return new EmptyPanel(aId);
    }
}
