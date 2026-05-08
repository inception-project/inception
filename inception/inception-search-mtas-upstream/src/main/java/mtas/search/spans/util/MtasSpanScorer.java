package mtas.search.spans.util;

import java.io.IOException;

import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.queries.spans.SpanScorer;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.similarities.Similarity.SimScorer;

public class MtasSpanScorer
    extends SpanScorer
{

    public MtasSpanScorer(Spans spans, SimScorer scorer, NumericDocValues norms)
    {
        super(spans, scorer, norms);
    }

    protected float scoreCurrentDoc() throws IOException
    {
        return (float) 1.0;
    }

}
