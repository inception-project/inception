package de.tudarmstadt.ukp.inception.experimental.api.messages.response.span;

import java.util.List;

import de.tudarmstadt.ukp.inception.experimental.api.model.Span;

public class AllSpanResponse
{
    private List<Span> spans;

    public AllSpanResponse(List<Span> aSpans) {
        spans = aSpans;
    }

    public List<Span> getSpans()
    {
        return spans;
    }

    public void setSpans(List<Span> aSpans)
    {
        spans = aSpans;
    }
}
