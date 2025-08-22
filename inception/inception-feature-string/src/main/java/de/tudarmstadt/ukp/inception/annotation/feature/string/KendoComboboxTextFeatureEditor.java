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
import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.wrapInTryCatch;
import static java.lang.String.format;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.Objects;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.template.IJQueryTemplate;
import org.wicketstuff.kendo.ui.KendoUIBehavior;
import org.wicketstuff.kendo.ui.form.combobox.ComboBox;
import org.wicketstuff.kendo.ui.form.combobox.ComboBoxBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.event.TagEvent;
import de.tudarmstadt.ukp.inception.schema.api.feature.SuggestionStatePanel;
import de.tudarmstadt.ukp.inception.support.kendo.KendoChoiceDescriptionScriptReference;

/**
 * String feature editor using a Kendo ComboBox field.
 * 
 * <b>PROs</b>
 * <ul>
 * <li>Dropdown box is always available</li>
 * <li>Description tooltips already work.</li>
 * <li>Re-focussing after safe does not work out of the box, but is covered by
 * wicket-jquery-focus-patch.js</li>
 * </ul>
 * 
 * <b>CONs</b>
 * <ul>
 * <li>No server-side filtering, thus not good for mid-sized or larger tagsets.</li>
 * </ul>
 * 
 * <b>TODOs</b>
 * <ul>
 * <li>...?</li>
 * </ul>
 */
public class KendoComboboxTextFeatureEditor
    extends TextFeatureEditorBase
{
    private static final long serialVersionUID = 8686646370500180943L;

    public KendoComboboxTextFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel, AnnotationActionHandler aHandler)
    {
        super(aId, aItem, aModel);

        var feat = getModelObject().feature;
        var traits = readFeatureTraits(feat);

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

    @OnEvent
    public void onTagEvent(TagEvent aEvent)
    {
        if (getModelObject() == null) {
            return;
        }

        // Check if the tagset that was updated is used by the current editor, otherwise return
        if (!Objects.equals(aEvent.getTag().getTagSet().getId(),
                getModelObject().getFeature().getTagset().getId())) {
            return;
        }

        // If the tag was created in the tagset used by this editor, then we re-render the editor
        // to ensure it picks up the new tag
        aEvent.getRequestTarget().ifPresent(target -> target.add(this));
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected AbstractTextComponent createInputField()
    {
        return new ComboBox<Tag>("value", PropertyModel.of(getModel(), "tagset"))
        {
            private static final long serialVersionUID = -1735694425658462932L;

            @Override
            protected IJQueryTemplate newTemplate()
            {
                return KendoChoiceDescriptionScriptReference.templateReorderable();
            }

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);
                aBehavior.setOption("animation", false);
                aBehavior.setOption("delay", 0);
                aBehavior.setOption("open",
                        KendoChoiceDescriptionScriptReference.applyTooltipScript());
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();

                // Trigger a re-loading of the tagset from the server as constraints may have
                // changed the ordering
                var target = RequestCycle.get().find(AjaxRequestTarget.class);
                if (target.isPresent()) {
                    target.get().appendJavaScript(wrapInTryCatch(format( //
                            "var $w = %s; if ($w) { $w.dataSource.read(); }",
                            KendoUIBehavior.widget(this, ComboBoxBehavior.METHOD))));
                }
            }
        };
    }
}
