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
package de.tudarmstadt.ukp.inception.externaleditor.command;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.request.cycle.RequestCycle;

public class QueuedEditorCommandsMetaDataKey
    extends MetaDataKey<HashMap<String, CommandQueue>>
{
    private static final long serialVersionUID = 102615176759478581L;

    public final static QueuedEditorCommandsMetaDataKey INSTANCE = new QueuedEditorCommandsMetaDataKey();

    /**
     * Get the per-request command queue for the given editor. The queue is keyed by the editor's
     * markup ID so that each editor accumulates and drains only its own commands. Multiple editors
     * may share a page (e.g. the main editor plus a read-only reference-document sidebar viewer);
     * without per-editor scoping, rendering one editor would replay another editor's queued
     * commands - so paging one editor could scroll the other.
     *
     * @param aEditor
     *            the editor whose command queue is requested
     * @return the command queue for that editor in the current request cycle
     */
    public static CommandQueue get(Component aEditor)
    {
        var requestCycle = RequestCycle.get();
        var queues = requestCycle.getMetaData(INSTANCE);
        if (queues == null) {
            queues = new HashMap<>();
            requestCycle.setMetaData(INSTANCE, queues);
        }
        return queues.computeIfAbsent(aEditor.getMarkupId(), $ -> new CommandQueue());
    }
}
