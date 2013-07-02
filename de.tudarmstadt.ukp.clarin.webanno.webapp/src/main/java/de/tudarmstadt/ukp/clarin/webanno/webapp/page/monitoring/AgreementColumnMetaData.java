/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.monitoring;

import java.util.List;

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
public class AgreementColumnMetaData
    extends AbstractColumn<List<String>>
{

    private static final long serialVersionUID = 1L;
    private int columnNumber;

    public AgreementColumnMetaData(final TableDataProvider prov, final int colNumber)
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
    public void populateItem(final Item<ICellPopulator<List<String>>> aCellItem,
            final String componentId, final IModel<List<String>> rowModel)
    {
            aCellItem.add(new Label(componentId, rowModel.getObject().get(columnNumber)));
    }
}
