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
package de.tudarmstadt.ukp.inception.support.wicket;

import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;

public class EmptyStateToolbar
    extends AbstractToolbar
{
    private static final long serialVersionUID = 1L;

    public EmptyStateToolbar(final DataTable<?, ?> aTable, final IModel<String> aMessageModel)
    {
        super(aTable);

        var label = new Label("msg", aMessageModel);

        var td = new WebMarkupContainer("td");
        td.add(AttributeModifier.replace("colspan",
                LambdaModel.of(() -> String.valueOf(aTable.getColumns().size()))));
        td.add(label);
        add(td);

        aTable.add(new ClassAttributeModifier()
        {
            private static final long serialVersionUID = 4534226094224688646L;

            @Override
            protected Set<String> update(Set<String> aClasses)
            {
                if (getTable().getRowCount() == 0) {
                    aClasses.add("h-100");
                }
                else {
                    aClasses.remove("h-100");
                }

                return aClasses;
            }
        });
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        setVisible(getTable().getRowCount() == 0);
    }
}
