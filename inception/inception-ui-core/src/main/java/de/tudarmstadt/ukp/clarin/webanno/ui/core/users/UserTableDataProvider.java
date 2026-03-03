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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.users;

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.users.UserTableSortKeys.UI_NAME;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.compare;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class UserTableDataProvider
    extends SortableDataProvider<UserTableRow, UserTableSortKeys>
    implements IFilterStateLocator<UserTableFilterState>, Serializable, Iterable<UserTableRow>
{
    private static final long serialVersionUID = -8262950880527423715L;

    private UserTableFilterState filterState;
    private IModel<List<UserTableRow>> source;
    private IModel<List<UserTableRow>> data;

    public UserTableDataProvider(IModel<List<UserTableRow>> aUsers)
    {
        source = aUsers;
        refresh();

        // Init filter
        filterState = new UserTableFilterState();

        // Initial Sorting
        setSort(UI_NAME, ASCENDING);
    }

    public void refresh()
    {
        data = Model.ofList(source.getObject());
    }

    @Override
    public Iterator<UserTableRow> iterator()
    {
        var filteredData = filter(data.getObject());
        filteredData.sort(this::comparator);
        return filteredData.iterator();
    }

    @Override
    public Iterator<? extends UserTableRow> iterator(long aFirst, long aCount)
    {
        var filteredData = filter(data.getObject());
        filteredData.sort(this::comparator);
        return filteredData //
                .subList((int) aFirst, (int) (aFirst + aCount)) //
                .iterator();
    }

    private int comparator(UserTableRow ob1, UserTableRow ob2)
    {
        var o1 = ob1;
        var o2 = ob2;
        int dir = getSort().isAscending() ? 1 : -1;
        switch (getSort().getProperty()) {
        case NAME:
            return dir * compare(o1.getUser().getUsername(), o2.getUser().getUsername());
        case UI_NAME:
            return dir * compare(o1.getUser().getUiName(), o2.getUser().getUiName());
        case CREATED:
            return dir * compare(o1.getUser().getCreated(), o2.getUser().getCreated());
        case LAST_LOGIN:
            return dir * compare(o1.getUser().getLastLogin(), o2.getUser().getLastLogin());
        default:
            return 0;
        }
    }

    private List<UserTableRow> filter(List<UserTableRow> aData)
    {
        var userStream = aData.stream();

        // Filter by document name
        if (filterState.getUsername() != null) {
            userStream = userStream.filter(user -> containsIgnoreCase(user.getUser().getUiName(),
                    filterState.getUsername())
                    || containsIgnoreCase(user.getUser().getUsername(), filterState.getUsername()));
        }

        // Filter by document states
        if (isNotEmpty(filterState.getStates())) {
            userStream = userStream
                    .filter(user -> filterState.getStates().contains(UserState.of(user.getUser())));
        }

        return userStream.collect(toList());
    }

    @Override
    public long size()
    {
        return filter(data.getObject()).size();
    }

    @Override
    public IModel<UserTableRow> model(UserTableRow aObject)
    {
        return Model.of(aObject);
    }

    @Override
    public UserTableFilterState getFilterState()
    {
        return filterState;
    }

    @Override
    public void setFilterState(UserTableFilterState aState)
    {
        filterState = aState;
    }

    public IModel<List<UserTableRow>> getModel()
    {
        return data;
    }
}
