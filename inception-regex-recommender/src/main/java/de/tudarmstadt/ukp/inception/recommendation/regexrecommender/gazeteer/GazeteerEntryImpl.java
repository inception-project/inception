package de.tudarmstadt.ukp.inception.recommendation.regexrecommender.gazeteer;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.GazeteerEntry;

public class GazeteerEntryImpl
    implements GazeteerEntry
{
    public final String regex;
    public final String label;
    public final int acceptedCount;
    public final int rejectedCount;
    
    public GazeteerEntryImpl(String aRegex, String aLabel, int aAcceptedCount, int aRejectedCount)
    {
        super();
        this.regex = aRegex;
        this.label = aLabel;
        this.acceptedCount = aAcceptedCount;
        this.rejectedCount = aRejectedCount;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof GazeteerEntryImpl)) {
            return false;
        }
        GazeteerEntryImpl castOther = (GazeteerEntryImpl) other;
        return new EqualsBuilder().append(regex, castOther.regex)
                .append(label, castOther.label)
                .append(acceptedCount, castOther.acceptedCount)
                .append(rejectedCount, castOther.rejectedCount)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(regex)
                                    .append(acceptedCount)
                                    .append(rejectedCount)
                                    .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("regex", regex)
                                        .append("label", label)
                                        .append("acceptedCount", acceptedCount)
                                        .append("rejectedCount", rejectedCount)
                                        .toString();
    }
}

