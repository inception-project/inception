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

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.uima.cas.CAS;

/**
 * Object representing the attempt to load a CAS, either containing a CAS or an exception if the
 * load failed.
 */
public class CasHolder
{
    private final CasKey key;
    
    private CAS cas;
    private Exception exception;
    private boolean typeSystemOutdated;
    private boolean deleted;

    public CasHolder(CasKey aKey)
    {
        key = aKey;
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
        setCas(aCas);
        exception = null;
    }
    
    public CasKey getKey()
    {
        return key;
    }
    
    public boolean isCasSet()
    {
        return cas != null;
    }

    public CAS getCas()
    {
        if (cas == null) {
            throw new IllegalStateException("Trying to get CAS before it was set");
        }
        
        return cas;
    }

    public void setCas(CAS aCas)
    {
        Validate.notNull(aCas, "CAS cannot be null");
        
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

    public synchronized  void setDeleted(boolean aDeleted)
    {
        deleted = aDeleted;
    }
    
    public synchronized  boolean isDeleted()
    {
        return deleted;
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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE).append("key", key)
                .append("deleted", deleted).append("typeSystemOutdated", typeSystemOutdated)
                .toString();
    }
    
    
}
