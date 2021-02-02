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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRulesConfigurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.behaviors.AnchoringModeSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.behaviors.OverlapModeSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.behaviors.ValidationModeSelect;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm;

public class SpanLayerTraitsEditor
    extends LayerTraitsEditor_ImplBase<SpanLayerTraits, SpanLayerSupport>
{
    private static final long serialVersionUID = -9082045435380184514L;

    public SpanLayerTraitsEditor(String aId, SpanLayerSupport aLayerSupport,
            IModel<AnnotationLayer> aLayer)
    {
        super(aId, aLayerSupport, aLayer);
    }

    @Override
    protected void initializeForm(Form<SpanLayerTraits> aForm)
    {
        aForm.add(new ColoringRulesConfigurationPanel("coloringRules", getLayerModel(),
                getTraitsModel().bind("coloringRules.rules")));

        aForm.add(new ValidationModeSelect("validationMode", getLayerModel()));

        OverlapModeSelect overlapMode = new OverlapModeSelect("overlapMode", getLayerModel());
        overlapMode.add(LambdaBehavior.onConfigure(_this -> {
            AnnotationLayer layer = getLayerModelObject();
            _this.setEnabled(
                    // Surface form must be non-stacking for CONLL-U writer to work.
                    !SurfaceForm.class.getName().equals(layer.getName()) &&
            // Not configurable for layers that attach to tokens (currently that is
            // the only layer on which we use the attach feature)
            layer.getAttachFeature() == null);
        }));
        aForm.add(overlapMode);

        AnchoringModeSelect anchoringMode = new AnchoringModeSelect("anchoringMode",
                getLayerModel());
        anchoringMode.add(LambdaBehavior.onConfigure(_this -> {
            AnnotationLayer layer = getLayerModelObject();
            _this.setEnabled(
                    // Surface form must be locked to token boundaries for CONLL-U writer to work.
                    !SurfaceForm.class.getName().equals(layer.getName()) &&
            // Not configurable for layers that attach to tokens (currently
            // that is the only layer on which we use the attach feature)
            layer.getAttachFeature() == null);
        }));
        aForm.add(anchoringMode);

        CheckBox crossSentence = new CheckBox("crossSentence");
        crossSentence.setOutputMarkupPlaceholderTag(true);
        crossSentence.setModel(PropertyModel.of(getLayerModel(), "crossSentence"));
        crossSentence.add(LambdaBehavior.onConfigure(_this -> {
            AnnotationLayer layer = getLayerModelObject();
            _this.setEnabled(
                    // Surface form must be locked to token boundaries for CONLL-U writer
                    // to work.
                    !SurfaceForm.class.getName().equals(layer.getName()) &&
            // Not configurable for layers that attach to tokens (currently that
            // is the only layer on which we use the attach feature)
            layer.getAttachFeature() == null);
        }));
        aForm.add(crossSentence);

        TextArea<String> onClickJavascriptAction = new TextArea<String>("onClickJavascriptAction");
        onClickJavascriptAction
                .setModel(PropertyModel.of(getLayerModel(), "onClickJavascriptAction"));
        onClickJavascriptAction.add(new AttributeModifier("placeholder",
                "alert($PARAM.PID + ' ' + $PARAM.PNAME + ' ' + $PARAM.DOCID + ' ' + "
                        + "$PARAM.DOCNAME + ' ' + $PARAM.fieldname);"));
        aForm.add(onClickJavascriptAction);

        CheckBox showTextInHover = new CheckBox("showTextInHover");
        showTextInHover.setOutputMarkupPlaceholderTag(true);
        showTextInHover.setModel(PropertyModel.of(getLayerModel(), "showTextInHover"));
        // Surface form must be locked to token boundaries for CONLL-U writer to work.
        showTextInHover.add(enabledWhen(
                () -> !SurfaceForm.class.getName().equals(getLayerModelObject().getName())));
        aForm.add(showTextInHover);
    }
}
