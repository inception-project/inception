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
package de.tudarmstadt.ukp.inception.externalsearch;

import static java.util.Collections.emptyList;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class ExternalSearchResult
    implements Serializable
{
    private static final long serialVersionUID = 2698492628701213714L;

    private final DocumentRepository repository;
    private final String collectionId;
    private final String documentId;
    private String documentTitle;
    private String source;
    private String uri;
    private String timestamp;
    private String language;
    private Double score;
    private List<ExternalSearchHighlight> highlights;

    public ExternalSearchResult(DocumentRepository aRepository, String aCollectionId,
            String aDocumentId)
    {
        repository = aRepository;
        collectionId = aCollectionId;
        documentId = aDocumentId;
        highlights = emptyList();
    }
    
    public DocumentRepository getRepository()
    {
        return repository;
    }

    /**
     * Get the ID of the collecting containing the matching document. This is typically a system ID.
     */
    public String getCollectionId()
    {
        return collectionId;
    }

    /**
     * Get the ID of the matching document. This is typically a system ID.
     */
    public String getDocumentId()
    {
        return documentId;
    }

    /**
     * Set the title of the matching document. This is typically a human-readable title.
     */
    public String getDocumentTitle()
    {
        return documentTitle;
    }

    /**
     * Get the title of the matching document.
     */
    public void setDocumentTitle(String aDocumentTitle)
    {
        documentTitle = aDocumentTitle;
    }

    /**
     * Set the source of the matching document. This identifies where the document originally came
     * from before it was indexed. Mind that a collection may contain documents from different
     * sources.
     */
    public String getOriginalSource()
    {
        return source;
    }

    /**
     * Get the source of the matching document.
     */
    public void setOriginalSource(String aSource)
    {
        source = aSource;
    }

    /**
     * Set the source URI of the matching document. This identifies where the document originally
     * came from before it was indexed. Mind that a collection may contain documents from different
     * sources.
     */
    public String getOriginalUri()
    {
        return uri;
    }

    /**
     * Get the source URI of the matching document.
     */
    public void setOriginalUri(String aUri)
    {
        this.uri = aUri;
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(String aTimestamp)
    {
        timestamp = aTimestamp;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String aLanguage)
    {
        language = aLanguage;
    }

    public Double getScore()
    {
        return score;
    }

    public void setScore(Double aScore)
    {
        score = aScore;
    }

    public List<ExternalSearchHighlight> getHighlights()
    {
        return highlights;
    }

    public void setHighlights(List<ExternalSearchHighlight> aHighlights)
    {
        if (aHighlights == null) {
            highlights = emptyList();
        }
        else {
            highlights = aHighlights;
        }
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
                .append("collectionId", collectionId)
                .append("documentId", documentId)
                .append("originalSource", source)
                .append("originalUri", uri)
                .append("documentTitle", documentTitle)
                .toString();
    }
}
