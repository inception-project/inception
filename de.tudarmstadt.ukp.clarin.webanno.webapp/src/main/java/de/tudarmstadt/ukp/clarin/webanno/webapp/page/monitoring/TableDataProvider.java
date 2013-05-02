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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

/**
 * Data provider for the user's annotation documents status {@link DataGridView}
 * @author Seid Muhie Yimam
 *
 */
public class TableDataProvider
    extends SortableDataProvider<List<? extends String>>
{

    private static final long serialVersionUID = 1L;

    private IModel<List<List<? extends String>>> dataModel;
    private int size = 0;
    private List<String> colNames;

    public List<String> getColNames()
    {
        if (colNames == null) {
            dataModel.getObject();
        }
        return colNames;
    }

    public TableDataProvider(final List<String> aTableHeaders,
            final List<List<String>> aCellContents)
    {
        dataModel = new LoadableDetachableModel<List<List<? extends String>>>()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<List<? extends String>> load()
            {
                ArrayList<List<? extends String>> resultList = new ArrayList<List<? extends String>>();

                colNames = new ArrayList<String>();
                for (String document : aTableHeaders) {
                    colNames.add(document);
                }

                int rowsRead = 0;
                for (List<String> cellContents : aCellContents) {
                    List<String> row = new ArrayList<String>();
                    rowsRead++;
                    for (String cellContent : cellContents) {
                        row.add(cellContent);
                    }
                    resultList.add(row);
                }
                size = rowsRead;
                return resultList;
            }
        };
    }

    public Iterator<List<? extends String>> iterator(int first, int count)
    {

        int boundsSafeCount = count;

        if (first + count > size) {
            boundsSafeCount = first - size;
        }
        else {
            boundsSafeCount = count;
        }

        return dataModel.getObject().subList(first, first + boundsSafeCount).iterator();
    }

    public int size()
    {
        return size;
    }

    public IModel<List<? extends String>> model(List<? extends String> object)
    {
        return Model.<String> ofList(object);
    }

    @Override
    public void detach()
    {
        dataModel.detach();
        super.detach();
    }

    public int getColumnCount()
    {
        return getColNames().size();
    }
}