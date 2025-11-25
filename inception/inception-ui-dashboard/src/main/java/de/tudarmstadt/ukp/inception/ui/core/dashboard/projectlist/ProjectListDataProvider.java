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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist.ProjectListSortKeys.NAME;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.containsAny;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class ProjectListDataProvider
    extends SortableDataProvider<ProjectEntry, ProjectListSortKeys>
    implements IFilterStateLocator<ProjectListFilterState>, Serializable
{
    private static final String NAME_DESC_SEP = "::";

    private static final long serialVersionUID = 2144634813265254948L;

    private static final Date BEGINNING_OF_TIME = new Date(0);

    private ProjectListFilterState filterState;
    private IModel<List<ProjectEntry>> source;
    private IModel<List<ProjectEntry>> data;

    public ProjectListDataProvider(IModel<List<ProjectEntry>> aDocuments)
    {
        source = aDocuments;
        refresh();

        // Init filter
        filterState = new ProjectListFilterState();

        // Initial Sorting
        setSort(NAME, ASCENDING);
    }

    public void refresh()
    {
        data = Model.ofList(source.getObject());
    }

    @Override
    public Iterator<? extends ProjectEntry> iterator(long aFirst, long aCount)
    {
        var filteredData = filter(data.getObject());
        filteredData.sort(this::comparator);
        return filteredData //
                .subList((int) aFirst, (int) (aFirst + aCount)) //
                .iterator();
    }

    private int comparator(ProjectEntry ob1, ProjectEntry ob2)
    {
        var o1 = ob1.getProject();
        var o2 = ob2.getProject();
        int dir = getSort().isAscending() ? 1 : -1;
        switch (getSort().getProperty()) {
        case NAME:
            return dir * (o1.getName().compareTo(o2.getName()));
        case STATE:
            return dir * (o1.getState().getName().compareTo(o2.getState().getName()));
        case CREATED: {
            var d1 = o1.getCreated();
            var d2 = o2.getCreated();
            d1 = d1 != null ? d1 : BEGINNING_OF_TIME;
            d2 = d2 != null ? d2 : BEGINNING_OF_TIME;
            return dir * (d1.compareTo(d2));
        }
        case UPDATED: {
            var d1 = o1.getUpdated();
            var d2 = o2.getUpdated();
            d1 = d1 != null ? d1 : BEGINNING_OF_TIME;
            d2 = d2 != null ? d2 : BEGINNING_OF_TIME;
            return dir * (d1.compareTo(d2));
        }
        default:
            return 0;
        }
    }

    private List<ProjectEntry> filter(List<ProjectEntry> aData)
    {
        var projectStream = aData.stream();

        // Filter by project name
        if (filterState.getQuery() != null) {
            String nameFilter;
            String descriptionFilter;

            var query = filterState.getQuery();
            if (query.contains(NAME_DESC_SEP)) {
                nameFilter = substringBefore(query, NAME_DESC_SEP).trim();
                descriptionFilter = substringAfter(query, NAME_DESC_SEP).trim();
            }
            else {
                nameFilter = query.trim();
                descriptionFilter = null;
            }

            if (isNotBlank(nameFilter)) {
                projectStream = projectStream
                        .filter(doc -> containsIgnoreCase(doc.getProject().getName(), nameFilter));
            }

            if (isNotBlank(descriptionFilter)) {
                projectStream = projectStream
                        .filter(doc -> containsIgnoreCase(doc.getProject().getDescription(),
                                descriptionFilter));
            }
        }

        // Filter by user roles
        if (isNotEmpty(filterState.getRoles())) {
            projectStream = projectStream
                    .filter(project -> containsAny(filterState.getRoles(), project.getLevels()));
        }

        if (isNotEmpty(filterState.getStates())) {
            projectStream = projectStream.filter(
                    project -> filterState.getStates().contains(project.getProject().getState()));
        }

        return projectStream.collect(toList());
    }

    @Override
    public long size()
    {
        return filter(data.getObject()).size();
    }

    @Override
    public IModel<ProjectEntry> model(ProjectEntry aObject)
    {
        return Model.of(aObject);
    }

    @Override
    public ProjectListFilterState getFilterState()
    {
        return filterState;
    }

    @Override
    public void setFilterState(ProjectListFilterState aState)
    {
        filterState = aState;
    }

    public IModel<List<ProjectEntry>> getModel()
    {
        return data;
    }
}
