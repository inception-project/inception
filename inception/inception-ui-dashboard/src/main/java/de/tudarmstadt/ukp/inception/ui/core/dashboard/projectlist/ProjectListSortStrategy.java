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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING;
import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.DESCENDING;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;

public enum ProjectListSortStrategy
{
    NAME(ProjectListSortKeys.NAME, ASCENDING),
    CREATED_OLDEST(ProjectListSortKeys.CREATED, ASCENDING),
    CREATED_NEWEST(ProjectListSortKeys.CREATED, DESCENDING),
    RECENTLY_UPDATED(ProjectListSortKeys.UPDATED, DESCENDING);

    final ProjectListSortKeys key;
    final SortOrder order;

    ProjectListSortStrategy(ProjectListSortKeys aKey, SortOrder aOrder)
    {
        key = aKey;
        order = aOrder;
    }
}
