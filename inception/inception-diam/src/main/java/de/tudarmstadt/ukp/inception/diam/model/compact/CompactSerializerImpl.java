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
package de.tudarmstadt.ukp.inception.diam.model.compact;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
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
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#compactSerializer}.
 * </p>
 */
public class CompactSerializerImpl
    implements CompactSerializer
{
    public static final String ID = "compact";

    private final AnnotationSchemaProperties properties;

    public CompactSerializerImpl(AnnotationSchemaProperties aProperties)
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

        renderLayers(aResponse, aVDoc);

        return aResponse;
    }

    private void renderLayers(CompactAnnotatedText aResponse, VDocument aVDoc)
    {
        for (AnnotationLayer layer : aVDoc.getAnnotationLayers()) {
            if (!properties.isTokenLayerEditable()
                    && Token.class.getName().equals(layer.getName())) {
                continue;
            }

            if (!properties.isSentenceLayerEditable()
                    && Sentence.class.getName().equals(layer.getName())) {
                continue;
            }

            for (VSpan vspan : aVDoc.spans(layer.getId())) {
                List<CompactRange> offsets = vspan.getRanges().stream()
                        .map(range -> new CompactRange(range.getBegin(), range.getEnd()))
                        .collect(toList());

                CompactSpan entity = new CompactSpan(vspan.getVid(), offsets, vspan.getLabelHint(),
                        vspan.getColorHint());
                entity.getAttributes()
                        .setClippedAtStart(vspan.getRanges().get(0).isClippedAtBegin());
                entity.getAttributes().setClippedAtEnd(
                        vspan.getRanges().get(vspan.getRanges().size() - 1).isClippedAtEnd());

                aResponse.addSpan(entity);
            }

            for (VArc varc : aVDoc.arcs(layer.getId())) {
                CompactRelation arc = new CompactRelation(varc.getVid(),
                        getArgument(varc.getSource(), varc.getTarget()), varc.getLabelHint(),
                        varc.getColorHint());
                aResponse.addRelation(arc);
            }
        }

        for (var marker : aVDoc.getMarkers()) {
            if (marker instanceof VAnnotationMarker) {
                aResponse.addAnnotationMarker(
                        new CompactAnnotationMarker((VAnnotationMarker) marker));
            }
            else if (marker instanceof VTextMarker) {
                aResponse.addTextMarker(new CompactTextMarker((VTextMarker) marker));
            }
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
