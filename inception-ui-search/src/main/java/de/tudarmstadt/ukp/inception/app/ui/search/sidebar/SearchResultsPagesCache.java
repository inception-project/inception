/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.tudarmstadt.ukp.inception.search.ResultsGroup;


public class SearchResultsPagesCache implements Serializable
{

    private Map<PageKey, List<ResultsGroup>> pages;

    public SearchResultsPagesCache() {
        pages = new HashMap<>();
    }

    public List<ResultsGroup> getPage(PageKey aPageKey) {
        return pages.get(aPageKey);
    }

    public List<ResultsGroup> getPage(int pageFrom, int pageTo) {
        return pages.get(new PageKey(pageFrom, pageTo));
    }

    public void setPage(PageKey aPageKey, List<ResultsGroup> aPage) {
        pages.put(aPageKey, aPage);
    }

    public void setPage(int pageFrom, int pageTo, List<ResultsGroup> aPage) {
        pages.put(new PageKey(pageFrom, pageTo), aPage);
    }

    public Collection<List<ResultsGroup>> allPages() {
        return pages.values();
    }

    public void clear() {
        pages = new HashMap<>();
    }

    public boolean isEmpty() {
        return pages.isEmpty();
    }

    public boolean containsPage(long from, long to) {
        return pages.containsKey(new PageKey(from, to));
    }

    public static class PageKey implements Serializable {

        private long from;
        private long to;

        public PageKey (long aFrom, long aTo) {
            from = aFrom;
            to = aTo;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            PageKey pageKey = (PageKey) o;
            return from == pageKey.from && to == pageKey.to;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(from, to);
        }
    }
}
