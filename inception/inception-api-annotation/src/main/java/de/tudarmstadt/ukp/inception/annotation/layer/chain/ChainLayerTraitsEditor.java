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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.LayerTraitsEditor_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.AnchoringModeSelect;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.OverlapModeSelect;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.ValidationModeSelect;

public class ChainLayerTraitsEditor
    extends LayerTraitsEditor_ImplBase<ChainLayerTraits, ChainLayerSupport>
{
    private static final long serialVersionUID = -9082045435380184514L;

    public ChainLayerTraitsEditor(String aId, ChainLayerSupport aLayerSupport,
            IModel<AnnotationLayer> aLayer)
    {
        super(aId, aLayerSupport, aLayer);
    }

    @Override
    protected void initializeForm(Form<ChainLayerTraits> aForm)
    {
        aForm.add(new ValidationModeSelect("validationMode", getLayerModel()));

        aForm.add(new AnchoringModeSelect("anchoringMode", getLayerModel()));

        aForm.add(new OverlapModeSelect("overlapMode", getLayerModel()));

        CheckBox linkedListBehavior = new CheckBox("linkedListBehavior");
        linkedListBehavior.setOutputMarkupId(true);
        linkedListBehavior.setModel(PropertyModel.of(getLayerModel(), "linkedListBehavior"));
        aForm.add(linkedListBehavior);

        CheckBox crossSentence = new CheckBox("crossSentence");
        crossSentence.setOutputMarkupPlaceholderTag(true);
        crossSentence.setModel(PropertyModel.of(getLayerModel(), "crossSentence"));
        crossSentence.add(visibleWhen(() -> !isBlank(getLayerModelObject().getType())));
        aForm.add(crossSentence);

        TextArea<String> onClickJavascriptAction = new TextArea<String>("onClickJavascriptAction");
        onClickJavascriptAction
                .setModel(PropertyModel.of(getLayerModel(), "onClickJavascriptAction"));
        onClickJavascriptAction.add(new AttributeModifier("placeholder",
                "alert($PARAM.PID + ' ' + $PARAM.PNAME + ' ' + $PARAM.DOCID + ' ' + "
                        + "$PARAM.DOCNAME + ' ' + $PARAM.fieldname);"));
        aForm.add(onClickJavascriptAction);
    }
}
