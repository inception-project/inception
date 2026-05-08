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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.pivot.api.aggregator.CellRenderer;

public class PivotColumn<T, S>
    implements IStyledColumn<Row<T>, S>, IExportableColumn<Row<T>, S>
{
    private static final long serialVersionUID = -3327434665151784044L;

    private final IModel<CompoundKey> key;
    private final S sortProperty;
    private final CellRenderer cellRenderer;

    public PivotColumn(IModel<CompoundKey> aKey, CellRenderer aCellRenderer)
    {
        this(aKey, aCellRenderer, null);
    }

    public PivotColumn(IModel<CompoundKey> aKey, CellRenderer aCellRenderer, S aSortProperty)
    {
        key = aKey;
        sortProperty = aSortProperty;
        cellRenderer = aCellRenderer;
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
        return key.map(CompoundKey::toJson);
    }

    @Override
    public Component getHeader(String aComponentId)
    {
        return new PivotHeader(aComponentId, key);
    }

    @Override
    public String getCssClass()
    {
        return null;
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public void populateItem(Item<ICellPopulator<Row<T>>> aItem, String aComponentId,
            IModel<Row<T>> aRowModel)
    {
        if (cellRenderer != null) {
            aItem.add(cellRenderer.render(aComponentId, (IModel) getDataModel(aRowModel)));
            return;
        }

        aItem.add(new Label(aComponentId, getDataModel(aRowModel)));
    }

    @Override
    public IModel<?> getDataModel(IModel<Row<T>> aRowModel)
    {
        return aRowModel.map(r -> r.data().get(key.getObject()));
    }

    @Override
    public void detach()
    {
        // Nothing to do
    }
}
