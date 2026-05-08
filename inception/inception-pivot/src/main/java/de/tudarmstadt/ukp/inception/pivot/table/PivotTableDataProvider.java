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
package de.tudarmstadt.ukp.inception.pivot.table;

import static de.tudarmstadt.ukp.inception.pivot.table.CompoundKey.ROW_KEY;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.inception.pivot.aggregator.CountAggregator;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.Aggregator;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;

public class PivotTableDataProvider<A extends Serializable, T>
    extends SortableDataProvider<Row<A>, CompoundKey>
    implements IFilterStateLocator<PivotTableFilterState>
{
    private static final long serialVersionUID = 758372801344980928L;

    public static final PivotTableDataProvider<Long, Object> EMPTY = PivotTableDataProvider
            .builder(emptyList(), emptyList(), emptyList(), new CountAggregator()).build();

    private final Map<CompoundKey, Map<CompoundKey, A>> rows;
    private final List<CompoundKey> rowKeys = new ArrayList<>();
    private final Set<CompoundKey> columnKeys;
    private final boolean showCellControls;
    private int offeredItemCount;
    private int addedItemCount;

    private PivotTableFilterState filterState;

    public PivotTableDataProvider(Builder<A, T> aBuilder)
    {
        showCellControls = aBuilder.cellBuilder.getSchema().size() > 1;
        rows = aBuilder.rows;
        rowKeys.addAll(aBuilder.rows.keySet());
        columnKeys = aBuilder.columnKeys;
        setSort(ROW_KEY, ASCENDING);
        filterState = new PivotTableFilterState();
        addedItemCount = aBuilder.addedItemCount;
        offeredItemCount = aBuilder.offeredItemCount;
    }

    public int getAddedItemCount()
    {
        return addedItemCount;
    }

    public int getOfferedItemCount()
    {
        return offeredItemCount;
    }

    @Override
    public PivotTableFilterState getFilterState()
    {
        return filterState;
    }

    @Override
    public void setFilterState(PivotTableFilterState aState)
    {
        filterState = aState;
    }

    private Stream<CompoundKey> filter(Map<CompoundKey, Map<CompoundKey, A>> aData)
    {
        var rowKeyStream = aData.keySet().stream();

        if (filterState.isHideRowsWithSameValuesInAllColumns()
                || filterState.isHideRowsWithAnyDifferentValue()) {
            rowKeyStream = rowKeyStream.filter(row -> {
                var values = aData.get(row).values();
                if (isNotEmpty(values)) {
                    var first = values.iterator().next();
                    var matchCount = values.stream() //
                            .filter(v -> Objects.equals(first, v)) //
                            .count();

                    if (filterState.isHideRowsWithSameValuesInAllColumns()) {
                        // There is at least one column that has a different value than the others
                        return matchCount != columnKeys.size();
                    }
                    else {
                        // I.e. if filterState.isHideRowsWithAnyDifferentValue()
                        // All columns have the same value
                        return matchCount == columnKeys.size();
                    }
                }

                // All columns are empty (have the same value)
                return !filterState.isHideRowsWithSameValuesInAllColumns();
            });
        }

        if (filterState.isHideRowsWithEmptyValues() || filterState.isHideRowsWithoutEmptyValues()) {
            rowKeyStream = rowKeyStream.filter(row -> {
                var values = aData.get(row).values();
                if (isNotEmpty(values)) {
                    var nonNullCount = values.stream() //
                            .filter(Objects::nonNull) //
                            .count();
                    if (filterState.isHideRowsWithEmptyValues()) {
                        return nonNullCount == columnKeys.size();
                    }
                    else {
                        // I.e. if filterState.isHideRowsWithoutEmptyValues()
                        return nonNullCount < columnKeys.size();
                    }
                }

                // All columns are empty
                return !filterState.isHideRowsWithEmptyValues();
            });
        }

        return rowKeyStream;
    }

    private Stream<CompoundKey> sort(Stream<CompoundKey> aRowKeyStream)
    {
        var rowKeyStream = aRowKeyStream;
        if (getSort() != null && getSort().getProperty() != null) {
            var ascending = getSort().isAscending();
            rowKeyStream = rowKeyStream
                    .sorted(ascending ? this::rowComparator : (a, b) -> rowComparator(b, a));
        }

        return rowKeyStream;
    }

    public Map<CompoundKey, Map<CompoundKey, A>> getRows()
    {
        return rows;
    }

    @SuppressWarnings("unchecked")
    private int rowComparator(CompoundKey aKey1, CompoundKey aKey2)
    {
        var sortKey = getSort().getProperty();

        var value1 = sortKey == ROW_KEY ? aKey1 : rows.get(aKey1).get(sortKey);
        var value2 = sortKey == ROW_KEY ? aKey2 : rows.get(aKey2).get(sortKey);

        if (value1 == null && value2 == null) {
            return 0;
        }

        if (value1 == null) {
            return -1;
        }

        if (value2 == null) {
            return 1;
        }

        if (value1 instanceof Comparable cmp1 && value2 instanceof Comparable cmp2) {
            return cmp1.compareTo(cmp2);
        }

        return 0;
    }

    @Override
    public Iterator<Row<A>> iterator(long first, long count)
    {
        var visibleRowKeys = sort(filter(rows)).toList();

        return new Iterator<Row<A>>()
        {
            private final Iterator<CompoundKey> rowKeyIterator = visibleRowKeys.iterator();
            private long currentIndex = 0;
            private boolean skipped = false;

            private void skip()
            {
                if (!skipped) {
                    for (var i = 0; i < first && rowKeyIterator.hasNext(); i++) {
                        rowKeyIterator.next();
                        currentIndex++;
                    }
                    skipped = true;
                }
            }

            @Override
            public boolean hasNext()
            {
                skip();
                return rowKeyIterator.hasNext() && currentIndex < first + count;
            }

            @Override
            public Row<A> next()
            {
                if (!hasNext()) {
                    throw new IllegalStateException("No more elements");
                }
                var entry = rowKeyIterator.next();
                currentIndex++;
                return new Row<>(entry, rows.get(entry));
            }
        };
    }

    public Collection<CompoundKey> columnKeys()
    {
        return columnKeys;
    }

    public Collection<CompoundKey> rowKeys()
    {
        return rowKeys;
    }

    public boolean isShowCellControls()
    {
        return showCellControls;
    }

    @Override
    public long size()
    {
        return filter(rows).count();
    }

    @Override
    public IModel<Row<A>> model(Row<A> object)
    {
        return Model.of(object);
    }

    public static <A extends Serializable, T> Builder<A, T> builder(
            List<? extends Extractor<T, ? extends Serializable>> rowExtractors,
            List<? extends Extractor<T, ? extends Serializable>> colExtractors,
            List<? extends Extractor<T, ? extends Serializable>> cellExtractors,
            Aggregator<A, Object> aAggregator)
    {
        return new Builder<>(new CompoundKeyBuilder<>(rowExtractors),
                new CompoundKeyBuilder<>(colExtractors), new CompoundKeyBuilder<>(cellExtractors),
                aAggregator);
    }

    public static <A extends Serializable, T> Builder<A, T> builder(
            CompoundKeyBuilder<T> aRowBuilder, CompoundKeyBuilder<T> aColBuilder,
            CompoundKeyBuilder<T> aCellBuilder, Aggregator<A, Object> aAggregator)
    {
        return new Builder<>(aRowBuilder, aColBuilder, aCellBuilder, aAggregator);
    }

    public static class Builder<A extends Serializable, T>
    {
        private final CompoundKeyBuilder<T> rowBuilder;
        private final CompoundKeyBuilder<T> colBuilder;
        private final CompoundKeyBuilder<T> cellBuilder;
        private final Aggregator<A, Object> aggregator;

        private final Map<CompoundKey, Map<CompoundKey, A>> rows = new HashMap<>();
        private final Set<CompoundKey> columnKeys = new TreeSet<>();

        private int offeredItemCount = 0;
        private int addedItemCount = 0;

        private Builder(CompoundKeyBuilder<T> aRowBuilder, CompoundKeyBuilder<T> aColBuilder,
                CompoundKeyBuilder<T> aCellBuilder, Aggregator<A, Object> aAggregator)
        {
            rowBuilder = aRowBuilder;
            colBuilder = aColBuilder;
            cellBuilder = aCellBuilder;
            aggregator = aAggregator;
        }

        public boolean add(T item)
        {
            offeredItemCount++;

            var rowKey = rowBuilder.buildKey(item, aggregator);
            var colKey = colBuilder.buildKey(item, aggregator);
            var cellValue = cellBuilder.buildKey(item, aggregator);

            if (!(rowKey.getSchema().isWeak() && colKey.getSchema().isWeak())) {
                if (rowKey.isWeak() && colKey.isWeak()) {
                    return false;
                }
            }

            columnKeys.add(colKey);

            addedItemCount++;
            rows.computeIfAbsent(rowKey, rk -> new HashMap<>()).compute(colKey, (k, v) -> {
                if (v == null) {
                    v = aggregator.initialValue();
                }
                return aggregator.aggregate(v, cellValue);
            });

            return true;
        }

        public PivotTableDataProvider<A, T> build()
        {
            return new PivotTableDataProvider<>(this);
        }
    }
}
