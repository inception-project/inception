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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.CSVDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.CellRenderer;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.inception.support.wicket.PipedStreamResource;

public class PivotTable<A extends Serializable, T>
    extends Panel
{
    private static final long serialVersionUID = 1L;

    private List<IExportableColumn<Row<A>, CompoundKey>> columns;
    private PivotTableDataProvider<A, T> dataProvider;

    public PivotTable(String aId, PivotTableDataProvider<A, T> aDataProvider,
            CellRenderer aCellRenderer)
    {
        super(aId);
        setOutputMarkupPlaceholderTag(true);
        dataProvider = aDataProvider;
        columns = buildColumns(dataProvider, aCellRenderer);
        createTable(aDataProvider, columns);
        setVisible(dataProvider.size() > 0);
    }

    private List<IExportableColumn<Row<A>, CompoundKey>> buildColumns(
            PivotTableDataProvider<A, T> aDataProvider, CellRenderer aCellRenderer)
    {
        var cols = new ArrayList<IExportableColumn<Row<A>, CompoundKey>>();
        cols.add(new PivotHeaderColumn<A, CompoundKey>(CompoundKey.ROW_KEY));

        for (var columnKey : aDataProvider.columnKeys()) {
            cols.add(new PivotColumn<>(Model.of(columnKey), aCellRenderer, columnKey));
        }
        return cols;
    }

    private void createTable(PivotTableDataProvider<A, T> aDataProvider,
            List<IExportableColumn<Row<A>, CompoundKey>> aColumns)
    {
        var newTable = new DataTable<Row<A>, CompoundKey>("table", aColumns, aDataProvider, 100);
        newTable.setOutputMarkupId(true);
        newTable.addTopToolbar(
                new AjaxFallbackHeadersToolbar<CompoundKey>(newTable, aDataProvider));
        addOrReplace(newTable);

        var navigatorLabel = new NavigatorLabel("navigatorLabel", newTable);
        navigatorLabel.setOutputMarkupId(true);
        addOrReplace(navigatorLabel);

        var navigator = new BootstrapAjaxPagingNavigator("navigator", newTable)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onAjaxEvent(AjaxRequestTarget aTarget)
            {
                super.onAjaxEvent(aTarget);
                aTarget.add(navigatorLabel);
            }
        };
        addOrReplace(navigator);

        var csvExportLink = new AjaxDownloadLink("download", Model.of("pivot.csv"),
                LoadableDetachableModel.of(() -> actionExport()));
        addOrReplace(csvExportLink);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private IResourceStream actionExport()
    {
        return new PipedStreamResource((os) -> {
            var exporter = new CSVDataExporter();
            exporter.exportData(dataProvider, (List) columns, os);
        });
    }
}
