package mtas.search.spans;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.spans.SpanNearQuery;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;

import mtas.search.spans.util.MtasExtendedSpanAndQuery;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * Search for hits from multiple MtasSpanQueries occurring at the same position
 */
public class MtasSpanAndQuery
    extends MtasSpanQuery
{

    /** The base query. */
    private SpanNearQuery baseQuery;

    /** The clauses. */
    private HashSet<MtasSpanQuery> clauses;

    /**
     * Instantiates a new mtas span and query.
     *
     * @param initialClauses
     *            the initial clauses
     */
    public MtasSpanAndQuery(MtasSpanQuery... initialClauses)
    {
        super(null, null);
        // define estimates for minimum and maximum to enable
        // general preliminary optimization of queries
        Integer minimum = null;
        Integer maximum = null;
        clauses = new HashSet<>();
        for (MtasSpanQuery item : initialClauses) {
            if (!clauses.contains(item)) {
                clauses.add(item);
                if (item.getMinimumWidth() != null) {
                    if (minimum != null) {
                        minimum = Math.max(minimum, item.getMinimumWidth());
                    }
                    else {
                        minimum = item.getMinimumWidth();
                    }
                }
                if (item.getMaximumWidth() != null) {
                    if (maximum != null) {
                        maximum = Math.max(maximum, item.getMaximumWidth());
                    }
                    else {
                        maximum = item.getMaximumWidth();
                    }
                }
            }
        }
        setWidth(minimum, maximum);
        // define the base query to be used for the weight
        baseQuery = new MtasExtendedSpanAndQuery(
                clauses.toArray(new MtasSpanQuery[clauses.size()]));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanQuery#getField()
     */
    @Override
    public String getField()
    {
        return baseQuery.getField();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanQuery#createWeight(org.apache.lucene.
     * search.IndexSearcher, boolean)
     */
    @Override
    public SpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost)
        throws IOException
    {
        return baseQuery.createWeight(searcher, scoreMode, boost);
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.search.spans.util.MtasSpanQuery#rewrite(org.apache.lucene.index. IndexReader)
     */
    @Override
    public MtasSpanQuery rewrite(IndexReader reader) throws IOException
    {
        if (clauses.size() > 1) {
            // rewrite, count MtasSpanMatchAllQuery and check for
            // MtasSpanMatchNoneQuery
            MtasSpanQuery[] newClauses = new MtasSpanQuery[clauses.size()];
            MtasSpanQuery[] oldClauses = clauses.toArray(new MtasSpanQuery[clauses.size()]);
            int singlePositionQueries = 0;
            int matchAllSinglePositionQueries = 0;
            boolean actuallyRewritten = false;
            for (int i = 0; i < oldClauses.length; i++) {
                newClauses[i] = oldClauses[i].rewrite(reader);
                // did anything change?
                actuallyRewritten |= !oldClauses[i].equals(newClauses[i]);
                // no results if one of the clauses never matches
                if (newClauses[i] instanceof MtasSpanMatchNoneQuery) {
                    return (new MtasSpanMatchNoneQuery(this.getField())).rewrite(reader);
                }
                else {
                    if (newClauses[i].isSinglePositionQuery()) {
                        singlePositionQueries++;
                        if (newClauses[i] instanceof MtasSpanMatchAllQuery) {
                            matchAllSinglePositionQueries++;
                        }
                    }
                }
            }
            // filter clauses
            if (matchAllSinglePositionQueries > 0) {
                // compute new number of clauses
                int newNumber = newClauses.length - matchAllSinglePositionQueries;
                // but there should be always one...
                if (matchAllSinglePositionQueries == singlePositionQueries) {
                    newNumber++;
                }
                MtasSpanQuery[] newFilteredClauses = new MtasSpanQuery[newNumber];
                int j = 0;
                for (int i = 0; i < newClauses.length; i++) {
                    if (!(newClauses[i].isSinglePositionQuery()
                            && (newClauses[i] instanceof MtasSpanMatchAllQuery))) {
                        newFilteredClauses[j] = newClauses[i];
                        j++;
                        // there should be always one... (matches at most one time)
                    }
                    else if (matchAllSinglePositionQueries == singlePositionQueries) {
                        newFilteredClauses[j] = newClauses[i];
                        j++;
                        singlePositionQueries++; // only match this condition once
                    }
                }
                newClauses = newFilteredClauses;
            }
            if (newClauses.length == 0) {
                // no clauses left, so no results
                return (new MtasSpanMatchNoneQuery(this.getField())).rewrite(reader);
            }
            else if (newClauses.length == 1) {
                // only a single clause
                return newClauses[0].rewrite(reader);
            }
            else if (actuallyRewritten || newClauses.length != clauses.size()) {
                // rewrite again, just to be sure
                return new MtasSpanAndQuery(newClauses).rewrite(reader);
            }
            else {
                // do what you parent does
                return super.rewrite(reader);
            }
        }
        else if (clauses.size() == 1) {
            // only one, so just return this single clause
            return clauses.iterator().next().rewrite(reader);
        }
        else {
            // no clauses, therefore no matches
            return (new MtasSpanMatchNoneQuery(this.getField())).rewrite(reader);
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
        return baseQuery.toString(field);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanNearQuery#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MtasSpanAndQuery that = (MtasSpanAndQuery) obj;
        return baseQuery.equals(that.baseQuery);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanNearQuery#hashCode()
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(this.getClass().getSimpleName(), clauses);
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.search.spans.util.MtasSpanQuery#disableTwoPhaseIterator()
     */
    @Override
    public void disableTwoPhaseIterator()
    {
        super.disableTwoPhaseIterator();
        for (MtasSpanQuery item : clauses) {
            item.disableTwoPhaseIterator();
        }
    }

    @Override
    public boolean isMatchAllPositionsQuery()
    {
        return false;
    }

    @Override
    public void visit(QueryVisitor aVisitor)
    {
        baseQuery.visit(aVisitor);
    }
}
