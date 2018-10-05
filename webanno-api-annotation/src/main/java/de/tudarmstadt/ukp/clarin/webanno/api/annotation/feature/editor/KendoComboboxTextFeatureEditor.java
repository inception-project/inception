/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.KendoUIBehavior;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBoxBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;

/**
 * String feature editor using a Kendo ComboBox field.
 * 
 * <b>PROs</b>
 * <ul>
 * <li>Dropdown box is always available</li>
 * <li>Description tooltips already work.</li>
 * <li>Re-focussing after safe does not work out of the box, but is covered by 
 *     wicket-jquery-focus-patch.js</li>
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

    private static final Logger LOG = LoggerFactory.getLogger(KendoComboboxTextFeatureEditor.class);

    public KendoComboboxTextFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel)
    {
        super(aId, aItem, aModel);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
        
        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }

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
            protected void onConfigure()
            {
                super.onConfigure();
                
                // Trigger a re-loading of the tagset from the server as constraints may have
                // changed the ordering
                RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(target -> {
                    LOG.trace("onInitialize() requesting datasource re-reading");
                    target.appendJavaScript(
                            String.format("var $w = %s; if ($w) { $w.dataSource.read(); }",
                                    KendoUIBehavior.widget(this, ComboBoxBehavior.METHOD)));
                });
            }
        };
    }
}
