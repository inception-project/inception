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
package de.tudarmstadt.ukp.inception.annotation.layer.relation;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRulesConfigurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.LayerTraitsEditor_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.OverlapModeSelect;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.ValidationModeSelect;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerTraits;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;

public class RelationLayerTraitsEditor
    extends LayerTraitsEditor_ImplBase<RelationLayerTraits, RelationLayerSupport>
{
    private static final long serialVersionUID = -9082045435380184514L;

    private @SpringBean AnnotationSchemaProperties annotationEditorProperties;

    public RelationLayerTraitsEditor(String aId, RelationLayerSupport aLayerSupport,
            IModel<AnnotationLayer> aLayer)
    {
        super(aId, aLayerSupport, aLayer);
    }

    @Override
    protected void initializeForm(Form<RelationLayerTraits> aForm)
    {
        aForm.add(new RelationRenderModeSelect("renderMode"));

        aForm.add(new ValidationModeSelect("validationMode", getLayerModel()));

        var overlapMode = new OverlapModeSelect("overlapMode", getLayerModel());
        // Not configurable for layers that attach to tokens (currently that is the only layer on
        // which we use the attach feature)
        overlapMode.add(enabledWhen(() -> getLayerModelObject().getAttachFeature() == null));
        aForm.add(overlapMode);

        aForm.add(new ColoringRulesConfigurationPanel("coloringRules", getLayerModel(),
                getTraitsModel().bind("coloringRules.rules")));

        var crossSentence = new CheckBox("crossSentence");
        crossSentence.setOutputMarkupPlaceholderTag(true);
        crossSentence.setModel(PropertyModel.of(getLayerModel(), "crossSentence"));
        // Not configurable for layers that attach to tokens (currently that is the only layer on
        // which we use the attach feature)
        crossSentence.add(enabledWhen(() -> getLayerModelObject().getAttachFeature() == null));
        aForm.add(crossSentence);
    }
}
