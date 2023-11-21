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
package de.tudarmstadt.ukp.inception.annotation.feature.number;

import static org.apache.wicket.behavior.Behavior.onTag;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditorValueChangedEvent;

public class RatingFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 9112762779124263198L;

    private final RadioGroup<Integer> field;

    public RatingFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel,
            List<Integer> aRange)
    {
        super(aId, aItem, aModel);

        field = new RadioGroup<>("group", new PropertyModel<>(aModel, "value"));
        field.add(createFeaturesList(aRange));
        add(field);
    }

    @Override
    public RadioGroup<Integer> getFocusComponent()
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
                var radio = new Radio<>("radio", item.getModel(), field);
                item.add(radio);
                item.add(new Label("label", item.getModel())
                        .add(onTag((c, t) -> t.put("for", radio.getMarkupId()))));
            }
        };
    }

    @Override
    public void addFeatureUpdateBehavior()
    {
        // Need to use a AjaxFormChoiceComponentUpdatingBehavior here since we use a RadioGroup
        // here.
        FormComponent<?> focusComponent = getFocusComponent();
        focusComponent.add(new AjaxFormChoiceComponentUpdatingBehavior()
        {
            private static final long serialVersionUID = -5058365578109385064L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                send(focusComponent, BUBBLE,
                        new FeatureEditorValueChangedEvent(RatingFeatureEditor.this, aTarget));
            }
        });
    }
}
