package mtas.search.spans.util;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.lucene.queries.spans.SpanNearQuery;
import org.apache.lucene.queries.spans.SpanQuery;

/**
 * The Class MtasExtendedSpanAndQuery.
 */
public class MtasExtendedSpanAndQuery
    extends SpanNearQuery
{

    /** The local clauses. */
    private HashSet<SpanQuery> localClauses;

    /**
     * Instantiates a new mtas extended span and query.
     *
     * @param clauses
     *            the clauses
     */
    public MtasExtendedSpanAndQuery(SpanQuery... clauses)
    {
        super(clauses, -1 * (clauses.length - 1), false);
        this.localClauses = new HashSet<>();
        for (SpanQuery clause : clauses) {
            this.localClauses.add(clause);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanNearQuery#toString(java.lang.String)
     */
    @Override
    public String toString(String field)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.getClass().getSimpleName() + "([");
        Iterator<SpanQuery> i = localClauses.iterator();
        while (i.hasNext()) {
            SpanQuery clause = i.next();
            buffer.append(clause.toString(field));
            if (i.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append("])");
        return buffer.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanNearQuery#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MtasExtendedSpanAndQuery that = (MtasExtendedSpanAndQuery) obj;
        return localClauses.equals(that.localClauses);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanNearQuery#hashCode()
     */
    @Override
    public int hashCode()
    {
        int h = classHash();
        h = (h * 7) ^ super.hashCode();
        return h;
    }

}
