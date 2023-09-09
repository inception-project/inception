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
package de.tudarmstadt.ukp.inception.annotation.storage.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link CasStorageServiceAutoConfiguration}.
 * </p>
 */
@ConfigurationProperties("cas-storage.cache")
public class CasStorageCachePropertiesImpl
    implements CasStorageCacheProperties
{
    private Duration idleCasEvictionDelay = Duration.ofMinutes(5);
    private Duration minIdleCasTime = Duration.ofMinutes(5);
    private Duration casBorrowWaitTimeout = Duration.ofMinutes(3);
    private long sharedCasCacheSize = getDefaultCasCacheSize();

    @Override
    public Duration getIdleCasEvictionDelay()
    {
        return idleCasEvictionDelay;
    }

    public void setIdleCasEvictionDelay(Duration aIdleCasEvictionDelay)
    {
        idleCasEvictionDelay = aIdleCasEvictionDelay;
    }

    @Override
    public Duration getCasBorrowWaitTimeout()
    {
        return casBorrowWaitTimeout;
    }

    public void setCasBorrowWaitTimeout(Duration aCasBorrowWaitTimeout)
    {
        casBorrowWaitTimeout = aCasBorrowWaitTimeout;
    }

    @Override
    public long getSharedCasCacheSize()
    {
        return sharedCasCacheSize;
    }

    public void setSharedCasCacheSize(long aSharedCasCacheSize)
    {
        sharedCasCacheSize = aSharedCasCacheSize;
    }

    @Override
    public Duration getMinIdleCasTime()
    {
        return minIdleCasTime;
    }

    public void setMinIdleCasTime(Duration aMinIdleCasTime)
    {
        minIdleCasTime = aMinIdleCasTime;
    }

    private static final long MB = 1024 * 1024;

    public static long getDefaultCasCacheSize()
    {
        long maxMemory = Runtime.getRuntime().maxMemory();

        if (maxMemory < 256 * MB) {
            return 10;
        }

        if (maxMemory < 512 * MB) {
            return 50;
        }

        if (maxMemory < 1024 * MB) {
            return 250;
        }

        if (maxMemory < 2048 * MB) {
            return 750;
        }

        if (maxMemory < 4096 * MB) {
            return 1500;
        }

        return 5000;
    }
}
