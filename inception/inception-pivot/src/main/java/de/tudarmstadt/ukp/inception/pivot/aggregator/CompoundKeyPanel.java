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
package de.tudarmstadt.ukp.inception.pivot.aggregator;

import java.io.Serializable;
import java.util.Map.Entry;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.pivot.table.CompoundKey;
import de.tudarmstadt.ukp.inception.pivot.table.PivotTable;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaClassAttributeModifier;

public class CompoundKeyPanel
    extends GenericPanel<CompoundKey>
{
    private static final long serialVersionUID = 438430448624532748L;

    public CompoundKeyPanel(String aId, IModel<CompoundKey> aModel)
    {
        super(aId, aModel);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        var pivotTable = findParent(PivotTable.class);

        var container = new WebMarkupContainer("container");
        container.add(new LambdaClassAttributeModifier(classes -> {
            if (pivotTable.isCompoundKeyHorizontal()) {
                classes.add("list-group-horizontal");
            }
            else {
                classes.add("list-group-flush");
            }
            return classes;
        }));
        queue(container);

        var model = getModel();
        queue(new ListView<Entry<String, Serializable>>("key", model.map(CompoundKey::entries))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Entry<String, Serializable>> aItem)
            {
                var name = new Label("name", aItem.getModel().map(Entry::getKey));
                name.setVisible(model.map(CompoundKey::isMultiValue).getObject());
                aItem.add(name);

                aItem.add(
                        new Label("value", aItem.getModel().map(Entry::getValue).orElse("<none>")));
            }
        });
    }
}
