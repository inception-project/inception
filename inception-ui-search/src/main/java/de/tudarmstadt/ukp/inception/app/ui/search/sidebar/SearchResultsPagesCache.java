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
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import de.tudarmstadt.ukp.inception.search.ResultsGroup;

public class SearchResultsPagesCache
    implements Serializable
{
    private static final long serialVersionUID = 2219896532886718940L;

    private Map<PageKey, List<ResultsGroup>> pages;

    public SearchResultsPagesCache()
    {
        pages = new HashMap<>();
    }

    public List<ResultsGroup> getPage(long pageFirst, long pageCount)
    {
        return pages.get(new PageKey(pageFirst, pageCount));
    }

    public void putPage(long pageFirst, long pageCount, List<ResultsGroup> aPage)
    {
        pages.put(new PageKey(pageFirst, pageCount), aPage);
    }

    public List<ResultsGroup> allResultsGroups()
    {
        return pages.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public void clear()
    {
        pages = new HashMap<>();
    }

    public boolean isEmpty()
    {
        return pages.isEmpty();
    }

    public boolean containsPage(long first, long count)
    {
        return pages.containsKey(new PageKey(first, count));
    }

    private static class PageKey
        implements Serializable
    {
        private static final long serialVersionUID = 9167352821262381017L;

        private long first;
        private long count;

        public PageKey(long aFirst, long aCount)
        {
            first = aFirst;
            count = aCount;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PageKey pageKey = (PageKey) o;
            return first == pageKey.first && count == pageKey.count;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(first, count);
        }
    }
}
