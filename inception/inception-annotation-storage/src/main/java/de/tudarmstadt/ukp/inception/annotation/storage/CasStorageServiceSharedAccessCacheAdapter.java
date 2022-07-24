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
package de.tudarmstadt.ukp.inception.annotation.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageCacheProperties;

@ManagedResource("de.tudarmstadt.ukp.inception.annotation.storage:type=CasStorageServiceImpl,name=sharedReadOnlyCasCache")
public class CasStorageServiceSharedAccessCacheAdapter
{
    private final CasStorageServiceImpl casStorageService;
    private final CasStorageCacheProperties casStorageProperties;

    @Autowired
    public CasStorageServiceSharedAccessCacheAdapter(CasStorageServiceImpl aCasStorageService,
            CasStorageCacheProperties aCasStorageProperties)
    {
        casStorageService = aCasStorageService;
        casStorageProperties = aCasStorageProperties;
    }

    @ManagedAttribute
    public long getHitCount()
    {
        return casStorageService.getSharedAccessCacheStats().hitCount();
    }

    @ManagedAttribute
    public long getMissCount()
    {
        return casStorageService.getSharedAccessCacheStats().missCount();
    }

    @ManagedAttribute
    public long getLoadSuccessCount()
    {
        return casStorageService.getSharedAccessCacheStats().loadSuccessCount();
    }

    @ManagedAttribute
    public long getLoadFailureCount()
    {
        return casStorageService.getSharedAccessCacheStats().loadFailureCount();
    }

    @ManagedAttribute
    public double getLoadFailureRate()
    {
        return casStorageService.getSharedAccessCacheStats().loadFailureRate();
    }

    @ManagedAttribute
    public long getTotalLoadTime()
    {
        return casStorageService.getSharedAccessCacheStats().totalLoadTime();
    }

    @ManagedAttribute
    public long getEvictionCount()
    {
        return casStorageService.getSharedAccessCacheStats().evictionCount();
    }

    @ManagedAttribute
    public long getEvictionWeight()
    {
        return casStorageService.getSharedAccessCacheStats().evictionWeight();
    }

    @ManagedAttribute
    public double getHitRate()
    {
        return casStorageService.getSharedAccessCacheStats().hitRate();
    }

    @ManagedAttribute
    public double getMissRate()
    {
        return casStorageService.getSharedAccessCacheStats().missRate();
    }

    @ManagedAttribute
    public long getRequestCount()
    {
        return casStorageService.getSharedAccessCacheStats().requestCount();
    }

    @ManagedAttribute
    public long getActiveCount()
    {
        return casStorageService.getSharedAccessCacheSize();
    }

    @ManagedAttribute
    public long getMaxCount()
    {
        return casStorageProperties.getSharedCasCacheSize();
    }

    @ManagedAttribute
    public double getUtilizationRate()
    {
        long max = getMaxCount();

        if (max == 0) {
            return 0;
        }

        return getActiveCount() / max;
    }
}
