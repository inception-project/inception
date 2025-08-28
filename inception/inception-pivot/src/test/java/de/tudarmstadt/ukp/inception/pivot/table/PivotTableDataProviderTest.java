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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.pivot.aggregator.CountAggregator;
import de.tudarmstadt.ukp.inception.pivot.aggregator.ValueSetAggregator;
import de.tudarmstadt.ukp.inception.pivot.extractor.LambdaExtractor;

class PivotTableDataProviderTest
{
    @Test
    void testProviderWithCountAggregator()
    {
        record Item(String v1, String v2, String v3)
            implements Serializable
        {
            private static final long serialVersionUID = 1L;
        }

        var rowExtractor = new LambdaExtractor<>("row", Item.class, String.class, Item::v1);
        var colExtractor = new LambdaExtractor<>("col", Item.class, String.class, Item::v2);
        var cellExtractor = new LambdaExtractor<>("cell", Item.class, String.class, Item::v3);
        var aggregator = new CountAggregator();
        var builder = PivotTableDataProvider.builder(List.of(rowExtractor), List.of(colExtractor),
                List.of(cellExtractor), aggregator);

        builder.add(new Item("A", "a", "1"));
        builder.add(new Item("A", "a", "1"));
        builder.add(new Item("A", "a", "1"));

        var provider = builder.build();

        assertThat(provider.iterator(0, 10)) //
                .as("aggregates") //
                .toIterable() //
                .flatExtracting(row -> row.data().values()) //
                .containsExactly(3L);

        assertThat(provider.columnKeys()) //
                .as("column keys") //
                .extracting(CompoundKey::toString) //
                .containsExactly("{\"col\":\"a\"}");

        assertThat(provider.rowKeys()) //
                .as("row keys") //
                .extracting(CompoundKey::toString) //
                .containsExactly("{\"row\":\"A\"}");
    }

    @Test
    void testProviderWithValueSetAggregator()
    {
        record Item(String v1, String v2, String v3)
            implements Serializable
        {
            private static final long serialVersionUID = 1L;
        }

        var rowExtractor = new LambdaExtractor<>("row", Item.class, String.class, Item::v1);
        var colExtractor = new LambdaExtractor<>("col", Item.class, String.class, Item::v2);
        var cellExtractor = new LambdaExtractor<>("cell", Item.class, String.class, Item::v3);

        var rowKeyBuilder = new CompoundKeyBuilder<>(List.of(rowExtractor));
        var colKeyBuilder = new CompoundKeyBuilder<>(List.of(colExtractor));
        var cellKeyBuilder = new CompoundKeyBuilder<>(List.of(cellExtractor));

        var aggregator = new ValueSetAggregator();
        var builder = PivotTableDataProvider.builder(rowKeyBuilder, colKeyBuilder, cellKeyBuilder,
                aggregator);

        var item1 = new Item("A", "a", "1");
        var item2 = new Item("A", "a", "2");
        var item3 = new Item("A", "a", "3");
        builder.add(item1);
        builder.add(item2);
        builder.add(item3);

        var key1 = cellKeyBuilder.buildKey(item1, aggregator);
        var key2 = cellKeyBuilder.buildKey(item2, aggregator);
        var key3 = cellKeyBuilder.buildKey(item3, aggregator);

        var provider = builder.build();

        assertThat(provider.columnKeys()) //
                .as("column keys") //
                .extracting(CompoundKey::toString) //
                .containsExactly("{\"col\":\"a\"}");

        assertThat(provider.rowKeys()) //
                .as("row keys") //
                .extracting(CompoundKey::toString) //
                .containsExactly("{\"row\":\"A\"}");

        assertThat(provider.iterator(0, 10)) //
                .as("aggregates") //
                .toIterable() //
                .flatExtracting(row -> row.data().values()) //
                .containsExactly(new LinkedHashSet<>(asList(key1, key2, key3)));
    }
}
