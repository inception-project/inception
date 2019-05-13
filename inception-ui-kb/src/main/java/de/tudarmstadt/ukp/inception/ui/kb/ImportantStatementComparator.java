/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.ui.kb;

import java.util.Comparator;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.model.StatementGroupBean;

/**
 * Comparator which sorts specifiable "important" {@link KBHandle}s to the front. As a secondary
 * criterion, {@link KBHandle}s are sorted in lexical order by their UI label.
 */
public class ImportantStatementComparator<T>
    implements Comparator<StatementGroupBean>
{
    private final Function<StatementGroupBean, T> keyExtractor;
    private final Function<T, Boolean> important;
    private final LoadingCache<T, Integer> cache;

    public ImportantStatementComparator(Function<StatementGroupBean, T> aKeyExtractor,
            Function<T, Boolean> aImportant)
    {
        keyExtractor = aKeyExtractor;
        important = aImportant;
        cache = Caffeine.newBuilder().build(key -> important.apply(key) ? 0 : 1);
    }

    @Override
    public int compare(StatementGroupBean aGroup1, StatementGroupBean aGroup2)
    {
        int h1Importance = cache.get(keyExtractor.apply(aGroup1));
        int h2Importance = cache.get(keyExtractor.apply(aGroup2));

        if (h1Importance == h2Importance) {
            return aGroup1.getProperty().getUiLabel()
                    .compareToIgnoreCase(aGroup2.getProperty().getUiLabel());
        }
        else {
            return Integer.compare(h1Importance, h2Importance);
        }
    }
}
