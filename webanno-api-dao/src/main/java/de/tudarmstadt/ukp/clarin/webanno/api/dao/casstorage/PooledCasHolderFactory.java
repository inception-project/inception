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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

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
        return !aP.getObject().isTypeSystemOutdated();
    }
}
