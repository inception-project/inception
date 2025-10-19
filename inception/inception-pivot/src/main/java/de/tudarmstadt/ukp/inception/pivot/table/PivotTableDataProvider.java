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
import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.inception.pivot.aggregator.CountAggregator;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.Aggregator;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;

public class PivotTableDataProvider<A extends Serializable, T>
    extends SortableDataProvider<Row<A>, CompoundKey>
{
    private static final long serialVersionUID = 758372801344980928L;

    public static final PivotTableDataProvider<Long, Object> EMPTY = PivotTableDataProvider
            .builder(emptyList(), emptyList(), emptyList(), new CountAggregator()).build();

    private final Map<CompoundKey, Map<CompoundKey, A>> rows;
    private final List<CompoundKey> rowKeys = new ArrayList<>();
    private final Set<CompoundKey> columnKeys;
    private final boolean showCellControls;

    public PivotTableDataProvider(Builder<A, T> aBuilder)
    {
        showCellControls = aBuilder.cellBuilder.getSchema().size() > 1;
        rows = aBuilder.rows;
        rowKeys.addAll(aBuilder.rows.keySet());
        columnKeys = aBuilder.columnKeys;
        setSort(ROW_KEY, ASCENDING);
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
        if (getSort() != null && getSort().getProperty() != null) {
            var ascending = getSort().isAscending();
            rowKeys.sort(ascending ? this::rowComparator : (a, b) -> rowComparator(b, a));
        }

        return new Iterator<Row<A>>()
        {
            private final Iterator<CompoundKey> rowKeyIterator = rowKeys.iterator();
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
        return rows.size();
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
            var rowKey = rowBuilder.buildKey(item, aggregator);
            var colKey = colBuilder.buildKey(item, aggregator);
            var cellValue = cellBuilder.buildKey(item, aggregator);

            if (!(rowKey.getSchema().isWeak() && colKey.getSchema().isWeak())) {
                if (rowKey.isWeak() && colKey.isWeak()) {
                    return false;
                }
            }

            columnKeys.add(colKey);

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
