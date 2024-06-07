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
package de.tudarmstadt.ukp.inception.image.feature;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.image.ExternalImage;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.kendo.ui.form.TextField;

import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;

public class ImageFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 7056716971303216020L;

    private static final Logger LOG = LoggerFactory.getLogger(ImageFeatureEditor.class);

    private AbstractTextComponent<String> field;

    public ImageFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));

        IModel<String> urlModel = getModel().bind("value");

        ExternalImage preview = new ExternalImage("preview");
        // Need to manually set the markup ID of the preview because the feature editor usually
        // resides in a list view causing the markup ID to change on every refresh - and this
        // causes the AJAX behaviors to stop working because they may still refer to an outdated
        // ID
        preview.setMarkupId(aItem.getMarkupId() + "-preview-" + aModel.getObject().feature.getId());
        preview.setDefaultModel(urlModel);
        preview.add(visibleWhen(() -> urlValidator().isValid(urlModel.getObject())));
        preview.setOutputMarkupId(true);
        add(preview);

        field = new TextField<String>("value");
        field.add(urlValidator());
        field.add(OnChangeAjaxBehavior.onChange(_target -> _target.add(preview)));
        add(field);
    }

    private UrlValidator urlValidator()
    {
        return new UrlValidator(new String[] { "http", "https" });
    }

    @Override
    public CompoundPropertyModel<FeatureState> getModel()
    {
        return (CompoundPropertyModel<FeatureState>) super.getModel();
    }

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
}
