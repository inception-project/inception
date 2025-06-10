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
package de.tudarmstadt.ukp.inception.scheduling;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTaskMonitor
    extends TaskMonitor
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private long lastLog = 0;
    private TaskState lastState;

    public LoggingTaskMonitor(Task aTask)
    {
        super(aTask);
    }

    @Override
    protected void commit()
    {
        if (isDestroyed()) {
            return;
        }

        var now = System.currentTimeMillis();
        var state = getState();
        if (state != lastState || now - lastLog > 5_000) {
            LOG.info("{}: Documents processed: {}/{}", getProject(), getProgress(),
                    getMaxProgress());
            lastLog = now;
            lastState = state;
        }
    }
}
