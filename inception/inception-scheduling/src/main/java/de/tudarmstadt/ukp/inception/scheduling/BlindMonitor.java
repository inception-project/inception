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

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.function.Consumer;

public class BlindMonitor
    implements Monitor
{
    private static final BlindMonitor INSTANCE = new BlindMonitor();

    public static Monitor blindMonitor()
    {
        return INSTANCE;
    }

    @Override
    public ProgressScope openScope(String aUnit, int aMaxProgress)
    {
        return new ProgressScope()
        {
            @Override
            public int getProgress()
            {
                return 0;
            }

            @Override
            public void update(Consumer<ProgressUpdate> aUpdater)
            {
                // Do nothing
            }

            @Override
            public void close()
            {
                // Do nothing
            }
        };
    }

    @Override
    public void update(Consumer<MonitorUpdate> aUpdate)
    {
        // Do nothing
    }

    @Override
    public List<Progress> getProgressList()
    {
        return emptyList();
    }

    @Override
    public int getMaxProgress()
    {
        return 0;
    }

    @Override
    public int getProgress()
    {
        return 0;
    }

    @Override
    public boolean isCancelled()
    {
        return false;
    }

    @Override
    public long getDuration()
    {
        return 0;
    }
}
