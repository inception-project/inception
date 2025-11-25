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

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Comparator.naturalOrder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

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

public class ValueSetCellPanel
    extends GenericPanel<LinkedHashSet<Serializable>>
{
    private static final long serialVersionUID = -6396333508191887738L;

    public ValueSetCellPanel(String aId, IModel<LinkedHashSet<Serializable>> aModel)
    {
        super(aId, aModel);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        var data = new ArrayList<>(getModelObject());
        if (!data.isEmpty()) {
            if (data.get(0) instanceof String) {
                ((List) data).sort(CASE_INSENSITIVE_ORDER);
            }
            else {
                ((List) data).sort(naturalOrder());
            }
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
            protected void populateItem(ListItem<Serializable> aItem)
            {
                if (aItem.getModelObject() instanceof CompoundKey key) {
                    aItem.add(new CompoundKeyPanel("value", Model.of(key)));
                }
                else {
                    aItem.add(new Label("value", aItem.getModel()));
                }
            }
        });
    }
}
