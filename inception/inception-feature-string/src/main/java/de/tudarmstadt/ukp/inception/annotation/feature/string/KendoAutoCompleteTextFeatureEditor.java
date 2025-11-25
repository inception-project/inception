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
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.ReorderableTagAutoCompleteField;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.SuggestionStatePanel;
import de.tudarmstadt.ukp.inception.support.kendo.KendoChoiceDescriptionScriptReference;

/**
 * String feature editor using a Kendo AutoComplete field.
 * 
 * <b>PROs</b>
 * <ul>
 * <li>Can be auto-focussed when an annotation is selected.</li>
 * <li>Description tooltips already work.</li>
 * <li>Server-side filtering</li>
 * <li>Re-focussing after safe does not work out of the box, but is covered by
 * wicket-jquery-focus-patch.js</li>
 * </ul>
 * 
 * <b>CONs</b>
 * <ul>
 * <li>Clicking into the input field does not directly open the suggestion list. Keyboard input is
 * required for the list to open.</li>
 * </ul>
 * 
 * <b>TODOs</b>
 * <ul>
 * <li>...?</li>
 * </ul>
 */
public class KendoAutoCompleteTextFeatureEditor
    extends TextFeatureEditorBase
{
    private static final long serialVersionUID = 8686646370500180943L;

    private @SpringBean StringFeatureSupportProperties properties;

    public KendoAutoCompleteTextFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel, AnnotationActionHandler aHandler)
    {
        super(aId, aItem, aModel);

        AnnotationFeature feat = getModelObject().feature;
        StringFeatureTraits traits = readFeatureTraits(feat);

        add(new KeyBindingsPanel("keyBindings", () -> traits.getKeyBindings(), aModel, aHandler)
                // The key bindings are only visible when the label is also enabled, i.e. when the
                // editor is used in a "normal" context and not e.g. in the keybindings
                // configuration panel
                .add(visibleWhen(() -> getLabelComponent().isVisible())));

        add(new SuggestionStatePanel("suggestionInfo", aModel));
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }

    @Override
    protected AbstractTextComponent<ReorderableTag> createInputField()
    {
        return new ReorderableTagAutoCompleteField("value", getModel(),
                properties.getAutoCompleteMaxResults());
    }
}
