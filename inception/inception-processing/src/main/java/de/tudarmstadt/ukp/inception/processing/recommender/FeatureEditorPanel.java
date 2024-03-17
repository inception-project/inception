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
package de.tudarmstadt.ukp.inception.processing.recommender;

import static java.util.Collections.emptyList;

import java.util.List;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.wicket.DescriptionTooltipBehavior;

public class FeatureEditorPanel
    extends GenericPanel<List<FeatureState>>
{
    private static final long serialVersionUID = -3186504694280146998L;

    private static final String CID_EDITOR = "editor";
    private static final String CID_FEATURE_EDITORS = "featureEditors";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    public FeatureEditorPanel(String aId)
    {
        this(aId, Model.ofList(emptyList()));
    }

    public FeatureEditorPanel(String aId, IModel<List<FeatureState>> aFeatureStates)
    {
        super(aId, aFeatureStates);

        setOutputMarkupId(true);

        queue(createFeaturesList(CID_FEATURE_EDITORS, aFeatureStates));
    }

    private ListView<FeatureState> createFeaturesList(String aId,
            IModel<List<FeatureState>> aFeatureStates)
    {
        return new ListView<FeatureState>(aId, aFeatureStates)
        {
            private static final long serialVersionUID = -1139622234318691941L;

            @Override
            protected void populateItem(ListItem<FeatureState> item)
            {
                final FeatureState featureState = item.getModelObject();
                final FeatureEditor editor;

                // Look up a suitable editor and instantiate it
                var featureSupport = featureSupportRegistry.findExtension(featureState.feature)
                        .orElseThrow();
                editor = featureSupport.createEditor(CID_EDITOR, this, null, null, item.getModel());

                // Add tooltip on label
                var tooltipTitle = new StringBuilder();
                tooltipTitle.append(featureState.feature.getUiName());
                if (featureState.feature.getTagset() != null) {
                    tooltipTitle.append(" (");
                    tooltipTitle.append(featureState.feature.getTagset().getName());
                    tooltipTitle.append(')');
                }

                var labelComponent = editor.getLabelComponent();
                labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                labelComponent.add(new DescriptionTooltipBehavior(tooltipTitle.toString(),
                        featureState.feature.getDescription()));

                // We need to enable the markup ID here because we use it during the AJAX behavior
                // that automatically saves feature editors on change/blur.
                // Check addAnnotateActionBehavior.
                editor.setOutputMarkupPlaceholderTag(true);

                // Ensure that markup IDs of feature editor focus components remain constant
                // across refreshes of the feature editor panel. This is required to restore the
                // focus.
                editor.getFocusComponent().setOutputMarkupId(true);
                editor.getFocusComponent().setMarkupId(FeatureEditorPanel.this.getMarkupId()
                        + editor.getModelObject().feature.getId());

                item.add(editor);
            }
        };
    }
}
