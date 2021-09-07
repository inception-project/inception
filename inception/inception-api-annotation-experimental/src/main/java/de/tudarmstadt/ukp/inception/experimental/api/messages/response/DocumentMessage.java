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
package de.tudarmstadt.ukp.inception.experimental.api.messages.response;

import java.util.List;

import de.tudarmstadt.ukp.inception.experimental.api.model.Viewport;

/**
 * Class required for Messaging between Server and Client.
 * Basis for JSON
 * DocumentMessage: Message published to a specific client containing the data for the requested document
 *
 * Attributes:

 * viewport: List of Viewports and their contents requested by the client
 **/
public class DocumentMessage
{
    private List<Viewport> viewport;

    public DocumentMessage(List<Viewport> aViewport)
    {
        viewport = aViewport;
    }

    public List<Viewport> getViewport()
    {
        return viewport;
    }

    public void setViewport(List<Viewport> aViewport)
    {
        viewport = aViewport;
    }
}
