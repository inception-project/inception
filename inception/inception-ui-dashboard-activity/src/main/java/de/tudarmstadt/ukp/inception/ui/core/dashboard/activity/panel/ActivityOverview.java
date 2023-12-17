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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.activity.panel;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ActivityOverview
{
    public final @JsonProperty String from;
    public final @JsonProperty String to;
    public final @JsonProperty Map<String, ActivityOverviewItem> items;

    public ActivityOverview(Instant aFrom, Instant aTo, Map<Instant, ActivityOverviewItem> aItems)
    {
        var dateFormat = ISO_LOCAL_DATE.withZone(ZoneId.of("UTC"));
        from = dateFormat.format(aFrom);
        to = dateFormat.format(aTo);

        items = new LinkedHashMap<String, ActivityOverviewItem>();
        for (var entry : aItems.entrySet()) {
            items.put(dateFormat.format(entry.getKey()), entry.getValue());
        }
    }
}
