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
package de.tudarmstadt.ukp.inception.diam.sidebar.preferences;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static java.util.Collections.swap;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class PinnedGroupsPanel
    extends Panel
{
    private static final long serialVersionUID = 5606608290992666303L;

    private final IModel<String> newItemName = Model.of();

    public PinnedGroupsPanel(String id, IModel<List<String>> aModel)
    {
        super(id, aModel);

        setOutputMarkupId(true);

        ListView<String> additionalMatchingPropertiesListView = new ListView<String>("items",
                getModel())
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item)
            {
                var itemForm = new Form<Void>("itemForm");
                itemForm.add(buildTextField("textField", item.getModel()));
                itemForm.add(new LambdaAjaxLink("removeItem", t -> PinnedGroupsPanel.this
                        .actionRemoveProperty(t, item.getModelObject())));
                itemForm.add(new LambdaAjaxLink("moveUp",
                        t -> PinnedGroupsPanel.this.actionMoveUp(t, item.getModelObject()))
                                .add(enabledWhen(() -> item.getIndex() > 0)));
                itemForm.add(new LambdaAjaxLink("moveDown",
                        t -> PinnedGroupsPanel.this.actionMoveDown(t, item.getModelObject())).add(
                                enabledWhen(() -> item.getIndex() < getModelObject().size() - 1)));
                item.add(itemForm);
            }

        };
        additionalMatchingPropertiesListView.setOutputMarkupId(true);
        add(additionalMatchingPropertiesListView);

        TextField<String> newPropertyField = new TextField<>("newItemName", newItemName);
        newPropertyField.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        add(newPropertyField);

        LambdaAjaxLink addPropertyButton = new LambdaAjaxLink("addItem",
                PinnedGroupsPanel.this::actionAddProperty);
        add(addPropertyButton);
    }

    @SuppressWarnings("unchecked")
    public IModel<List<String>> getModel()
    {
        return (IModel<List<String>>) getDefaultModel();
    }

    private TextField<String> buildTextField(String id, IModel<String> model)
    {
        TextField<String> iriTextfield = new TextField<>(id, model);
        iriTextfield.setOutputMarkupId(true);
        iriTextfield.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            // Do nothing just update the model values
        }));
        iriTextfield.setEnabled(false);
        return iriTextfield;
    }

    private void actionAddProperty(AjaxRequestTarget aTarget)
    {
        var item = newItemName.getObject();
        var items = getModel().getObject();
        if (!items.contains(item)) {
            items.add(item);
        }
        newItemName.setObject(null);
        aTarget.add(this);
    }

    private void actionRemoveProperty(AjaxRequestTarget aTarget, String aItem)
    {
        getModel().getObject().remove(aItem);
        aTarget.add(this);
    }

    private void actionMoveUp(AjaxRequestTarget aTarget, String aItem)
    {
        var items = getModel().getObject();
        int index = items.indexOf(aItem);
        if (index > 0) {
            swap(items, index - 1, index);
        }
        aTarget.add(this);
    }

    private void actionMoveDown(AjaxRequestTarget aTarget, String aItem)
    {
        var items = getModel().getObject();
        int index = items.indexOf(aItem);
        if (index < items.size() - 1) {
            swap(items, index, index + 1);
        }
        aTarget.add(this);
    }
}
