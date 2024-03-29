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

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.request.cycle.RequestCycle;

public class QueuedEditorCommandsMetaDataKey
    extends MetaDataKey<CommandQueue>
{
    private static final long serialVersionUID = 102615176759478581L;

    public final static QueuedEditorCommandsMetaDataKey INSTANCE = new QueuedEditorCommandsMetaDataKey();

    public static CommandQueue get()
    {
        RequestCycle requestCycle = RequestCycle.get();
        CommandQueue commandQueue = requestCycle.getMetaData(INSTANCE);
        if (commandQueue == null) {
            commandQueue = new CommandQueue();
            requestCycle.setMetaData(INSTANCE, commandQueue);
        }
        return commandQueue;
    }
}
