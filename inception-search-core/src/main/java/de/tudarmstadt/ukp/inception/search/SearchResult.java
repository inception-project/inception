/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.search;

import java.io.Serializable;

public class SearchResult
    implements Serializable
{
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
        result = prime * result + ((documentTitle == null) ? 0 : documentTitle.hashCode());
        result = prime * result + ((leftContext == null) ? 0 : leftContext.hashCode());
        result = prime * result + offsetEnd;
        result = prime * result + offsetStart;
        result = prime * result + ((rightContext == null) ? 0 : rightContext.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + tokenLength;
        result = prime * result + tokenStart;
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
        SearchResult other = (SearchResult) obj;
        if (documentTitle == null) {
            if (other.documentTitle != null)
                return false;
        }
        else if (!documentTitle.equals(other.documentTitle))
            return false;
        if (leftContext == null) {
            if (other.leftContext != null)
                return false;
        }
        else if (!leftContext.equals(other.leftContext))
            return false;
        if (offsetEnd != other.offsetEnd)
            return false;
        if (offsetStart != other.offsetStart)
            return false;
        if (rightContext == null) {
            if (other.rightContext != null)
                return false;
        }
        else if (!rightContext.equals(other.rightContext))
            return false;
        if (text == null) {
            if (other.text != null)
                return false;
        }
        else if (!text.equals(other.text))
            return false;
        if (tokenLength != other.tokenLength)
            return false;
        if (tokenStart != other.tokenStart)
            return false;
        return true;
    }
}
