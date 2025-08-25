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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import static de.tudarmstadt.ukp.inception.ui.kb.feature.InstanceListSortKeys.LABEL;
import static java.util.stream.Collectors.toList;
import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.Strings;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.inception.kb.graph.KBObject;

public class InstanceListDataProvider
    extends SortableDataProvider<KBObject, InstanceListSortKeys>
    implements IFilterStateLocator<InstanceListFilterState>, Serializable
{
    private static final long serialVersionUID = 4454173648915924289L;

    private InstanceListFilterState filterState;
    private IModel<List<? extends KBObject>> source;
    private IModel<List<KBObject>> data;

    public InstanceListDataProvider(IModel<List<? extends KBObject>> aInstances)
    {
        source = aInstances;
        refresh();

        // Init filter
        filterState = new InstanceListFilterState();

        // Initial Sorting
        setSort(LABEL, ASCENDING);
    }

    @SuppressWarnings("unchecked")
    public void refresh()
    {
        data = Model.ofList((List<KBObject>) source.getObject());
    }

    @Override
    public Iterator<? extends KBObject> iterator(long aFirst, long aCount)
    {
        var filteredData = filter(data.getObject());
        filteredData.sort(this::comparator);
        return filteredData //
                .subList((int) aFirst, (int) (aFirst + aCount)) //
                .iterator();
    }

    private int comparator(KBObject ob1, KBObject ob2)
    {
        int dir = getSort().isAscending() ? 1 : -1;
        switch (getSort().getProperty()) {
        case LABEL:
            return dir * (ob1.getUiLabel().compareTo(ob2.getUiLabel()));
        default:
            return 0;
        }
    }

    private List<? extends KBObject> filter(List<? extends KBObject> aData)
    {
        Stream<? extends KBObject> dataStream = aData.stream();

        // Filter by project name
        if (filterState.getUiLabel() != null) {
            dataStream = dataStream
                    .filter($ -> Strings.CI.contains($.getUiLabel(), filterState.getUiLabel()));
        }

        return dataStream.collect(toList());
    }

    @Override
    public long size()
    {
        return filter(data.getObject()).size();
    }

    @Override
    public IModel<KBObject> model(KBObject aObject)
    {
        return Model.of(aObject);
    }

    @Override
    public InstanceListFilterState getFilterState()
    {
        return filterState;
    }

    @Override
    public void setFilterState(InstanceListFilterState aState)
    {
        filterState = aState;
    }

    public IModel<List<KBObject>> getModel()
    {
        return data;
    }
}
