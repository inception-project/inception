package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

public class TokenJSONObject
    extends JSONOutput
{
    private long tokenId;
    private String coveredText;
    private int begin;
    private int end;

    public long getTokenId()
    {
        return tokenId;
    }

    public void setTokenId(long tokenId)
    {
        this.tokenId = tokenId;
    }

    public String getCoveredText()
    {
        return coveredText;
    }

    public void setCoveredText(String coveredText)
    {
        this.coveredText = coveredText;
    }

    public int getBegin()
    {
        return begin;
    }

    public void setBegin(int begin)
    {
        this.begin = begin;
    }

    public int getEnd()
    {
        return end;
    }

    public void setEnd(int end)
    {
        this.end = end;
    }

}
