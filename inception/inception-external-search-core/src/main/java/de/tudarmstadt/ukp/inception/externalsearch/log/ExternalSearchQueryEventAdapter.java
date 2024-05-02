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
package de.tudarmstadt.ukp.inception.externalsearch.log;

import java.io.IOException;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.externalsearch.event.ExternalSearchQueryEvent;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Component
public class ExternalSearchQueryEventAdapter
    implements EventLoggingAdapter<ExternalSearchQueryEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return ExternalSearchQueryEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public long getProject(ExternalSearchQueryEvent aEvent)
    {
        return aEvent.getProject().getId();
    }

    @Override
    public String getAnnotator(ExternalSearchQueryEvent aEvent)
    {
        return aEvent.getUser();
    }

    @Override
    public String getUser(ExternalSearchQueryEvent aEvent)
    {
        return aEvent.getUser();
    }

    @Override
    public String getDetails(ExternalSearchQueryEvent aEvent) throws IOException
    {
        Details details = new Details();
        details.query = aEvent.getQuery();
        return JSONUtil.toJsonString(details);
    }

    public static class Details
    {
        public String query;
    }
}
