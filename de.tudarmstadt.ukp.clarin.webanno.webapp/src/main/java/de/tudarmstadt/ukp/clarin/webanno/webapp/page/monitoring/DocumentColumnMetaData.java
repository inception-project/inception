/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.monitoring;

import java.util.List;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * Build dynamic columns for the user's annotation documents status {@link DataGridView}
 *
 * @author Seid Muhie Yimam
 *
 */
public class DocumentColumnMetaData
    extends AbstractColumn<List<String>>
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    int columnNumber;

    public DocumentColumnMetaData(final UserAnnotatedDocumentProvider prov, final int colNumber)
    {
        super(new AbstractReadOnlyModel<String>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject()
            {
                return prov.getColNames().get(colNumber);

            }
        });
        columnNumber = colNumber;
    }

    @Override
    public void populateItem(Item<ICellPopulator<List<String>>> cellItem, String componentId,
            IModel<List<String>> rowModel)
    {

        String value = rowModel.getObject().get(columnNumber);
        String color = "";
        if (value.equals("XX")) {
            color = "red";
        }
        else if(value.equals("YY")) {
            color ="green";
        }
        //It is a username column
        if (color.equals("")) {
            cellItem.add(new Label(componentId, value));
        }
        else {
            cellItem.add(new Label(componentId, value)).add(
                    new SimpleAttributeModifier("style", "color:" + color + ";background-color:"
                            + color));
        }
    }

}