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
package de.tudarmstadt.ukp.inception.annotation.feature.string;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.ConstraintsInUseIndicator;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

public abstract class TextFeatureEditorBase
    extends FeatureEditor
{
    private static final long serialVersionUID = -3499366171559879681L;

    private static final Logger LOG = LoggerFactory.getLogger(TextFeatureEditorBase.class);

    @SuppressWarnings("rawtypes")
    private FormComponent field;
    private boolean hideUnconstrainedFeature;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    public TextFeatureEditorBase(String aId, MarkupContainer aItem, IModel<FeatureState> aModel)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));

        field = createInputField();
        add(field);

        // Checks whether hide un-constraint feature is enabled or not
        hideUnconstrainedFeature = getModelObject().feature.isHideUnconstraintFeature();
        add(new ConstraintsInUseIndicator("textIndicator", getModel()));
    }

    protected abstract FormComponent createInputField();

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        LOG.trace("TextFeatureEditor(path: " + getPageRelativePath() + ", "
                + getModelObject().feature.getUiName() + ": " + getModelObject().value + ")");
    }

    @Override
    public FormComponent<?> getFocusComponent()
    {
        return field;
    }

    /**
     * Hides feature if "Hide un-constraint feature" is enabled and constraint rules are applied and
     * feature doesn't match any constraint rule
     */
    @Override
    public void onConfigure()
    {
        super.onConfigure();

        // if enabled and constraints rule execution returns anything other than green
        setVisible(!hideUnconstrainedFeature || (getModelObject().indicator.isAffected()
                && getModelObject().indicator.getStatusColor().equals("green")));
    }

    public StringFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature)
    {
        FeatureSupport<?> fs = featureSupportRegistry.findExtension(aAnnotationFeature)
                .orElseThrow();
        return (StringFeatureTraits) fs.readTraits(aAnnotationFeature);
    }
}
