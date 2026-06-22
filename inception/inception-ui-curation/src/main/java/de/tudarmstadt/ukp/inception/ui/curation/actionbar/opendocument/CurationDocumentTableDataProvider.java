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
import static de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationDocumentTableSortKeys.SENTENCES;
import static de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationDocumentTableSortKeys.TOKENS;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.compare;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.danekja.java.util.function.serializable.SerializableFunction;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.search.DocumentStatistics;

public class CurationDocumentTableDataProvider
    extends SortableDataProvider<SourceDocument, CurationDocumentTableSortKeys>
    implements IFilterStateLocator<CurationDocumentTableFilterState>, Serializable
{
    private static final long serialVersionUID = -8262950880527423715L;

    private CurationDocumentTableFilterState filterState;
    private List<SourceDocument> data;
    private SerializableFunction<Collection<SourceDocument>, //
            Map<Long, DocumentStatistics>> statisticsLoader;
    private Map<Long, DocumentStatistics> statistics;
    private boolean statisticsComplete;

    public CurationDocumentTableDataProvider(IModel<List<SourceDocument>> aDocuments)
    {
        data = aDocuments.getObject();

        // Init filter
        filterState = new CurationDocumentTableFilterState();

        // Initial Sorting
        setSort(NAME, ASCENDING);
    }

    public void setStatisticsLoader(
            SerializableFunction<Collection<SourceDocument>, Map<Long, DocumentStatistics>> aLoader)
    {
        statisticsLoader = aLoader;
    }

    /**
     * Statistics available to the table for rendering and sorting. Depending on the active sort,
     * this holds either the statistics for the current page or - once token/sentence sorting has
     * been requested - the cached statistics for the whole document set. Returns an empty map if no
     * loader has been wired or before the table has been rendered.
     */
    public Map<Long, DocumentStatistics> getStatistics()
    {
        return statistics != null ? statistics : emptyMap();
    }

    @Override
    public Iterator<? extends SourceDocument> iterator(long aFirst, long aCount)
    {
        var filteredData = filter(data);

        var sortProperty = getSort().getProperty();
        var sortByStatistics = sortProperty == TOKENS || sortProperty == SENTENCES;

        // Sorting by token/sentence count needs statistics for the whole document set, not just
        // the currently visible page. Load them for the full (unfiltered) set once and cache them
        // so that toggling the sort direction or switching back to another sort column does not
        // trigger another lookup. The document list is fixed for the lifetime of the dialog, so the
        // cache never needs to be invalidated.
        if (sortByStatistics && statisticsLoader != null && !statisticsComplete) {
            statistics = statisticsLoader.apply(data);
            statisticsComplete = true;
        }

        filteredData.sort(this::comparator);

        var page = filteredData //
                .subList((int) aFirst, (int) (aFirst + aCount));

        // For non-statistics sorts we only need the counts for the visible page - unless the full
        // set has already been cached, in which case we just reuse it.
        if (statisticsLoader != null && !statisticsComplete) {
            statistics = statisticsLoader.apply(page);
        }

        return page.iterator();
    }

    private int comparator(SourceDocument o1, SourceDocument o2)
    {
        int dir = getSort().isAscending() ? 1 : -1;
        switch (getSort().getProperty()) {
        case NAME:
            return dir * (o1.getName().compareTo(o2.getName()));
        case STATE:
            return dir * (o1.getState().getName().compareTo(o2.getState().getName()));
        case TOKENS:
            return dir * compareStatistics(o1, o2, DocumentStatistics::tokenCount);
        case SENTENCES:
            return dir * compareStatistics(o1, o2, DocumentStatistics::sentenceCount);
        case CREATED:
            return dir * compare(o1.getCreated(), o2.getCreated());
        case UPDATED:
            return dir * compare(o1.getUpdated(), o2.getUpdated());
        default:
            return 0;
        }
    }

    private int compareStatistics(SourceDocument o1, SourceDocument o2,
            ToLongFunction<DocumentStatistics> aAccessor)
    {
        var stats = getStatistics();
        var s1 = stats.get(o1.getId());
        var s2 = stats.get(o2.getId());
        var v1 = s1 != null ? aAccessor.applyAsLong(s1) : null;
        var v2 = s2 != null ? aAccessor.applyAsLong(s2) : null;
        return compare(v1, v2);
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
