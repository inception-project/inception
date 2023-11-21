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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.inception.search.LayerStatistics;

public class StatisticsProvider
    extends SortableDataProvider<LayerStatistics, String>
{
    private static final long serialVersionUID = 7665907275292598984L;

    class SortableDataProviderComparator
        implements Comparator<LayerStatistics>, Serializable
    {
        private static final long serialVersionUID = 6209880863368659008L;

        @Override
        public int compare(LayerStatistics aStats1, LayerStatistics aStats2)
        {
            var model1 = new PropertyModel<String>(aStats1, getSort().getProperty());
            var model2 = new PropertyModel<String>(aStats2, getSort().getProperty());

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
        setSort("getLayerFeatureName", SortOrder.ASCENDING);
        list = aList;

    }

    @Override
    public Iterator<LayerStatistics> iterator(long aFirst, long aCount)
    {
        List<LayerStatistics> newList = new ArrayList<LayerStatistics>(list);

        Collections.sort(newList, comparator);

        // Return the data for the current page - this can be determined only after sorting
        return newList.subList((int) aFirst, (int) (aFirst + aCount)).iterator();
    }

    @Override
    public IModel<LayerStatistics> model(LayerStatistics aObject)
    {
        return Model.of(aObject);
    }

    @Override
    public long size()
    {
        return list.size();
    }

    public void setData(List<LayerStatistics> aList)
    {
        list = aList;
    }

    public List<LayerStatistics> getData()
    {
        List<LayerStatistics> sortedList = new ArrayList<LayerStatistics>(list);
        Collections.sort(sortedList, comparator);

        return sortedList;
    }

}
