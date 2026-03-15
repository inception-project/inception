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
package de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.uima.cas.CAS;

/**
 * Object representing the attempt to load a CAS, either containing a CAS or an exception if the
 * load failed.
 */
public class CasHolder
{
    private final CasKey key;
    private final Exception exception;

    private CAS cas;
    private boolean typeSystemOutdated = false;
    private boolean deleted = false;

    // Transient runtime owner information (set when the holder is handed out by the pool)
    private transient volatile long ownerThreadId = -1L;
    private transient volatile String ownerThreadName;
    private transient volatile long ownerSinceMillis = 0L;
    private transient volatile StackTraceElement[] ownerStackTrace;

    // Toggle capturing stack traces for owners (disabled by default because of overhead)
    private static volatile boolean traceAccessEnabled = false;

    public CasHolder(CasKey aKey)
    {
        key = aKey;
        exception = null;
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

    public synchronized boolean isTypeSystemOutdated()
    {
        return typeSystemOutdated;
    }

    public synchronized void setTypeSystemOutdated(boolean aTypeSystemOutdated)
    {
        typeSystemOutdated = aTypeSystemOutdated;
    }

    public synchronized void setDeleted(boolean aDeleted)
    {
        deleted = aDeleted;
    }

    public synchronized boolean isDeleted()
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

    public String getCasHashCode()
    {
        if (cas != null) {
            return String.valueOf(cas.hashCode());
        }
        else {
            return "<unset>";
        }
    }

    public synchronized void setOwner(Thread aThread)
    {
        if (aThread == null) {
            ownerThreadId = -1L;
            ownerThreadName = null;
            ownerSinceMillis = 0L;
            ownerStackTrace = null;
        }
        else {
            ownerThreadId = aThread.threadId();
            ownerThreadName = aThread.getName();
            ownerSinceMillis = System.currentTimeMillis();
            if (traceAccessEnabled) {
                try {
                    ownerStackTrace = aThread.getStackTrace();
                }
                catch (Throwable t) {
                    ownerStackTrace = null;
                }
            }
            else {
                ownerStackTrace = null;
            }
        }
    }

    public synchronized void clearOwner()
    {
        ownerThreadId = -1L;
        ownerThreadName = null;
        ownerSinceMillis = 0L;
        ownerStackTrace = null;
    }

    public long getOwnerThreadId()
    {
        return ownerThreadId;
    }

    public String getOwnerThreadName()
    {
        return ownerThreadName;
    }

    public long getOwnerSinceMillis()
    {
        return ownerSinceMillis;
    }

    public StackTraceElement[] getOwnerStackTrace()
    {
        return ownerStackTrace;
    }

    public static void setTraceAccessEnabled(boolean aEnabled)
    {
        traceAccessEnabled = aEnabled;
    }

    @Override
    public String toString()
    {
        var tb = new ToStringBuilder(this, SHORT_PREFIX_STYLE).append("key", key)
                .append("cas", getCasHashCode()) //
                .append("deleted", deleted) //
                .append("typeSystemOutdated", typeSystemOutdated);

        if (ownerThreadId > 0) {
            tb.append("owner", ownerThreadName + "(" + ownerThreadId + ")").append("ownerSince",
                    ownerSinceMillis);
        }

        return tb.toString();
    }

}
