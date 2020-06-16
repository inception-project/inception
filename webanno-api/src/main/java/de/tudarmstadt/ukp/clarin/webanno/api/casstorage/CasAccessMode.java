/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.clarin.webanno.api.casstorage;

public enum CasAccessMode
{
    /**
     * The CAS is will be exclusively owned by the caller. The caller may make modifications to the
     * CAS and optionally save these modifications to the storage. The modifications will not be
     * visible to other threads until the CAS has been written and the session has released its
     * lock on the CAS.
     */
    EXCLUSIVE_WRITE_ACCESS,
    
    /**
     * The CAS may be shared between multiple callers. The callers must not make any kind of 
     * modifications to the CAS and they cannot save the CAS.
     */
    SHARED_READ_ONLY_ACCESS,
    
    /**
     * Loads a CAS directly from the storage without fetching it from a pool or cache, without
     * adding it to a cache and without adding it to a session. Also, no analysis and repairs will
     * be performed.
     * <p>
     * <b>NOTE:</b> This mode is reserved for very special occasions. E.g. when loading an
     * annotation CAS defers to loading the INITIAL_CAS, or when initializing the curation CAS.
     */
    UNMANAGED_ACCESS,
    /**
     * Loads a CAS directly from the storage without fetching it from a pool or cache, without 
     * adding it to a cache and without adding it to a session. Also, no analysis and repairs
     * will be performed. Finally, it does not consider any possibly supplied CAS provider. If the
     * file does not exist in the storage already, an exception will be generated.
     * <p>
     * <b>NOTE:</b> This mode is reserved for very special occasions, e.g. when the CASDoctor needs
     * to load the current state from disk.
     */
    UNMANAGED_NON_INITIALIZING_ACCESS;
    
    public boolean alsoPermits(CasAccessMode aOtherMode)
    {
        switch (this) {
        case EXCLUSIVE_WRITE_ACCESS:
            return EXCLUSIVE_WRITE_ACCESS.equals(aOtherMode) || 
                    SHARED_READ_ONLY_ACCESS.equals(aOtherMode); 
        case SHARED_READ_ONLY_ACCESS:
            return SHARED_READ_ONLY_ACCESS.equals(aOtherMode);
        case UNMANAGED_ACCESS:
            return false;
        case UNMANAGED_NON_INITIALIZING_ACCESS:
            return false;
        default:
            throw new IllegalArgumentException("Unknown mode: [" + aOtherMode + "]");
        }
    }
}
