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

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;

public abstract class AbstractCodingAgreementMeasureSupport<T extends DefaultAgreementTraits>
    extends AgreementMeasureSupport_ImplBase<T, FullCodingAgreementResult, ICodingAnnotationStudy>
{
    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        AnnotationLayer layer = aFeature.getLayer();

        return asList(SpanLayerSupport.TYPE, RelationLayerSupport.TYPE).contains(layer.getType())
                && asList(SINGLE_TOKEN, TOKENS, SENTENCES).contains(layer.getAnchoringMode())
                // Link features are supported (because the links generate sub-positions in the diff
                // but multi-value primitives (e.g. multi-value strings) are not supported
                && (aFeature.getMultiValueMode() == MultiValueMode.NONE
                        || aFeature.getLinkMode() != NONE);
    }

    @Override
    public Panel createResultsPanel(String aId, IModel<PairwiseAnnotationResult> aResults)
    {
        return new PairwiseCodingAgreementTable(aId, aResults);
    }
}
