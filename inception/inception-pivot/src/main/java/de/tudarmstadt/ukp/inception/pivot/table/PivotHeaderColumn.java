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
package de.tudarmstadt.ukp.inception.pivot.table;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class PivotHeaderColumn<T, S>
    implements IStyledColumn<Row<T>, S>, IExportableColumn<Row<T>, S>
{
    private static final long serialVersionUID = -3327434665151784044L;

    private final S sortProperty;

    public PivotHeaderColumn()
    {
        this(null);
    }

    public PivotHeaderColumn(S aSortProperty)
    {
        sortProperty = aSortProperty;
    }

    @Override
    public S getSortProperty()
    {
        return sortProperty;
    }

    @Override
    public IModel<String> getDisplayModel()
    {
        // We do not use this during rendering, but it may be used e.g. when exporting to CSV
        return Model.of("");
    }

    @Override
    public Component getHeader(String aComponentId)
    {
        return new PivotHeader(aComponentId,
                Model.of(new CompoundKey(new CompoundKeySchema(false, "\u00A0"), false, "Key")));
    }

    @Override
    public String getCssClass()
    {
        return "headers";
    }

    @Override
    public void populateItem(Item<ICellPopulator<Row<T>>> aItem, String componentId,
            IModel<Row<T>> aRowModel)
    {
        aItem.add(new PivotHeader(componentId, getDataModel(aRowModel)));
    }

    @Override
    public IModel<CompoundKey> getDataModel(IModel<Row<T>> aRowModel)
    {
        return aRowModel.map(Row::key);
    }

    @Override
    public void detach()
    {
        // Nothing to do
    }
}
