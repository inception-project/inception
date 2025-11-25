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

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.CSVDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.CellRenderer;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.inception.support.wicket.PipedStreamResource;

public class PivotTable<A extends Serializable, T>
    extends Panel
{
    private static final long serialVersionUID = 1L;

    private final List<IExportableColumn<Row<A>, CompoundKey>> columns;
    private final PivotTableDataProvider<A, T> dataProvider;
    private final IModel<Boolean> valueSetOrientation = Model.of(false);
    private final IModel<Boolean> compoundKeyOrientation = Model.of(false);
    private final CompoundPropertyModel<PivotTableFilterState> filterState;

    public PivotTable(String aId, PivotTableDataProvider<A, T> aDataProvider,
            CellRenderer aCellRenderer)
    {
        super(aId);

        setOutputMarkupPlaceholderTag(true);

        dataProvider = aDataProvider;
        filterState = CompoundPropertyModel.of(dataProvider.getFilterState());
        setVisible(dataProvider.size() > 0);

        columns = buildColumns(dataProvider, aCellRenderer);
        var table = new DataTable<Row<A>, CompoundKey>("table", columns, aDataProvider, 100);
        table.setOutputMarkupId(true);
        table.addTopToolbar(new AjaxFallbackHeadersToolbar<CompoundKey>(table, aDataProvider));
        table.add(new StickyRowHeaderBehavior("td.headers > div"));
        // Keeping the columns sticky does not seem to work
        // table.add(new StickyColumnHeaderBehavior("tr.headers > th:not(:first-child) > a >
        // span"));
        queue(table);

        var navigatorLabel = new NavigatorLabel("navigatorLabel", table);
        navigatorLabel.setOutputMarkupId(true);
        queue(navigatorLabel);

        var navigator = new BootstrapAjaxPagingNavigator("navigator", table)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onAjaxEvent(AjaxRequestTarget aTarget)
            {
                super.onAjaxEvent(aTarget);
                aTarget.add(navigatorLabel);
            }
        };
        queue(navigator);

        var csvExportLink = new AjaxDownloadLink("download", Model.of("pivot.csv"),
                LoadableDetachableModel.of(() -> actionExport()));
        queue(csvExportLink);

        var cellOrientationControls = new WebMarkupContainer("cell-orientation-controls");
        cellOrientationControls.setVisible(dataProvider.isShowCellControls());
        queue(cellOrientationControls);

        var valueSetOrientationCheck = new CheckBox("valueSetOrientation", valueSetOrientation);
        valueSetOrientationCheck.setOutputMarkupId(true);
        valueSetOrientationCheck
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, _target -> {
                    _target.add(table);
                }));
        queue(valueSetOrientationCheck);

        var compoundKeyOrientationCheck = new CheckBox("compoundKeyOrientation",
                compoundKeyOrientation);
        compoundKeyOrientationCheck.setOutputMarkupId(true);
        compoundKeyOrientationCheck
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, _target -> {
                    _target.add(table);
                }));
        queue(compoundKeyOrientationCheck);

        var filterPanel = new WebMarkupContainer("filterPanel");
        filterPanel.setOutputMarkupId(true);
        queue(filterPanel);

        var hideRowsWithSameValuesInAllColumns = new CheckBox("showRowsWithSameValuesInAllColumns",
                LambdaModel.of(filterState, //
                        s -> !s.isHideRowsWithSameValuesInAllColumns(),
                        (s, v) -> s.setHideRowsWithSameValuesInAllColumns(!v)));
        hideRowsWithSameValuesInAllColumns.setOutputMarkupId(true);
        hideRowsWithSameValuesInAllColumns
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, _target -> {
                    _target.add(table, navigator, navigatorLabel, filterPanel);
                }));
        queue(hideRowsWithSameValuesInAllColumns);

        var hideRowsWithAnyDifferentValue = new CheckBox("showRowsWithAnyDifferentValue",
                LambdaModel.of(filterState, //
                        s -> !s.isHideRowsWithAnyDifferentValue(),
                        (s, v) -> s.setHideRowsWithAnyDifferentValue(!v)));
        hideRowsWithAnyDifferentValue.setOutputMarkupId(true);
        hideRowsWithAnyDifferentValue
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, _target -> {
                    _target.add(table, navigator, navigatorLabel, filterPanel);
                }));
        queue(hideRowsWithAnyDifferentValue);

        var showRowsWithEmptyValues = new CheckBox("showRowsWithEmptyValues",
                LambdaModel.of(filterState, //
                        s -> !s.isHideRowsWithEmptyValues(),
                        (s, v) -> s.setHideRowsWithEmptyValues(!v)));
        showRowsWithEmptyValues.setOutputMarkupId(true);
        showRowsWithEmptyValues
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, _target -> {
                    _target.add(table, navigator, navigatorLabel, filterPanel);
                }));
        queue(showRowsWithEmptyValues);

        var showRowsWithoutEmptyValues = new CheckBox("showRowsWithoutEmptyValues",
                LambdaModel.of(filterState, //
                        s -> !s.isHideRowsWithoutEmptyValues(),
                        (s, v) -> s.setHideRowsWithoutEmptyValues(!v)));
        showRowsWithoutEmptyValues.setOutputMarkupId(true);
        showRowsWithoutEmptyValues
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, _target -> {
                    _target.add(table, navigator, navigatorLabel, filterPanel);
                }));
        queue(showRowsWithoutEmptyValues);
    }

    public boolean isCompoundKeyHorizontal()
    {
        return compoundKeyOrientation.getObject();
    }

    public boolean isValueSetHorizontal()
    {
        return valueSetOrientation.getObject();
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private IResourceStream actionExport()
    {
        return new PipedStreamResource((os) -> {
            var exporter = new CSVDataExporter();
            exporter.exportData(dataProvider, (List) columns, os);
        });
    }
}
