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
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ActivitySummary
{
    public final @JsonProperty String from;
    public final @JsonProperty String to;
    public final @JsonProperty List<ActivitySummaryItem> globalItems;
    public final @JsonProperty Map<String, List<ActivitySummaryItem>> perDocumentItems;

    public ActivitySummary(Instant aFrom, Instant aTo, List<ActivitySummaryItem> aGlobalItems,
            Map<String, List<ActivitySummaryItem>> aPerDocumentItems)
    {
        var formatter = ISO_LOCAL_DATE.withZone(ZoneId.of("UTC"));
        from = formatter.format(aFrom);
        to = formatter.format(aTo);
        globalItems = aGlobalItems;
        perDocumentItems = aPerDocumentItems;
    }
}
