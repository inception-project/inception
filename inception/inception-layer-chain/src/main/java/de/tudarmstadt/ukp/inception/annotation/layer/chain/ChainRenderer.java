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
package de.tudarmstadt.ukp.inception.annotation.layer.chain;

import static de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainAdapter.ARC_LABEL_FEATURE;
import static de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainAdapter.SPAN_LABEL_FEATURE;
import static de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainRenderMode.ALWAYS;
import static de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainRenderMode.WHEN_SELECTED;
import static de.tudarmstadt.ukp.inception.schema.api.feature.TypeUtil.getUiLabelText;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.Renderer_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerTraits;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;

public class ChainRenderer
    extends Renderer_ImplBase<ChainAdapter>
{
    private final List<SpanLayerBehavior> behaviors;

    private Type chainType;
    private Feature chainFirst;
    private ChainLayerTraits traits;

    public ChainRenderer(ChainAdapter aTypeAdapter, LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry, List<SpanLayerBehavior> aBehaviors)
    {
        super(aTypeAdapter, aLayerSupportRegistry, aFeatureSupportRegistry);

        if (aBehaviors == null) {
            behaviors = emptyList();
        }
        else {
            var temp = new ArrayList<SpanLayerBehavior>(aBehaviors);
            AnnotationAwareOrderComparator.sort(temp);
            behaviors = temp;
        }

        var typeAdapter = getTypeAdapter();
        traits = typeAdapter.getTraits(ChainLayerTraits.class).orElseGet(ChainLayerTraits::new);
    }

    @Override
    protected boolean typeSystemInit(TypeSystem aTypeSystem)
    {
        var typeAdapter = getTypeAdapter();
        chainType = aTypeSystem.getType(typeAdapter.getChainTypeName());

        if (chainType == null) {
            // If the types are not defined, then we do not need to try and render them because the
            // CAS does not contain any instances of them
            return false;
        }

        chainFirst = chainType.getFeatureByBaseName(typeAdapter.getChainFirstFeatureName());

        return true;
    }

    @Override
    public List<Annotation> selectAnnotationsInWindow(RenderRequest aRequest)
    {
        var cas = aRequest.getCas();

        var typeAdapter = getTypeAdapter();
        return cas.<Annotation> select(typeAdapter.getAnnotationTypeName()) //
                .toList();
    }

    @Override
    public void render(RenderRequest aRequest, List<AnnotationFeature> aFeatures,
            VDocument aResponse)
    {
        if (!checkTypeSystem(aRequest.getCas())) {
            return;
        }

        var typeAdapter = getTypeAdapter();

        // Find the features for the arc and span labels - it is possible that we do not find a
        // feature for arc/span labels because they may have been disabled.
        AnnotationFeature spanLabelFeature = null;
        AnnotationFeature arcLabelFeature = null;
        for (var feature : aFeatures) {
            if (SPAN_LABEL_FEATURE.equals(feature.getName())) {
                spanLabelFeature = feature;
            }
            if (ARC_LABEL_FEATURE.equals(feature.getName())) {
                arcLabelFeature = feature;
            }
        }
        // At this point arc and span feature labels must have been found! If not, the later code
        // will crash.

        // Sorted index mapping annotations to the corresponding rendered spans
        var annoToSpanIdx = new HashMap<AnnotationFS, VSpan>();

        var windowBegin = aResponse.getWindowBegin();
        var windowEnd = aResponse.getWindowEnd();

        var colorIndex = 0;
        // Iterate over the chains
        var chains = aRequest.getCas().select(typeAdapter.getChainTypeName()).asList();
        for (var chainFs : chains) {
            var curLinkFs = (AnnotationFS) chainFs.getFeatureValue(chainFirst);
            var linkNext = curLinkFs.getType()
                    .getFeatureByBaseName(typeAdapter.getLinkNextFeatureName());

            AnnotationFS prevLinkFs = null;

            var links = new ArrayList<AnnotationFS>();
            while (curLinkFs != null) {
                links.add(curLinkFs);
                curLinkFs = (AnnotationFS) curLinkFs.getFeatureValue(linkNext);
            }

            var linksArray = links.toArray(AnnotationFS[]::new);

            // Iterate over the links of the chain
            for (var linkFs : linksArray) {
                // Is link after window? If yes, we can skip the rest of the chain
                if (linkFs.getBegin() >= windowEnd) {
                    break; // Go to next chain
                }

                // Is not overlapping the viewport? We only need links that are actually visible in
                // the viewport
                if (!overlapping(linkFs, windowBegin, windowEnd)) {
                    // prevLinkFs remains null until we enter the window
                    continue; // Go to next link
                }

                // Render span
                {
                    var range = VRange.clippedRange(aResponse, linkFs);

                    if (!range.isPresent()) {
                        continue; // Go to next link
                    }

                    var label = getUiLabelText(typeAdapter, linkFs,
                            (spanLabelFeature != null) ? asList(spanLabelFeature) : emptyList());

                    var span = new VSpan(typeAdapter.getLayer(), linkFs, range.get(), colorIndex,
                            label);
                    annoToSpanIdx.put(linkFs, span);
                    aResponse.add(span);
                }

                // Render arc (we do this on prevLinkFs because then we easily know that the current
                // and last link are within the window ;)
                if (prevLinkFs != null) {
                    String label;

                    if (typeAdapter.isLinkedListBehavior() && arcLabelFeature != null) {
                        // Render arc label
                        label = getUiLabelText(typeAdapter, prevLinkFs, asList(arcLabelFeature));
                    }
                    else {
                        // Render only chain type
                        label = getUiLabelText(typeAdapter, prevLinkFs, emptyList());
                    }

                    // When the request comes from the sidebar, the state is null - we render all
                    // the arcs in that case
                    var renderArc = aRequest.getState() == null || traits.getRenderMode() == ALWAYS
                            || (traits.getRenderMode() == WHEN_SELECTED
                                    && isSelected(aRequest, linksArray));

                    if (renderArc) {
                        aResponse.add(VArc.builder() //
                                .withLayer(typeAdapter.getLayer()) //
                                .withVid(new VID(prevLinkFs, 1, VID.NONE, VID.NONE)) //
                                .withSource(prevLinkFs) //
                                .withTarget(linkFs) //
                                .withEquivalenceSet(colorIndex) //
                                .withLabel(label) //
                                .build());
                    }
                }

                // Render errors if required features are missing
                renderRequiredFeatureErrors(aRequest, aFeatures, linkFs, aResponse);

                prevLinkFs = linkFs;
            }

            // The color index is updated even for chains that have no visible links in the current
            // window because we would like the chain color to be independent of visibility. In
            // particular the color of a chain should not change when switching pages/scrolling.
            colorIndex++;
        }

        for (var behavior : behaviors) {
            behavior.onRender(typeAdapter, aResponse, annoToSpanIdx);
        }
    }

    private boolean isSelected(RenderRequest aRequest, FeatureStructure... aFSes)
    {
        var selection = aRequest.getState().getSelection();

        if (!selection.isSet()) {
            return false;
        }

        for (var fs : aFSes) {
            if (VID.of(fs).getId() == selection.getAnnotation().getId()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<VObject> render(RenderRequest aRequest, List<AnnotationFeature> aFeatures,
            VDocument aResponse, AnnotationFS aFS)
    {
        return Collections.emptyList();
    }
}
