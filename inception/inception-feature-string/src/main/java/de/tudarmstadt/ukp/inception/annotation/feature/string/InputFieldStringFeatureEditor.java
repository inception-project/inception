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

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.UrlValidator;
import org.wicketstuff.kendo.ui.form.TextField;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsPanel;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.SuggestionStatePanel;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class InputFieldStringFeatureEditor
    extends TextFeatureEditorBase
{
    private static final long serialVersionUID = 8686646370500180943L;

    private ExternalLink openIri;
    private UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });

    public InputFieldStringFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel, AnnotationActionHandler aHandler)
    {
        super(aId, aItem, aModel);

        var feat = getModelObject().feature;
        var traits = readFeatureTraits(feat);

        var featureValueAsString = aModel.map(FeatureState::getValue).map(Object::toString);

        openIri = new ExternalLink("openIri", featureValueAsString);
        openIri.add(visibleWhen(featureValueAsString.map(urlValidator::isValid)));
        openIri.setOutputMarkupPlaceholderTag(true);
        queue(openIri);

        add(new KeyBindingsPanel("keyBindings", () -> traits.getKeyBindings(), aModel, aHandler)
                // The key bindings are only visible when the label is also enabled, i.e. when the
                // editor is used in a "normal" context and not e.g. in the key bindings
                // configuration panel
                .add(visibleWhen(() -> getLabelComponent().isVisible())));

        add(new SuggestionStatePanel("suggestionInfo", aModel));
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected AbstractTextComponent createInputField()
    {

        var textField = new TextField<>("value");
        textField.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            t.add(openIri);
        }));
        return textField;
    }
}
