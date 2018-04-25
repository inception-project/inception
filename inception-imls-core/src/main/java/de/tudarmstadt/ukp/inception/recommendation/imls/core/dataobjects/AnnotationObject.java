/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class AnnotationObject
    extends TokenObject implements Serializable
{
    private static final long serialVersionUID = -1145787227041121430L;
    
    private List<TokenObject> sentenceTokens;
    private String annotation;
    private String description;
    private final String feature;
    private final String classifier;
    private final double confidence;
    private long recommenderId;
    
    public <T extends TokenObject> AnnotationObject(String annotation, String description,
        String documentURI, String documentName, String coveredText, Offset offset,
        List<T> sentenceTokens, int id, String aFeature, String classifier, double confidence)
    {
        this.annotation = annotation;
        this.description = description;
        this.documentURI = documentURI;
        this.documentName = documentName;
        this.coveredText = coveredText;
        this.offset = offset;
        this.id = id;
        this.feature = aFeature;
        this.classifier = classifier;
        this.confidence = confidence;
        
        // we have to strip the tokenObject out of the given object to ensure equality
        this.sentenceTokens = new LinkedList<>();
        if (sentenceTokens == null) {
            return;
        }
        for (int i = 0; i < sentenceTokens.size(); i++) {
            T t = sentenceTokens.get(i);
            this.sentenceTokens.add(new TokenObject(t.getOffset(), t.getCoveredText(), 
                t.getDocumentURI(),  t.getDocumentName(), i));
        }
    }

    public <T extends TokenObject> AnnotationObject(String annotation, String documentURI,
        String documentName, String coveredText, Offset offset, List<T> sentenceTokens,
        int id, String aFeature, String classifier, double confidence)
    {
        this(annotation, null, documentURI, documentName, coveredText, offset, sentenceTokens, id,
            aFeature, classifier, confidence);
    }

    public <T extends TokenObject> AnnotationObject(String annotation, T token,
            List<T> sentenceTokens, int id, String aFeature, String classifier, double confidence)
    {
        this(annotation, token.getDocumentURI(), token.getDocumentName(), token.getCoveredText(), 
        token.getOffset(), sentenceTokens, id, aFeature, classifier, confidence);
    }

    public <T extends TokenObject> AnnotationObject(String annotation, T token,
            List<T> sentenceTokens, int id, String aFeature, String classifier)
    {
        this(annotation, token.getDocumentURI(), token.getDocumentName(), token.getCoveredText(), 
        token.getOffset(), sentenceTokens, id, aFeature, classifier, -1);
    }

    public <T extends TokenObject> AnnotationObject(String annotation, String description, T token,
        List<T> sentenceTokens, int id, String aFeature, String classifier)
    {
        this(annotation, description, token.getDocumentURI(), token.getDocumentName(),
            token.getCoveredText(), token.getOffset(), sentenceTokens, id, aFeature, classifier,
            -1);
    }
    
    public AnnotationObject(AnnotationObject ao, int id, String aFeature, String classifier)
    {
        this(ao.getAnnotation(), ao.getDocumentURI(), ao.getDocumentName(), ao.getCoveredText(), 
            ao.getOffset(), ao.getSentenceTokens(), id, aFeature, classifier, -1);
    }
    
    public <T extends TokenObject> AnnotationObject(String annotation, T token,
            List<T> sentenceTokens, int id, String aFeature)
    {
        this(annotation, token.getDocumentURI(), token.getDocumentName(), token.getCoveredText(),
            token.getOffset(), sentenceTokens, id, aFeature, null, -1);
    }

    public AnnotationObject(AnnotationObject ao, int id, String aFeature)
    {
        this(ao.getAnnotation(), ao.getDocumentURI(), ao.getDocumentName(), ao.getCoveredText(), 
            ao.getOffset(), ao.getSentenceTokens(), id, aFeature, null, -1);
    }

    public List<TokenObject> getSentenceTokens()
    {
        return sentenceTokens;
    }

    public String[] getSentenceTokensAsStringArray()
    {
        String[] sentenceStringTokens = new String[sentenceTokens.size()];
        for (int i = 0; i < sentenceStringTokens.length; i++) {
            sentenceStringTokens[i] = sentenceTokens.get(i).getCoveredText();
        }
        return sentenceStringTokens;
    }

    public String getAnnotation()
    {
        return annotation;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        this.description = aDescription;
    }

    public double getConfidence()
    {
        return confidence;
    }
    
    @Override
    public String getDocumentURI()
    {
        return documentURI;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getFeature()
    {
        return feature;
    }

    public void setSentenceTokens(List<TokenObject> sentenceTokens)
    {
        this.sentenceTokens = sentenceTokens;
    }

    public void setSentenceTokensFromCAS(
            List<de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token> sentenceTokens,
            String documentURI)
    {
        if (this.sentenceTokens == null) {
            this.sentenceTokens = new LinkedList<>();
        }
        
        for (int i = 0; i < sentenceTokens.size(); i++) {
            de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token casToken = 
                    sentenceTokens.get(i);

            Offset offset = new Offset(casToken.getBegin(), casToken.getEnd(), i, i);

            this.sentenceTokens
                    .add(new TokenObject(offset, casToken.getCoveredText(), documentURI, 
                        "N/A", id));
        }
    }

    public void setAnnotation(String tag)
    {
        this.annotation = tag;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public void setRecommenderId(long recommendationId)
    {
        this.recommenderId = recommendationId;
    }  
    
    public long getRecommenderId() {
        return recommenderId;
    }
    
    @Override
    public String toString()
    {
        return "Annotation: " + annotation + " - CoveredText: " + coveredText + " - Offset: "
                + offset.toString() + " - DocumentURI: " + documentURI + " - DocumentName: " 
                + documentName +  " - ID: " + id;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
        result = prime * result + ((sentenceTokens == null) ? 0 : sentenceTokens.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AnnotationObject other = (AnnotationObject) obj;
        if (annotation == null) {
            if (other.annotation != null) {
                return false;
            }
        }
        else if (!annotation.equals(other.annotation)) {
            return false;
        }
        if (sentenceTokens == null) {
            if (other.sentenceTokens != null) {
                return false;
            }
        }
        else if (!sentenceTokens.equals(other.sentenceTokens)) {
            return false;
        }
        return true;
    }

}
