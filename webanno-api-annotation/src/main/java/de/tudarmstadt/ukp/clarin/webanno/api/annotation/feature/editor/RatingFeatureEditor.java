/*
 * Copyright 2019
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

import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.googlecode.wicket.kendo.ui.form.Radio;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.event.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;

public class RatingFeatureEditor
        extends FeatureEditor
{
    private static final long serialVersionUID = 9112762779124263198L;
    
    @SuppressWarnings("rawtypes")
    private final RadioGroup<Integer> field;
    private IModel<FeatureState> model;
    
    public RatingFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel,
            List<Integer> aRange)
    {
        super(aId, aItem, aModel);
        model = aModel;
        
        field = new RadioGroup<>("group", new PropertyModel<>(aModel, "value"));
        field.add(createFeaturesList(aRange));
        add(field);
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public RadioGroup getFocusComponent()
    {
        return field;
    }
    
    private ListView<Integer> createFeaturesList(List<Integer> range)
    {
        return new ListView<Integer>("radios", range)
        {
            private static final long serialVersionUID = 6856342528153905386L;
            
            @Override
            protected void populateItem(ListItem<Integer> item)
            {
                Radio<Integer> radio =
                        new Radio<>("radio", new Model(item.getModelObject()), field);
                radio.add(new AjaxEventBehavior("click")
                {
                    @Override protected void onEvent(AjaxRequestTarget aTarget)
                    {
                        model.getObject().value = item.getModelObject();
                        update(aTarget);
                    }
                });
                Radio.Label label = new Radio.Label("label", item.getModelObject(), radio);
                item.add(radio, label);
            }
        };
    }
    
    private void update(AjaxRequestTarget aTarget) {
        send(this, Broadcast.BUBBLE,
                new FeatureEditorValueChangedEvent(model.getObject(), aTarget));
    }
    
}
