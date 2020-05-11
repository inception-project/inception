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

import org.apache.uima.cas.CAS;

/**
 * Object representing the attempt to load a CAS, either containing a CAS or an exception if the
 * load failed.
 */
public class CasHolder
{
    private CasKey key;
    private CAS cas;
    private Exception exception;
    private boolean typeSystemOutdated;

    public CasHolder()
    {
        // Nothing to do
    }
    
    public CasHolder(CasKey aKey, Exception aException)
    {
        key = aKey;
        cas = null;
        exception = aException;
    }

    public CasHolder(CasKey aKey, CAS aCas)
    {
        key = aKey;
        cas = aCas;
        exception = null;
    }
    
    public CasKey getKey()
    {
        return key;
    }

    public CAS getCas()
    {
        return cas;
    }

    public void setCas(CAS aCas)
    {
        cas = aCas;
    }

    public Exception getException()
    {
        return exception;
    }

    public void setException(Exception aException)
    {
        exception = aException;
    }
    
    public synchronized boolean isTypeSystemOutdated()
    {
        return typeSystemOutdated;
    }

    public synchronized void setTypeSystemOutdated(boolean aTypeSystemOutdated)
    {
        typeSystemOutdated = aTypeSystemOutdated;
    }

    public static CasHolder of(CasKey aKey, SupplierThrowingException<CAS> aSupplier)
    {
        try {
            return new CasHolder(aKey, aSupplier.get());
        }
        catch (Exception e) {
            return new CasHolder(aKey, e);
        }
    }
    
    @FunctionalInterface
    public static interface SupplierThrowingException<V>
    {
        V get() throws Exception;
    }
}
