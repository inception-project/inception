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
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.inception.search.LayerStatistics;

public class StatisticsProvider
    extends SortableDataProvider
{

    class SortableDataProviderComparator
        implements Comparator<LayerStatistics>, Serializable
    {
        @Override
        public int compare(LayerStatistics aStats1, LayerStatistics aStats2)
        {
            PropertyModel<Comparable> model1 = new PropertyModel<Comparable>(aStats1,
                    (String) getSort().getProperty());
            PropertyModel<Comparable> model2 = new PropertyModel<Comparable>(aStats2,
                    (String) getSort().getProperty());

            int result = model1.getObject().compareTo(model2.getObject());

            if (!getSort().isAscending()) {
                result = -result;
            }

            return result;
        }

    }

    private List<LayerStatistics> list = new ArrayList<LayerStatistics>();
    private SortableDataProviderComparator comparator = new SortableDataProviderComparator();

    public StatisticsProvider(List<LayerStatistics> aList)
    {
        // The default sorting
        setSort("feature.getUiName", SortOrder.ASCENDING);
        list = aList;

    }

    @Override
    public Iterator<LayerStatistics> iterator(long aFirst, long aCount)
    {
        // In this example the whole list gets copied, sorted and sliced; in real applications
        // typically your database would deliver a sorted and limited list

        // Get the data
        List<LayerStatistics> newList = new ArrayList<LayerStatistics>(list);

        // Sort the data
        Collections.sort(newList, comparator);

        // Return the data for the current page - this can be determined only after sorting
        return newList.subList((int) aFirst, (int) (aFirst + aCount)).iterator();
    }

    @Override
    public IModel<LayerStatistics> model(Object aObject)
    {
        return new IModel<LayerStatistics>()
        {
            @Override
            public LayerStatistics getObject()
            {
                return (LayerStatistics) aObject;
            }
        };
    }

    @Override
    public long size()
    {
        return list.size();
    }

}
