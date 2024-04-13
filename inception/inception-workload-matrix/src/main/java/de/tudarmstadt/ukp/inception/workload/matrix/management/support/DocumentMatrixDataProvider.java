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
package de.tudarmstadt.ukp.inception.workload.matrix.management.support;

import static de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixSortKey.DOCUMENT_NAME;
import static java.util.Collections.emptyList;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class DocumentMatrixDataProvider
    extends SortableDataProvider<DocumentMatrixRow, DocumentMatrixSortKey>
    implements IFilterStateLocator<DocumentMatrixFilterState>, Serializable
{
    private static final long serialVersionUID = -3869576909905361406L;

    private List<DocumentMatrixRow> matrixData;
    private DocumentMatrixFilterState filter;

    public DocumentMatrixDataProvider(List<DocumentMatrixRow> aData)
    {
        matrixData = aData;

        setSort(DOCUMENT_NAME, ASCENDING);
    }

    public void setMatrixData(List<DocumentMatrixRow> aMatrixData)
    {
        matrixData = aMatrixData;
    }

    public List<DocumentMatrixRow> getMatrixData()
    {
        return filter(matrixData);
    }

    @Override
    public Iterator<? extends DocumentMatrixRow> iterator(long aFirst, long aCount)
    {
        // Apply Filter
        var newList = getMatrixData();

        // Apply sorting
        newList.sort((o1, o2) -> {
            int dir = getSort().isAscending() ? 1 : -1;
            return dir * getSort().getProperty().compare(o1, o2);
        });

        if ((int) aFirst + (int) aCount > newList.size()) {
            aCount = newList.size() - aFirst;
        }

        return newList.subList((int) aFirst, ((int) aFirst + (int) aCount)).iterator();
    }

    @Override
    public long size()
    {
        return getMatrixData().size();
    }

    private List<DocumentMatrixRow> filter(List<DocumentMatrixRow> aData)
    {
        var rowStream = matrixData.stream();

        String documentNameFilter = filter.getDocumentName();
        if (isNotBlank(documentNameFilter)) {
            if (filter.isMatchDocumentNameAsRegex()) {
                try {
                    var p = Pattern.compile(".*(" + documentNameFilter + ").*", CASE_INSENSITIVE)
                            .asMatchPredicate();
                    rowStream = rowStream.filter(row -> p.test(row.getSourceDocument().getName()));
                }
                catch (PatternSyntaxException e) {
                    return emptyList();
                }
            }
            else {
                rowStream = rowStream
                        .filter(row -> containsIgnoreCase(row.getSourceDocument().getName(),
                                documentNameFilter));
            }
        }

        // Filter by document states
        if (isNotEmpty(filter.getStates())) {
            rowStream = rowStream.filter(doc -> filter.getStates().contains(doc.getState()));
        }

        return rowStream.collect(toCollection(ArrayList::new));
    }

    @Override
    public IModel<DocumentMatrixRow> model(DocumentMatrixRow aRow)
    {
        return Model.of(aRow);
    }

    @Override
    public DocumentMatrixFilterState getFilterState()
    {
        return filter;
    }

    @Override
    public void setFilterState(DocumentMatrixFilterState aState)
    {
        filter = aState;
    }
}
