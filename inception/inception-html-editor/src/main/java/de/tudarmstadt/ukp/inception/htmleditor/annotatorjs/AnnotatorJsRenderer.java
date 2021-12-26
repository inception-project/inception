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
package de.tudarmstadt.ukp.inception.htmleditor.annotatorjs;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.RenderRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.TerminalRenderStep;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Annotation;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Range;

public class AnnotatorJsRenderer
    implements TerminalRenderStep<List<Annotation>>
{
    public static final String ID = "AnnotatorJsRenderer";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public List<Annotation> render(VDocument aVDoc, RenderRequest aRequest)
    {
        List<Annotation> annotations = new ArrayList<>();

        for (AnnotationLayer layer : aVDoc.getAnnotationLayers()) {
            for (VSpan vspan : aVDoc.spans(layer.getId())) {
                String labelText = vspan.getLabelHint();
                labelText = "[" + layer.getUiName() + "] "
                        + (isBlank(labelText) ? "no label" : labelText);

                Annotation anno = new Annotation();
                anno.setId(vspan.getVid().toString());
                anno.setText(labelText);
                anno.setColor(vspan.getColorHint());
                // Looks like the "quote" is not really required for AnnotatorJS to render the
                // annotation.
                anno.setQuote("");
                anno.setRanges(toRanges(vspan.getRanges()));
                annotations.add(anno);
            }
        }

        return annotations;
    }

    private List<Range> toRanges(List<VRange> aRanges)
    {
        return aRanges.stream().map(r -> new Range(r.getBegin(), r.getEnd()))
                .collect(Collectors.toList());
    }
}
