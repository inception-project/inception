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
package de.tudarmstadt.ukp.inception.assistant.tool;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.Tool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibrary;

public class ClockToolLibrary
    implements ToolLibrary
{
    @Override
    public String getId()
    {
        return getClass().getName();
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return true;
    }

    @Tool(value = "get_current_time", actor = "Clock", description = "Returns the current date and time.")
    public Map<String, String> getTime() throws IOException
    {
        var now = LocalDateTime.now(UTC);
        return Map.of( //
                "year", String.valueOf(now.getYear()), //
                "month", String.valueOf(now.getMonthValue()), //
                "day", String.valueOf(now.getDayOfMonth()), //
                "day-of-week", now.getDayOfWeek().toString(), //
                "week-of-year", String.valueOf(now.get(ALIGNED_WEEK_OF_YEAR)), //
                "hour", String.valueOf(now.getHour()), //
                "minute", String.valueOf(now.getMinute()), //
                "second", String.valueOf(now.getSecond()));
    }
}
