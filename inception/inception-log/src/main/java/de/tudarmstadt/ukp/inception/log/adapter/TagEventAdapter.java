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
package de.tudarmstadt.ukp.inception.log.adapter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.log.model.TagDetails;
import de.tudarmstadt.ukp.inception.schema.api.event.TagEvent;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Component
public class TagEventAdapter
    implements EventLoggingAdapter<TagEvent>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return TagEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public long getProject(TagEvent aEvent)
    {
        return aEvent.getTag().getTagSet().getProject().getId();
    }

    @Override
    public String getDetails(TagEvent aEvent)
    {
        try {
            var details = new TagDetails(aEvent.getTag());
            return JSONUtil.toJsonString(details);
        }
        catch (IOException e) {
            log.error("Unable to log event [{}]", aEvent, e);
            return "<ERROR>";
        }
    }
}
