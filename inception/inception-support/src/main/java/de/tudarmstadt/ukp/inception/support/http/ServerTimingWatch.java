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
package de.tudarmstadt.ukp.inception.support.http;

import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;

public class ServerTimingWatch
    implements AutoCloseable
{
    private String key;
    private String description;
    private long startTime;
    private long stopTime;
    private boolean running = false;

    public ServerTimingWatch(String aKey)
    {
        this(aKey, null);
    }

    public ServerTimingWatch(String aKey, String aDescription)
    {
        key = aKey;
        description = aDescription;
        start();
    }

    public void setKey(String aKey)
    {
        key = aKey;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public long getTime()
    {
        if (running) {
            return System.currentTimeMillis() - startTime;
        }
        else {
            return stopTime;
        }
    }

    public void start()
    {
        startTime = System.currentTimeMillis();
        running = true;
    }

    public long stop()
    {
        long stop = getTime();
        running = false;
        return stop;
    }

    @Override
    public void close()
    {
        WicketUtil.serverTiming(key, description, getTime());
    }
}
