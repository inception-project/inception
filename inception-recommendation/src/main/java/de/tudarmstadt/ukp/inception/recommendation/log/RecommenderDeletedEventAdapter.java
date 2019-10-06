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

package de.tudarmstadt.ukp.inception.recommendation.log;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderDeletedEvent;

@Component
public class RecommenderDeletedEventAdapter
    implements EventLoggingAdapter<RecommenderDeletedEvent>
{
    @Override
    public boolean accepts(Object aEvent)
    {
        return aEvent instanceof RecommenderDeletedEvent;
    }

    @Override
    public String getUser(RecommenderDeletedEvent aEvent)
    {
        return aEvent.getUser();
    }
}
