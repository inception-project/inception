/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.v2.model.RMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.v2.model.RMessageLevel;

@JsonInclude(Include.NON_NULL)
public class ProjectJSONObject<T>
{
    private List<RMessage> messages = new ArrayList<>();
    private T content;

    public ProjectJSONObject()
    {
        // Nothing to do
    }

    public ProjectJSONObject(RMessageLevel aLevel, String aMessage)
    {
        addMessage(aLevel, aMessage);
    }

    public ProjectJSONObject(T aContent)
    {
        content = aContent;
    }

    public void setContent(T aContent)
    {
        content = aContent;
    }

    public T getContent()
    {
        return content;
    }

    public void addMessage(RMessageLevel aLevel, String aMessage)
    {
        messages.add(new RMessage(aLevel, aMessage));
    }

    public List<RMessage> getMessages()
    {
        return messages;
    }
}
