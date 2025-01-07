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

import java.time.Duration;

public abstract class DebouncingTask
    extends Task
{
    private final long runnableAfter;

    protected DebouncingTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder);

        runnableAfter = System.currentTimeMillis() + aBuilder.debounceMillis;
    }

    @Override
    public boolean isReadyToStart()
    {
        return System.currentTimeMillis() > runnableAfter;
    }

    public static abstract class Builder<T extends Builder<?>>
        extends Task.Builder<T>
    {
        private long debounceMillis;

        public T withDebounceDelay(Duration aDebounceDelay)
        {
            debounceMillis = aDebounceDelay.toMillis();
            return (T) this;
        }

        public T withDebounceMillis(long aDebounceMillis)
        {
            debounceMillis = aDebounceMillis;
            return (T) this;
        }
    }
}
