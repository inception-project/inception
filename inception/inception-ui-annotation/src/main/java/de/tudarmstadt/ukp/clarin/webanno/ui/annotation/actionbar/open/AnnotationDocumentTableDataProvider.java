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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;

public class AnnotationDocumentTableDataProvider
    extends SortableDataProvider<AnnotationDocument, AnnotationDocumentTableSortKeys>
    implements IFilterStateLocator<AnnotationDocumentTableFilterState>, Serializable
{
    private static final long serialVersionUID = -8262950880527423715L;

    private AnnotationDocumentTableFilterState filterState;
    private IModel<List<AnnotationDocument>> data;

    public AnnotationDocumentTableDataProvider(IModel<List<AnnotationDocument>> aDocuments)
    {
        data = aDocuments;

        // Init filter
        filterState = new AnnotationDocumentTableFilterState();

        // Initial Sorting
        setSort(NAME, ASCENDING);
    }

    @Override
    public Iterator<? extends AnnotationDocument> iterator(long aFirst, long aCount)
    {
        var filteredData = filter(data.getObject());
        filteredData.sort(this::comparator);
        return filteredData //
                .subList((int) aFirst, (int) (aFirst + aCount)) //
                .iterator();
    }

    private int comparator(AnnotationDocument o1, AnnotationDocument o2)
    {
        int dir = getSort().isAscending() ? 1 : -1;
        switch (getSort().getProperty()) {
        case NAME:
            return dir * (o1.getName().compareTo(o2.getName()));
        case STATE:
            return dir * (o1.getState().getName().compareTo(o2.getState().getName()));
        case CREATED:
            return dir * compare(o1.getDocument().getCreated(), o2.getDocument().getCreated());
        case UPDATED:
            return dir * compare(o1.getUpdated(), o2.getUpdated());
        default:
            return 0;
        }
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
