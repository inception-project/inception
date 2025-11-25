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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRulesConfigurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.LayerTraitsEditor_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.AnchoringModeSelect;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.OverlapModeSelect;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.ValidationModeSelect;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerTraits;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;

public class SpanLayerTraitsEditor
    extends LayerTraitsEditor_ImplBase<SpanLayerTraits, SpanLayerSupport>
{
    private static final long serialVersionUID = -9082045435380184514L;

    private @SpringBean AnnotationSchemaProperties annotationEditorProperties;

    public SpanLayerTraitsEditor(String aId, SpanLayerSupport aLayerSupport,
            IModel<AnnotationLayer> aLayer)
    {
        super(aId, aLayerSupport, aLayer);
    }

    @Override
    protected void initializeForm(Form<SpanLayerTraits> aForm)
    {
        var coloringRules = new ColoringRulesConfigurationPanel("coloringRules", getLayerModel(),
                getTraitsModel().bind("coloringRules.rules"));
        coloringRules.add(visibleWhen(this::isColoringRulesVisible));
        aForm.add(coloringRules);

        aForm.add(new ValidationModeSelect("validationMode", getLayerModel()));

        var overlapMode = new OverlapModeSelect("overlapMode", getLayerModel());
        overlapMode.add(enabledWhen(this::isOverlapModeEditable));
        aForm.add(overlapMode);

        var anchoringMode = new AnchoringModeSelect("anchoringMode", getLayerModel());
        anchoringMode.add(enabledWhen(this::isAnchoringModeEditable));
        aForm.add(anchoringMode);

        var crossSentence = new CheckBox("crossSentence");
        crossSentence.setOutputMarkupPlaceholderTag(true);
        crossSentence.setModel(PropertyModel.of(getLayerModel(), "crossSentence"));
        crossSentence.add(enabledWhen(this::isCrossSentenceModeEditable));
        crossSentence.add(visibleWhen(this::isCrossSentenceModeVisible));
        aForm.add(crossSentence);

        var showTextInHover = new CheckBox("showTextInHover");
        showTextInHover.setOutputMarkupPlaceholderTag(true);
        showTextInHover.setModel(PropertyModel.of(getLayerModel(), "showTextInHover"));
        aForm.add(showTextInHover);
    }

    private boolean isColoringRulesVisible()
    {
        AnnotationLayer layer = getLayerModelObject();

        // Segmentation layers do not really have a label, so maybe we do not need coloring rules
        // for them
        if (Sentence.class.getName().equals(layer.getName())
                || Token.class.getName().equals(layer.getName())) {
            return false;
        }

        return true;
    }

    private boolean isCrossSentenceModeVisible()
    {
        AnnotationLayer layer = getLayerModelObject();

        // For segmentation layers, we do not allow changing the cross-sentence mode
        if (Sentence.class.getName().equals(layer.getName())
                || Token.class.getName().equals(layer.getName())) {
            return false;
        }

        return true;
    }

    private boolean isCrossSentenceModeEditable()
    {
        AnnotationLayer layer = getLayerModelObject();

        // Surface form must be locked to token boundaries for CONLL-U writer to work.
        if (SurfaceForm.class.getName().equals(layer.getName())) {
            return false;
        }

        // Not configurable for layers that attach to tokens (currently that is the only layer on
        // which we use the attach feature)
        if (layer.getAttachFeature() != null) {
            return false;
        }

        // For segmentation layers, we do not allow changing the cross-sentence mode
        if (Sentence.class.getName().equals(layer.getName())
                || Token.class.getName().equals(layer.getName())) {
            return false;
        }

        return true;
    }

    private boolean isOverlapModeEditable()
    {
        AnnotationLayer layer = getLayerModelObject();

        // Surface form must be non-stacking for CONLL-U writer to work.
        if (SurfaceForm.class.getName().equals(layer.getName())) {
            return false;
        }

        // Not configurable for layers that attach to tokens (currently that is the only layer on
        // which we use the attach feature)
        if (layer.getAttachFeature() != null) {
            return false;
        }

        // For segmentation layers, we do not allow changing the overlap mode
        if (Sentence.class.getName().equals(layer.getName())
                || Token.class.getName().equals(layer.getName())) {
            return false;
        }

        return true;
    }

    private boolean isAnchoringModeEditable()
    {
        AnnotationLayer layer = getLayerModelObject();

        // Surface form must be locked to token boundaries for CONLL-U writer to work.
        if (SurfaceForm.class.getName().equals(layer.getName())) {
            return false;
        }

        // Not configurable for layers that attach to tokens (currently that is the only layer on
        // which we use the attach feature)
        if (layer.getAttachFeature() != null) {
            return false;
        }

        // For segmentation layers, we do not allow changing the anchoring mode
        if (Sentence.class.getName().equals(layer.getName())
                || Token.class.getName().equals(layer.getName())) {
            return false;
        }

        return true;
    }
}
