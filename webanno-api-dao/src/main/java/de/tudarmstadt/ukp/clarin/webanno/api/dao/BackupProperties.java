/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("backup")
public class BackupProperties
{
    private long interval;
    private final KeepOptions keep = new KeepOptions();

    public void setInterval(long aInterval)
    {
        interval = aInterval;
    }
    
    public long getInterval()
    {
        return interval;
    }
    
    public KeepOptions getKeep()
    {
        return keep;
    }
    
    public static class KeepOptions
    {
        private long time;
        private int number;
        public long getTime()
        {
            return time;
        }
        public void setTime(long aTime)
        {
            time = aTime;
        }
        public int getNumber()
        {
            return number;
        }
        public void setNumber(int aNumber)
        {
            number = aNumber;
        }
    }
}
