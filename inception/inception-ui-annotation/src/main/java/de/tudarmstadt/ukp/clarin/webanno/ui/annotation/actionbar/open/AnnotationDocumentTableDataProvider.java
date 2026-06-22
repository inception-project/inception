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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open;

import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open.AnnotationDocumentTableSortKeys.NAME;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open.AnnotationDocumentTableSortKeys.SENTENCES;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open.AnnotationDocumentTableSortKeys.TOKENS;
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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.search.DocumentStatistics;

public class AnnotationDocumentTableDataProvider
    extends SortableDataProvider<AnnotationDocument, AnnotationDocumentTableSortKeys>
    implements IFilterStateLocator<AnnotationDocumentTableFilterState>, Serializable
{
    private static final long serialVersionUID = -8262950880527423715L;

    private AnnotationDocumentTableFilterState filterState;
    private IModel<List<AnnotationDocument>> data;
    private SerializableFunction<Collection<SourceDocument>, //
            Map<Long, DocumentStatistics>> statisticsLoader;
    private Map<Long, DocumentStatistics> statistics;
    private boolean statisticsComplete;

    public AnnotationDocumentTableDataProvider(IModel<List<AnnotationDocument>> aDocuments)
    {
        data = aDocuments;

        // Init filter
        filterState = new AnnotationDocumentTableFilterState();

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

    /**
     * Drops the cached statistics. Must be called when the underlying document list or the data
     * owner changes, since the statistics are specific to that combination.
     */
    public void clearStatistics()
    {
        statistics = null;
        statisticsComplete = false;
    }

    @Override
    public Iterator<? extends AnnotationDocument> iterator(long aFirst, long aCount)
    {
        var filteredData = filter(data.getObject());

        var sortProperty = getSort().getProperty();
        var sortByStatistics = sortProperty == TOKENS || sortProperty == SENTENCES;

        // Sorting by token/sentence count needs statistics for the whole document set, not just
        // the currently visible page. Load them for the full (unfiltered) set once and cache them
        // so that toggling the sort direction or switching back to another sort column does not
        // trigger another lookup. The cache is dropped via clearStatistics() when the document
        // list or the data owner changes.
        if (sortByStatistics && statisticsLoader != null && !statisticsComplete) {
            statistics = loadStatistics(data.getObject());
            statisticsComplete = true;
        }

        filteredData.sort(this::comparator);

        var page = filteredData //
                .subList((int) aFirst, (int) (aFirst + aCount));

        // For non-statistics sorts we only need the counts for the visible page - unless the full
        // set has already been cached, in which case we just reuse it.
        if (statisticsLoader != null && !statisticsComplete) {
            statistics = loadStatistics(page);
        }

        return page.iterator();
    }

    private Map<Long, DocumentStatistics> loadStatistics(List<AnnotationDocument> aDocuments)
    {
        var srcDocs = aDocuments.stream() //
                .map(AnnotationDocument::getDocument) //
                .collect(toList());
        return statisticsLoader.apply(srcDocs);
    }

    private int comparator(AnnotationDocument o1, AnnotationDocument o2)
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
            return dir * compare(o1.getDocument().getCreated(), o2.getDocument().getCreated());
        case UPDATED:
            return dir * compare(o1.getUpdated(), o2.getUpdated());
        default:
            return 0;
        }
    }

    private int compareStatistics(AnnotationDocument o1, AnnotationDocument o2,
            ToLongFunction<DocumentStatistics> aAccessor)
    {
        var stats = getStatistics();
        var s1 = stats.get(o1.getDocument().getId());
        var s2 = stats.get(o2.getDocument().getId());
        var v1 = s1 != null ? aAccessor.applyAsLong(s1) : null;
        var v2 = s2 != null ? aAccessor.applyAsLong(s2) : null;
        return compare(v1, v2);
    }

    public List<AnnotationDocument> filter(List<AnnotationDocument> aData)
    {
        Stream<AnnotationDocument> docStream = aData.stream();

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
        return filter(data.getObject()).size();
    }

    @Override
    public IModel<AnnotationDocument> model(AnnotationDocument aObject)
    {
        return Model.of(aObject);
    }

    @Override
    public AnnotationDocumentTableFilterState getFilterState()
    {
        return filterState;
    }

    @Override
    public void setFilterState(AnnotationDocumentTableFilterState aState)
    {
        filterState = aState;
    }

    public IModel<List<AnnotationDocument>> getModel()
    {
        return data;
    }
}
