package de.tudarmstadt.ukp.inception.search;

public class DefaultSearchResultImpl implements SearchResult {
    /**
     *
     */
    private static final long serialVersionUID = 2698492628701213714L;

    private int tokenStart = -1;
    private int tokenLength = -1;
    private int offsetStart = -1;
    private int offsetEnd = -1;
    private String text;
    private String leftContext;
    private String rightContext;
    private long documentId;
    private String documentTitle;

    public int getTokenStart()
    {
        return tokenStart;
    }

    public void setTokenStart(int aTokenStart)
    {
        tokenStart = aTokenStart;
    }

    public int getTokenLength()
    {
        return tokenLength;
    }

    public void setTokenLength(int aTokenLength)
    {
        tokenLength = aTokenLength;
    }

    public int getOffsetStart()
    {
        return offsetStart;
    }

    public void setOffsetStart(int offsetStart)
    {
        this.offsetStart = offsetStart;
    }

    public int getOffsetEnd()
    {
        return offsetEnd;
    }

    public void setOffsetEnd(int offsetEnd)
    {
        this.offsetEnd = offsetEnd;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String aText)
    {
        text = aText;
    }

    public String getLeftContext()
    {
        return leftContext;
    }

    public void setLeftContext(String aLeftContext)
    {
        leftContext = aLeftContext;
    }

    public String getRightContext()
    {
        return rightContext;
    }

    public void setRightContext(String aRightContext)
    {
        rightContext = aRightContext;
    }

    public long getDocumentId()
    {
        return documentId;
    }

    public void setDocumentId(long aDocumentId)
    {
        documentId = aDocumentId;
    }

    public String getDocumentTitle()
    {
        return documentTitle;
    }

    public void setDocumentTitle(String aDocumentTitle)
    {
        documentTitle = aDocumentTitle;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (documentId ^ (documentId >>> 32));
        result = prime * result + offsetEnd;
        result = prime * result + offsetStart;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultSearchResultImpl other = (DefaultSearchResultImpl) obj;
        if (documentId != other.getDocumentId())
            return false;
        if (offsetEnd != other.getOffsetEnd())
            return false;
        if (offsetStart != other.getOffsetStart())
            return false;
        return true;
    }
}
