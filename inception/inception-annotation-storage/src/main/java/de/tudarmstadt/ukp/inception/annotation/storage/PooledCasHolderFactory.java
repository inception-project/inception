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

import static org.apache.commons.pool2.PooledObjectState.RETURNING;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasHolder;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasKey;

public class PooledCasHolderFactory
    extends BaseKeyedPooledObjectFactory<CasKey, CasHolder>
{
    @Override
    public CasHolder create(CasKey aKey) throws Exception
    {
        return new CasHolder(aKey);
    }

    @Override
    public PooledObject<CasHolder> wrap(CasHolder aCas)
    {
        return new DefaultPooledObject<CasHolder>(aCas);
    }

    @Override
    public boolean validateObject(CasKey aKey, PooledObject<CasHolder> aP)
    {
        var casHolder = aP.getObject();
        // When the holder is being returned, we do not need to keep the holder if the CAS has not
        // been loaded - no need to cache an empty holder, we can easily recreate it. Keeping it
        // just gives us a wrong impression of the fill state of the cache.
        if (aP.getState() == RETURNING && !casHolder.isCasSet()) {
            return false;
        }

        // If the type system has changed or the CAS being held has been deleted, we can
        // also discard the holder. This forces a re-load from disk next time the CAS is
        // accessed.
        if (casHolder.isTypeSystemOutdated() || casHolder.isDeleted()) {
            return false;
        }

        return true;
    }
}
