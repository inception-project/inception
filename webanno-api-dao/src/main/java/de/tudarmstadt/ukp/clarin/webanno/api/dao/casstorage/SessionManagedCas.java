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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode;

public class SessionManagedCas
{
    private final long sourceDocumentId;
    private final String userId;
    private final CasAccessMode mode;
    private final CAS cas;
    
    private int readCount;
    private int writeCount;
    
    public SessionManagedCas(long aSourceDocumentId, String aUserId, CasAccessMode aMode, CAS aCas)
    {
        super();
        sourceDocumentId = aSourceDocumentId;
        userId = aUserId;
        mode = aMode;
        cas = aCas;
    }
    
    public long getSourceDocumentId()
    {
        return sourceDocumentId;
    }
    
    public String getUserId()
    {
        return userId;
    }
    
    public CasAccessMode getMode()
    {
        return mode;
    }
    
    public CAS getCas()
    {
        return cas;
    }
    
    public void incrementReadCount()
    {
        readCount++;
    }

    public void incrementWriteCount()
    {
        writeCount++;
    }
    
    public int getReadCout()
    {
        return readCount;
    }
    
    public int getWriteCount()
    {
        return writeCount;
    }
    
    /**
     * @return whether making modifications to the managed CAS is permitted.
     */
    public boolean isWritingPermitted()
    {
        return EXCLUSIVE_WRITE_ACCESS.equals(mode);
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof SessionManagedCas)) {
            return false;
        }
        SessionManagedCas castOther = (SessionManagedCas) other;
        return Objects.equals(sourceDocumentId, castOther.sourceDocumentId) && 
                Objects.equals(userId, castOther.userId) && 
                Objects.equals(mode, castOther.mode);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sourceDocumentId, userId, mode);
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE);
        
        if (sourceDocumentId >= 0l) {
            builder.append("doc", sourceDocumentId)
                    .append("user", userId);
        }
        else {
            builder.append("purpose", userId);
        }
        
        return builder
                .append("cas", cas.hashCode())
                .append("m", mode)
                .append("r", readCount)
                .append("w", writeCount)
                .toString();

    }
}
