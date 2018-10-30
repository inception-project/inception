/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.search.log;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.search.event.SearchQueryEvent;

@Component
public class SearchQueryEventAdapter
    implements EventLoggingAdapter<SearchQueryEvent>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean accepts(Object aEvent)
    {
        return aEvent instanceof SearchQueryEvent;
    }

    @Override
    public long getProject(SearchQueryEvent aEvent)
    {
        return aEvent.getProject().getId();
    }

    @Override
    public String getAnnotator(SearchQueryEvent aEvent)
    {
        return aEvent.getUser();
    }

    @Override
    public String getUser(SearchQueryEvent aEvent)
    {
        return aEvent.getUser();
    }

    @Override
    public String getDetails(SearchQueryEvent aEvent)
    {
        try {
            Details details = new Details();

            details.query = aEvent.getQuery();

            return JSONUtil.toJsonString(details);
        }
        catch (IOException e) {
            log.error("Unable to log event [{}]", aEvent, e);
            return "<ERROR>";
        }
    }

    public static class Details
    {
        public String query;
    }
}
