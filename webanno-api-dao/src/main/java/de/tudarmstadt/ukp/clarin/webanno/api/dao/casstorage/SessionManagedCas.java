package de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.uima.cas.CAS;

public class SessionManagedCas
{
    final long sourceDocumentId;
    final String userId;
    final CasAccessMode mode;
    final CAS cas;
    
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

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof SessionManagedCas)) {
            return false;
        }
        SessionManagedCas castOther = (SessionManagedCas) other;
        return Objects.equals(sourceDocumentId, castOther.sourceDocumentId)
                && Objects.equals(userId, castOther.userId) && Objects.equals(mode, castOther.mode);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sourceDocumentId, userId, mode);
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.NO_FIELD_NAMES_STYLE)
                .append("sourceDocumentId", sourceDocumentId)
                .append("userId", userId)
                .append("mode", mode)
                .toString();
    }
}
