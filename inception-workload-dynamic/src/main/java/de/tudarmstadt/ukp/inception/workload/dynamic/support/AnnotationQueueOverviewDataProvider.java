/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.inception.workload.dynamic.support;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * This helper class enables features required for the workload page. It also enables access to most
 * of the features of the workload page (e.g. the table)
 */
public class AnnotationQueueOverviewDataProvider
    extends SortableDataProvider<SourceDocument, AnnotationQueueSortKeys>
    implements IFilterStateLocator<Filter>, Serializable
{
    private static final long serialVersionUID = 4125678936105494485L;

    private List<SourceDocument> data;
    private List<AnnotationDocument> allAnnotationDocuments;
    private List<SourceDocument> shownDocuments;

    private IModel<List<SourceDocument>> model;
    private Filter filter;

    /**
     * Default Constructor, initialize values and set the default sorting of the table to ASCENDING
     * of the first column of the table.
     */
    public AnnotationQueueOverviewDataProvider(List<SourceDocument> aData,
            List<AnnotationDocument> aAllAnnotationDocuments)
    {
        data = aData;
        allAnnotationDocuments = aAllAnnotationDocuments;
        shownDocuments = new ArrayList<>();

        // Init filter
        filter = new Filter();

        // Initial Sorting
        setSort(AnnotationQueueSortKeys.Document, SortOrder.ASCENDING);

        // Required, set model
        model = new LoadableDetachableModel<List<SourceDocument>>()
        {
            private static final long serialVersionUID = -3938543310389673460L;

            @Override
            protected List<SourceDocument> load()
            {
                return data;
            }
        };
    }

    @Override
    public Iterator<SourceDocument> iterator(long aFirst, long aCount)
    {
        // Apply Filter
        List<SourceDocument> newList = filterTable(data);

        // Apply sorting
        newList.sort((o1, o2) -> {
            int dir = getSort().isAscending() ? 1 : -1;

            switch (getSort().getProperty()) {
            case State:
                return dir * (o1.getState().getName().compareTo(o2.getState().getName()));
            case Annotators:
                return dir * (Integer.compare(getUsersWorkingOnTheDocument(o1).length(),
                        getUsersWorkingOnTheDocument(o2).length()));
            case Assigned:
                return dir * Long.compare(getInProgressAmountForDocument(o1),
                        getInProgressAmountForDocument(o2));
            case Document:
                return dir * (o1.getName().compareTo(o2.getName()));
            case Finished:
                return dir * Long.compare(getFinishedAmountForDocument(o1),
                        getFinishedAmountForDocument(o2));
            case Updated:
                if (o1.getUpdated() == null) {
                    return dir;
                }
                else if (o2.getUpdated() == null) {
                    return dir * -1;
                }
                else {
                    return dir * (o1.getUpdated().compareTo(o2.getUpdated()));
                }
            case Actions: // fall-through
            default:
                return 0;
            }
        });

        // Reset
        shownDocuments.clear();
        shownDocuments.addAll(newList);

        if ((int) aFirst + (int) aCount > newList.size()) {
            aCount = newList.size() - aFirst;
        }

        return newList.subList((int) aFirst, ((int) aFirst + (int) aCount)).iterator();
    }

    @Override
    public long size()
    {
        return filterTable(data).size();
    }

    @Override
    public IModel<SourceDocument> model(SourceDocument aSourceDocument)
    {
        return Model.of(aSourceDocument);
    }

    @Override
    public void detach()
    {
        super.detach();
        model.detach();
    }

    /**
     * Filtering performed on the table
     */
    public List<SourceDocument> filterTable(List<SourceDocument> aData)
    {
        // AnnotationDocuments are created lazily, so we may not have one for every combination of
        // user and SourceDocument or even one for every SourceDocuemnt. But if any of the filters
        // below are used, then we must have an AnnotationDocument, so it is sufficient to check
        // these and we do not have to look at any SourceDocuments for which no AnnotationDocument
        // does exist.
        boolean filteredByAnnotationDocumentProperties = filter.getUsername() != null //
                || filter.getSelected() //
                || filter.getFrom() != null || filter.getTo() != null;

        Stream<SourceDocument> docStream;
        if (filteredByAnnotationDocumentProperties) {
            Stream<AnnotationDocument> annotationStream = allAnnotationDocuments.stream();

            // Filter by annotators(s)
            if (filter.getUsername() != null) {
                Set<String> usernames = Set.of(filter.getUsername().split(","));
                docStream = allAnnotationDocuments.stream()
                        .filter(adoc -> !AnnotationDocumentState.NEW.equals(adoc.getState()))
                        .filter(adoc -> usernames.contains(adoc.getUser()))
                        .map(adoc -> adoc.getDocument());
            }

            docStream = annotationStream.map(AnnotationDocument::getDocument).distinct();
        }
        else {
            docStream = aData.stream();
        }

        // Filter by document name
        if (filter.getDocumentName() != null) {
            docStream = docStream.filter(doc -> doc.getName().contains(filter.getDocumentName()));
        }

        // Filter by document states
        if (CollectionUtils.isNotEmpty(filter.getStates())) {
            docStream = docStream.filter(doc -> filter.getStates().contains(doc.getState()));
        }

        // Filter out any documents which have any annotations ongoing or finished
        if (filter.getSelected()) {
            docStream = docStream.filter(doc -> allAnnotationDocuments.stream()
                    .anyMatch(adoc -> AnnotationDocumentState.IN_PROGRESS.equals(adoc.getState())
                            || AnnotationDocumentState.FINISHED.equals(adoc.getState())));
        }

        // Filter by last updated
        if (filter.getFrom() != null || filter.getTo() != null) {
            docStream = docStream.filter(doc -> doc.getUpdated() != null);

            if (filter.getFrom() != null) {
                docStream = docStream
                        .filter(doc -> doc.getUpdated().compareTo(filter.getFrom()) >= 0);
            }

            if (filter.getTo() != null) {
                docStream = docStream
                        .filter(doc -> doc.getUpdated().compareTo(filter.getTo()) <= 0);
            }
        }

        return docStream.collect(Collectors.toList());
    }

    /**
     * Helper method, returns for a document how often it is "finished" within the project
     */
    public long getFinishedAmountForDocument(SourceDocument aDocument)
    {
        return allAnnotationDocuments.stream()
                .filter(d -> d.getDocument().equals(aDocument) && d.getState().equals(FINISHED))
                .count();
    }

    /**
     * Helper methods, returns for a document how often it is currently "in progress" within the
     * project.
     */
    public long getInProgressAmountForDocument(SourceDocument aDocument)
    {
        return allAnnotationDocuments.stream()
                .filter(d -> d.getDocument().equals(aDocument) && d.getState().equals(IN_PROGRESS))
                .count();
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

    public String getUsersWorkingOnTheDocument(SourceDocument aDocument)
    {
        return allAnnotationDocuments.stream()
                .filter(d -> d.getDocument().equals(aDocument) && !d.getState().equals(NEW)
                        && !d.getState().equals(IGNORE))
                .map(AnnotationDocument::getUser).sorted().collect(Collectors.joining(", "));
    }

    public void setAllAnnotationDocuments(List<AnnotationDocument> aListOfAnnotationDocuments)
    {
        allAnnotationDocuments = aListOfAnnotationDocuments;
    }

}
