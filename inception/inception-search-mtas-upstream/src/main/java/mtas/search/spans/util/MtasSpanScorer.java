package mtas.search.spans.util;

import java.io.IOException;

import org.apache.lucene.queries.spans.SpanScorer;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.LeafSimScorer;

public class MtasSpanScorer
    extends SpanScorer
{

    public MtasSpanScorer(SpanWeight weight, Spans spans, LeafSimScorer docScorer)
    {
        super(weight, spans, docScorer);
    }

    protected float scoreCurrentDoc() throws IOException
    {
        return (float) 1.0;
    }

}
