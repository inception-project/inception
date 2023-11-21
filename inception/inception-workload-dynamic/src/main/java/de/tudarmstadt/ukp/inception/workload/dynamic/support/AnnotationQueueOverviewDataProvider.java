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

package de.tudarmstadt.ukp.inception.workload.dynamic.support;

import static de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys.DOCUMENT;
import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;

/**
 * This helper class enables features required for the workload page. It also enables access to most
 * of the features of the workload page (e.g. the table)
 */

public class AnnotationQueueOverviewDataProvider
    extends SortableDataProvider<AnnotationQueueItem, AnnotationQueueSortKeys>
    implements IFilterStateLocator<Filter>, Serializable
{
    private static final long serialVersionUID = 4125678936105494485L;

    private List<AnnotationQueueItem> annotationQueueItems;
    private Filter filter;

    public AnnotationQueueOverviewDataProvider(List<AnnotationQueueItem> aAnnotationQueueItemsList)
    {
        annotationQueueItems = aAnnotationQueueItemsList;

        // Init filter
        filter = new Filter();

        // Initial Sorting
        setSort(DOCUMENT, ASCENDING);
    }

    @Override
    public Iterator<AnnotationQueueItem> iterator(long aFirst, long aCount)
    {
        // Apply Filter
        List<AnnotationQueueItem> newList = filter(annotationQueueItems);

        // Apply sorting
        newList.sort((o1, o2) -> {
            int dir = getSort().isAscending() ? 1 : -1;
            switch (getSort().getProperty()) {
            case STATE:
                return dir * (o1.getSourceDocument().getState().getName()
                        .compareTo(o2.getSourceDocument().getState().getName()));
            case ANNOTATORS:
                return dir
                        * (Integer.compare(o1.getAnnotators().size(), o2.getAnnotators().size()));
            case ASSIGNED:
                return dir * Integer.compare(o1.getInProgressCount(), o2.getInProgressCount());
            case DOCUMENT:
                return dir * (o1.getSourceDocument().getName()
                        .compareTo(o2.getSourceDocument().getName()));
            case FINISHED:
                return dir * Integer.compare(o1.getFinishedCount(), o2.getFinishedCount());
            case UPDATED:
                if (o1.getLastUpdated() == null) {
                    return dir;
                }
                else if (o2.getLastUpdated() == null) {
                    return dir * -1;
                }
                else {
                    return dir * (o1.getLastUpdated().compareTo(o2.getLastUpdated()));
                }
            default:
                return 0;
            }
        });

        if ((int) aFirst + (int) aCount > newList.size()) {
            aCount = newList.size() - aFirst;
        }

        return newList.subList((int) aFirst, ((int) aFirst + (int) aCount)).iterator();
    }

    @Override
    public long size()
    {
        return filter(annotationQueueItems).size();
    }

    @Override
    public IModel<AnnotationQueueItem> model(AnnotationQueueItem aAnnotationQueueItem)
    {
        return Model.of(aAnnotationQueueItem);
    }

    private List<AnnotationQueueItem> filter(List<AnnotationQueueItem> aData)
    {
        // AnnotationDocuments are created lazily, so we may not have one for every combination of
        // user and SourceDocument or even one for every SourceDocument. But if any of the filters
        // below are used, then we must have an AnnotationDocument, so it is sufficient to check
        // these and we do not have to look at any SourceDocuments for which no AnnotationDocument
        // does exist.
        boolean filteredByAnnotationDocumentProperties = filter.getUsername() != null //
                || filter.getSelected() //
                || filter.getFrom() != null || filter.getTo() != null;

        Stream<AnnotationQueueItem> docStream = annotationQueueItems.stream();
        if (filteredByAnnotationDocumentProperties) {
            // Filter by annotators(s)
            if (filter.getUsername() != null) {
                Set<String> usernames = Set.of(filter.getUsername().split(","));
                docStream = docStream.filter(doc -> doc.getAnnotationDocuments().stream()
                        .filter(anno -> !AnnotationDocumentState.NEW.equals(anno.getState()))
                        .anyMatch(adoc -> usernames.contains(adoc.getUser())));
            }
        }
        else {
            docStream = aData.stream();
        }

        // Filter by document name
        if (filter.getDocumentName() != null) {
            docStream = docStream.filter(
                    doc -> doc.getSourceDocument().getName().contains(filter.getDocumentName()));
        }

        // Filter by document states
        if (CollectionUtils.isNotEmpty(filter.getStates())) {
            docStream = docStream.filter(doc -> filter.getStates().contains(doc.getState()));
        }

        // Filter out any documents which have any annotations ongoing or finished
        if (filter.getSelected()) {
            docStream = docStream.filter(doc -> doc.getAnnotationDocuments().stream()
                    .anyMatch(anno -> AnnotationDocumentState.NEW.equals(anno.getState())
                            || AnnotationDocumentState.FINISHED.equals(anno.getState())));
        }

        // Filter by last updated
        if (filter.getFrom() != null || filter.getTo() != null) {
            docStream = docStream.filter(doc -> doc.getSourceDocument().getUpdated() != null);

            if (filter.getFrom() != null) {
                docStream = docStream.filter(doc -> doc.getSourceDocument().getUpdated()
                        .compareTo(filter.getFrom()) >= 0);
            }

            if (filter.getTo() != null) {
                docStream = docStream.filter(
                        doc -> doc.getSourceDocument().getUpdated().compareTo(filter.getTo()) <= 0);
            }
        }

        return docStream.collect(Collectors.toList());
    }

    @Override
    public Filter getFilterState()
    {
        return filter;
    }

    @Override
    public void setFilterState(Filter aFilter)
    {
        filter = aFilter;
    }

    public void setAnnotationQueueItems(List<AnnotationQueueItem> aAnnotationQueueItems)
    {
        annotationQueueItems = aAnnotationQueueItems;
    }
}
