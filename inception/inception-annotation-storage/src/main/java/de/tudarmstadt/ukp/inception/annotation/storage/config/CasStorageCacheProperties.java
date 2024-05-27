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

public interface CasStorageCacheProperties
{
    /**
     * @return maximum time to wait when trying to perform an exclusive action on a CAS for another
     *         exclusive action to finish.
     */
    Duration getCasBorrowWaitTimeout();

    /**
     * @return time how often the exclusive-access pool is checked for idle CASes that can be
     *         removed and how long shared-access CASes remain the cache when not being used.
     */
    Duration getIdleCasEvictionDelay();

    /**
     * @return time that a CAS should remain in the exclusive-access pool before being evicted. This
     *         is meant to ensure that CASes that are used regularly remain a while in the pool
     *         before we have to read them from disk again.
     */
    Duration getMinIdleCasTime();

    /**
     * @return number of CAS instances that should be kept in memory for shared-read-only access.
     */
    long getSharedCasCacheSize();
}
