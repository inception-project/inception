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
package de.tudarmstadt.ukp.inception.diam.model.compactv2;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.diam.model.compactv2.config.DiamCompactV2AutoConfig;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VAnnotationMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VTextMarker;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.support.text.TextUtils;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DiamCompactV2AutoConfig#compactSerializerV2}.
 * </p>
 */
public class CompactSerializerV2Impl
    implements CompactSerializerV2
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String ID = "compact_v2";

    private final AnnotationSchemaProperties properties;

    public CompactSerializerV2Impl(AnnotationSchemaProperties aProperties)
    {
        properties = aProperties;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public CompactAnnotatedText render(VDocument aVDoc, RenderRequest aRequest)
    {
        CompactAnnotatedText aResponse = new CompactAnnotatedText();

        aResponse.setWindow(new CompactRange(aVDoc.getWindowBegin(), aVDoc.getWindowEnd()));

        renderText(aVDoc, aResponse, aRequest);

        renderLayers(aRequest, aResponse, aVDoc);

        return aResponse;
    }

    private void renderLayers(RenderRequest aRequest, CompactAnnotatedText aResponse,
            VDocument aVDoc)
    {
        var layers = new ArrayList<CompactLayer>();

        var vidToAnnotation = new HashMap<VID, CompactAnnotation>();

        for (AnnotationLayer layer : aVDoc.getAnnotationLayers()) {
            if (!properties.isTokenLayerEditable() && Token._TypeName.equals(layer.getName())) {
                continue;
            }

            if (!properties.isSentenceLayerEditable()
                    && Sentence._TypeName.equals(layer.getName())) {
                continue;
            }

            layers.add(new CompactLayer(layer.getId(), layer.getUiName(), layer.getType()));

            for (VSpan vspan : aVDoc.spans(layer.getId())) {
                var cspan = renderSpan(aRequest, vspan);
                aResponse.addSpan(cspan);
                vidToAnnotation.put(cspan.getVid(), cspan);
            }

            for (VArc varc : aVDoc.arcs(layer.getId())) {
                var carc = renderRelation(varc);
                aResponse.addRelation(carc);
                vidToAnnotation.put(carc.getVid(), carc);
            }
        }

        aResponse.setLayers(layers);

        renderMarkers(aRequest, aResponse, aVDoc);
        renderComments(aVDoc, vidToAnnotation);
    }

    private CompactRelation renderRelation(VArc varc)
    {
        CompactRelation carc = new CompactRelation(varc.getLayer(), varc.getVid(),
                getArgument(varc.getSource(), varc.getTarget()), varc.getLabelHint(),
                varc.getColorHint());
        carc.getAttributes().setScore(varc.getScore());
        carc.getAttributes().setHideScore(varc.isHideScore());
        return carc;
    }

    private CompactSpan renderSpan(RenderRequest aRequest, VSpan vspan)
    {
        CompactSpan cspan;
        if (aRequest.isClipSpans()) {
            List<CompactRange> offsets = vspan.getRanges().stream()
                    .map(range -> new CompactRange(range.getBegin(), range.getEnd()))
                    .collect(toList());

            cspan = new CompactSpan(vspan.getLayer(), vspan.getVid(), offsets, vspan.getLabelHint(),
                    vspan.getColorHint());
            cspan.getAttributes().setClippedAtStart(vspan.getRanges().get(0).isClippedAtBegin());
            cspan.getAttributes().setClippedAtEnd(
                    vspan.getRanges().get(vspan.getRanges().size() - 1).isClippedAtEnd());
        }
        else {
            List<CompactRange> offsets = vspan.getRanges().stream().map(
                    range -> new CompactRange(range.getOriginalBegin(), range.getOriginalEnd()))
                    .collect(toList());

            cspan = new CompactSpan(vspan.getLayer(), vspan.getVid(), offsets, vspan.getLabelHint(),
                    vspan.getColorHint());
        }
        cspan.getAttributes().setScore(vspan.getScore());
        cspan.getAttributes().setHideScore(vspan.isHideScore());
        return cspan;
    }

    private void renderMarkers(RenderRequest aRequest, CompactAnnotatedText aResponse,
            VDocument aVDoc)
    {
        for (var marker : aVDoc.getMarkers()) {
            if (marker instanceof VAnnotationMarker) {
                aResponse.addAnnotationMarker(
                        new CompactAnnotationMarker((VAnnotationMarker) marker));
            }
            else if (marker instanceof VTextMarker) {
                var range = ((VTextMarker) marker).getRange();
                List<CompactRange> offsets;
                if (aRequest.isClipSpans()) {
                    offsets = asList(new CompactRange(range.getBegin(), range.getEnd()));
                }
                else {
                    offsets = asList(
                            new CompactRange(range.getOriginalBegin(), range.getOriginalEnd()));
                }
                aResponse.addTextMarker(new CompactTextMarker(offsets, marker.getType()));
            }
        }
    }

    private void renderComments(VDocument aVDoc, HashMap<VID, CompactAnnotation> vidToAnnotation)
    {
        for (var comment : aVDoc.comments()) {
            var cann = vidToAnnotation.get(comment.getVid());
            if (cann == null) {
                LOG.debug("VID {} referenced by comment does not exist: {}", comment.getVid(),
                        comment);
                continue;
            }

            String code;
            switch (comment.getCommentType()) {
            case ERROR:
                code = CompactComment.ERROR;
                break;
            case INFO:
                code = CompactComment.INFO;
                break;
            default:
                throw new IllegalStateException(
                        "Unsupported comment type [" + comment.getCommentType() + "]");
            }
            cann.getAttributes().getComments().add(new CompactComment(comment.getComment(), code));
        }
    }

    /**
     * Argument lists for the arc annotation
     */
    private List<CompactArgument> getArgument(VID aGovernorFs, VID aDependentFs)
    {
        return asList(new CompactArgument("Arg1", aGovernorFs),
                new CompactArgument("Arg2", aDependentFs));
    }

    private void renderText(VDocument aVDoc, CompactAnnotatedText aResponse, RenderRequest aRequest)
    {
        if (!aRequest.isIncludeText()) {
            return;
        }

        String visibleText = aVDoc.getText();
        visibleText = TextUtils.sanitizeVisibleText(visibleText, '\uFFFD');
        aResponse.setText(visibleText);
    }
}
