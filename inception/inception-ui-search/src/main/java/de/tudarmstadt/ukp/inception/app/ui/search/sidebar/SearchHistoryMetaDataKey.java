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

import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

public class SearchHistoryMetaDataKey
    extends MetaDataKey<IModel<List<SearchHistoryItem>>>
{
    private static final long serialVersionUID = 102615176759478581L;

    public final static SearchHistoryMetaDataKey INSTANCE = new SearchHistoryMetaDataKey();

    public static IModel<List<SearchHistoryItem>> get(Component aOwner)
    {
        var options = aOwner.getMetaData(INSTANCE);
        if (options == null) {
            options = new ListModel<>(new ArrayList<>());
            aOwner.setMetaData(INSTANCE, options);
        }
        return options;
    }
}
