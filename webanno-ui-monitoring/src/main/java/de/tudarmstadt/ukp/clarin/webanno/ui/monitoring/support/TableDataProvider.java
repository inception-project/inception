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
package de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support;

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
 *
 */
public class TableDataProvider
    extends SortableDataProvider<List<String>, Object>
{

    private static final long serialVersionUID = 1L;

    private IModel<List<List<String>>> dataModel;
    private long size = 0;
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
        dataModel = new LoadableDetachableModel<List<List<String>>>()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<List<String>> load()
            {
                ArrayList<List<String>> resultList = new ArrayList<>();

                colNames = new ArrayList<>();
                colNames.addAll(aTableHeaders);

                int rowsRead = 0;
                for (List<String> cellContents : aCellContents) {
                    List<String> row = new ArrayList<>();
                    rowsRead++;
                    row.addAll(cellContents);
                    resultList.add(row);
                }
                size = rowsRead;
                return resultList;
            }
        };
    }

    @Override
    public Iterator<List<String>> iterator(long first, long count)
    {

        long boundsSafeCount;

        if (first + count > size) {
            boundsSafeCount = first - size;
        }
        else {
            boundsSafeCount = count;
        }

        return dataModel.getObject().subList((int) first, (int) (first + boundsSafeCount))
                .iterator();
    }

    @Override
    public long size()
    {
        return size;
    }

    @Override
    public IModel<List<String>> model(List<String> object)
    {
        return Model.ofList(object);
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
