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

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

public class SearchHistoryState
    implements PreferenceValue
{
    public static final PreferenceKey<SearchHistoryState> KEY_SEARCH_HISTORY = new PreferenceKey<>(
            SearchHistoryState.class, "annotation/search/history");

    private static final long serialVersionUID = -7670170712405879737L;

    private final List<SearchHistoryItem> historyItems = new ArrayList<>();

    public void setHistoryItems(List<SearchHistoryItem> aHistoryItems)
    {
        historyItems.clear();
        if (aHistoryItems != null) {
            historyItems.addAll(aHistoryItems);
        }
    }

    public List<SearchHistoryItem> getHistoryItems()
    {
        return historyItems;
    }
}
