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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.documents;

import static de.tudarmstadt.ukp.clarin.webanno.ui.project.documents.SourceDocumentTableSortKeys.NAME;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.compare;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class SourceDocumentTableDataProvider
    extends SortableDataProvider<SourceDocumentTableRow, SourceDocumentTableSortKeys>
    implements IFilterStateLocator<SourceDocumentTableFilterState>, Serializable,
    Iterable<SourceDocumentTableRow>
{
    private static final long serialVersionUID = -8262950880527423715L;

    private SourceDocumentTableFilterState filterState;
    private IModel<List<SourceDocumentTableRow>> source;
    private IModel<List<SourceDocumentTableRow>> data;

    public SourceDocumentTableDataProvider(IModel<List<SourceDocumentTableRow>> aDocuments)
    {
        source = aDocuments;
        refresh();

        // Init filter
        filterState = new SourceDocumentTableFilterState();

        // Initial Sorting
        setSort(NAME, ASCENDING);
    }

    public void refresh()
    {
        data = Model.ofList(source.getObject());
    }

    @Override
    public Iterator<SourceDocumentTableRow> iterator()
    {
        var filteredData = filter(data.getObject());
        filteredData.sort(this::comparator);
        return filteredData.iterator();
    }

    @Override
    public Iterator<? extends SourceDocumentTableRow> iterator(long aFirst, long aCount)
    {
        var filteredData = filter(data.getObject());
        filteredData.sort(this::comparator);
        return filteredData //
                .subList((int) aFirst, (int) (aFirst + aCount)) //
                .iterator();
    }

    private int comparator(SourceDocumentTableRow ob1, SourceDocumentTableRow ob2)
    {
        var o1 = ob1.getDocument();
        var o2 = ob2.getDocument();
        int dir = getSort().isAscending() ? 1 : -1;
        switch (getSort().getProperty()) {
        case NAME:
            return dir * (o1.getName().compareTo(o2.getName()));
        case STATE:
            return dir * (o1.getState().getName().compareTo(o2.getState().getName()));
        case CREATED:
            return dir * compare(o1.getCreated(), o2.getCreated());
        case UPDATED:
            return dir * compare(o1.getUpdated(), o2.getUpdated());
        case FORMAT:
            return dir * (o1.getFormat().compareTo(o2.getFormat()));
        default:
            return 0;
        }
    }

    private List<SourceDocumentTableRow> filter(List<SourceDocumentTableRow> aData)
    {
        Stream<SourceDocumentTableRow> docStream = aData.stream();

        // Filter by document name
        if (filterState.getDocumentName() != null) {
            docStream = docStream.filter(doc -> containsIgnoreCase(doc.getDocument().getName(),
                    filterState.getDocumentName()));
        }

        // Filter by document states
        if (isNotEmpty(filterState.getStates())) {
            docStream = docStream
                    .filter(doc -> filterState.getStates().contains(doc.getDocument().getState()));
        }

        return docStream.collect(toList());
    }

    @Override
    public long size()
    {
        return filter(data.getObject()).size();
    }

    @Override
    public IModel<SourceDocumentTableRow> model(SourceDocumentTableRow aObject)
    {
        return Model.of(aObject);
    }

    @Override
    public SourceDocumentTableFilterState getFilterState()
    {
        return filterState;
    }

    @Override
    public void setFilterState(SourceDocumentTableFilterState aState)
    {
        filterState = aState;
    }

    public IModel<List<SourceDocumentTableRow>> getModel()
    {
        return data;
    }
}
