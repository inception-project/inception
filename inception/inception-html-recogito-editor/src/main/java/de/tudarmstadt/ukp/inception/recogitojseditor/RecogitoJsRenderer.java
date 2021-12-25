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
package de.tudarmstadt.ukp.inception.recogitojseditor;

import java.util.ArrayList;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.TerminalRenderStep;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recogitojseditor.model.WebAnnotation;
import de.tudarmstadt.ukp.inception.recogitojseditor.model.WebAnnotationBodyItem;
import de.tudarmstadt.ukp.inception.recogitojseditor.model.WebAnnotationTarget;
import de.tudarmstadt.ukp.inception.recogitojseditor.model.WebAnnotations;

public class RecogitoJsRenderer implements TerminalRenderStep<WebAnnotations>
{
    @Override
    public WebAnnotations render(AnnotatorState aState, VDocument aVDoc, CAS aCas,
            ColoringStrategy aColoringStrategy)
    {
        WebAnnotations annotations = new WebAnnotations();

        for (AnnotationLayer layer : aVDoc.getAnnotationLayers()) {
            for (VSpan vspan : aVDoc.spans(layer.getId())) {
                String labelText = vspan.getLabelHint();
                String color = vspan.getColorHint();

                WebAnnotation anno = new WebAnnotation();
                anno.setId("#" + vspan.getVid().toString());
                anno.setType("Annotation");
                anno.setTarget(new ArrayList<>());
                anno.getTarget().add(new WebAnnotationTarget(vspan.getRanges().get(0).getBegin(),
                        vspan.getRanges().get(0).getEnd(), null));
                anno.setBody(new ArrayList<>());
                WebAnnotationBodyItem body = new WebAnnotationBodyItem();
                body.setType("TextualBody");
                body.setPurpose("tagging");
                body.setValue(labelText);
                anno.getBody().add(body);
                annotations.add(anno);
            }

            for (VArc varc : aVDoc.arcs(layer.getId())) {
                String labelText = varc.getLabelHint();
                String color = varc.getColorHint();

                WebAnnotation anno = new WebAnnotation();
                anno.setMotivation("linking");
                anno.setType("Annotation");
                anno.setId("#" + varc.getVid().toString());
                anno.setTarget(new ArrayList<>());
                anno.getTarget().add(new WebAnnotationTarget("#" + varc.getSource().toString()));
                anno.getTarget().add(new WebAnnotationTarget("#" + varc.getTarget().toString()));
                anno.setBody(new ArrayList<>());
                WebAnnotationBodyItem body = new WebAnnotationBodyItem();
                body.setType("TextualBody");
                body.setPurpose("tagging");
                body.setValue(labelText);
                anno.getBody().add(body);
                annotations.add(anno);
            }
        }

        return annotations;
    }
}
