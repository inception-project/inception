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
package de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument;

import static de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationDocumentTableSortKeys.NAME;
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

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CurationDocumentTableDataProvider
    extends SortableDataProvider<SourceDocument, CurationDocumentTableSortKeys>
    implements IFilterStateLocator<CurationDocumentTableFilterState>, Serializable
{
    private static final long serialVersionUID = -8262950880527423715L;

    private CurationDocumentTableFilterState filterState;
    private List<SourceDocument> data;

    public CurationDocumentTableDataProvider(IModel<List<SourceDocument>> aDocuments)
    {
        data = aDocuments.getObject();

        // Init filter
        filterState = new CurationDocumentTableFilterState();

        // Initial Sorting
        setSort(NAME, ASCENDING);
    }

    @Override
    public Iterator<? extends SourceDocument> iterator(long aFirst, long aCount)
    {
        var filteredData = filter(data);
        filteredData.sort(this::comparator);
        return filteredData //
                .subList((int) aFirst, (int) (aFirst + aCount)) //
                .iterator();
    }

    private int comparator(SourceDocument o1, SourceDocument o2)
    {
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
        default:
            return 0;
        }
    }

    private List<SourceDocument> filter(List<SourceDocument> aData)
    {
        Stream<SourceDocument> docStream = aData.stream();

        // Filter by document name
        if (filterState.getDocumentName() != null) {
            docStream = docStream.filter(
                    doc -> containsIgnoreCase(doc.getName(), filterState.getDocumentName()));
        }

        // Filter by document states
        if (isNotEmpty(filterState.getStates())) {
            docStream = docStream.filter(doc -> filterState.getStates().contains(doc.getState()));
        }

        return docStream.collect(toList());
    }

    @Override
    public long size()
    {
        return filter(data).size();
    }

    @Override
    public IModel<SourceDocument> model(SourceDocument aObject)
    {
        return Model.of(aObject);
    }

    @Override
    public CurationDocumentTableFilterState getFilterState()
    {
        return filterState;
    }

    @Override
    public void setFilterState(CurationDocumentTableFilterState aState)
    {
        filterState = aState;
    }
}
