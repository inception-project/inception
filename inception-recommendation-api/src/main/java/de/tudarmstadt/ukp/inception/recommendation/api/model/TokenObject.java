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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import java.io.Serializable;

public class TokenObject implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = 2411892060630719421L;
    protected Offset offset;
    protected String coveredText;
    protected String documentURI;
    protected String documentName;
    protected int id;
    // TODO #176 add document Id once it it available in the CAS
    
    protected TokenObject()
    {
        super();
    }
    
    public TokenObject(Offset offset, String coveredText, String documentURI, String documentName, 
        int id)
    {
        super();
        this.offset = offset;
        this.coveredText = coveredText;
        this.documentURI = documentURI;
        this.documentName = documentName;
        this.id = id;
    }

    public int getId()
    {
        return id;
    }
    
    public Offset getOffset()
    {
        return offset;
    }
    public void setOffset(Offset offset) {
        this.offset = offset;
    }
    public String getCoveredText()
    {
        return coveredText;
    }
    public void setCoveredText(String coveredText)
    {
        this.coveredText = coveredText;
    }
    public String getDocumentURI()
    {
        return documentURI;
    }
    public void setDocumentURI(String documentURI)
    {
        this.documentURI = documentURI;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((coveredText == null) ? 0 : coveredText.hashCode());
        result = prime * result + ((documentName == null) ? 0 : documentName.hashCode());
        result = prime * result + ((documentURI == null) ? 0 : documentURI.hashCode());
        result = prime * result + id;
        result = prime * result + ((offset == null) ? 0 : offset.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TokenObject other = (TokenObject) obj;
        if (coveredText == null) {
            if (other.coveredText != null)
                return false;
        } else if (!coveredText.equals(other.coveredText))
            return false;
        if (documentName == null) {
            if (other.documentName != null)
                return false;
        } else if (!documentName.equals(other.documentName))
            return false;
        if (documentURI == null) {
            if (other.documentURI != null)
                return false;
        } else if (!documentURI.equals(other.documentURI))
            return false;
        if (id != other.id)
            return false;
        if (offset == null) {
            if (other.offset != null)
                return false;
        } else if (!offset.equals(other.offset))
            return false;
        return true;
    }    
}
