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

import static java.util.Comparator.comparing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import de.tudarmstadt.ukp.inception.pivot.table.CompoundKey;
import de.tudarmstadt.ukp.inception.pivot.table.PivotTable;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaClassAttributeModifier;

public class ValueMapCellPanel
    extends GenericPanel<LinkedHashMap<Serializable, Integer>>
{
    private static final long serialVersionUID = -6396333508191887738L;

    public ValueMapCellPanel(String aId, IModel<LinkedHashMap<Serializable, Integer>> aModel)
    {
        super(aId, aModel);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        var data = new ArrayList<Pair<Serializable, Integer>>();
        if (!getModelObject().isEmpty()) {
            getModelObject().entrySet().forEach(e -> data.add(Pair.of(e.getKey(), e.getValue())));
            data.sort(comparing(Pair::getValue));
        }

        var pivotTable = findParent(PivotTable.class);

        var container = new WebMarkupContainer("container");
        container.add(new LambdaClassAttributeModifier(classes -> {
            if (pivotTable.isValueSetHorizontal()) {
                classes.add("list-group-horizontal");
            }
            else {
                classes.add("list-group-flush");
            }
            return classes;
        }));
        queue(container);

        queue(new ListView<>("item", new ListModel<>(data))
        {
            private static final long serialVersionUID = 6683848758703612359L;

            @Override
            protected void populateItem(ListItem<Pair<Serializable, Integer>> aItem)
            {
                var value = aItem.getModelObject().getKey();
                if (value instanceof CompoundKey key) {
                    aItem.add(new CompoundKeyPanel("value", Model.of(key)));
                }
                else {
                    aItem.add(new Label("value", aItem.getModel().map(Pair::getKey)));
                }

                var count = aItem.getModelObject().getValue();
                aItem.add(new Label("count", count));
            }
        });
    }
}
