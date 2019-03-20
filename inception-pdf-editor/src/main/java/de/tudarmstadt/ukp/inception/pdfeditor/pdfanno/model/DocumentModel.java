/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains information and mappings for document text provided by INCEpTION.
 * This class is used for annotation alignment between INCEpTION and PDFAnno.
 */
public class DocumentModel implements Serializable
{

    private static final long serialVersionUID = 28043850700314856L;

    /**
     * Document text provided by INCEpTION
     */
    private String documentText;

    /**
     * Document text provided by INCEpTION without whitespaces
     */
    private String whitespacelessText;

    /**
     * Mapping from whitespaceless text indices to original text indices
     */
    private Map<Integer, Integer> characterPositionMap;

    public DocumentModel(String aDocumentText)
    {
        setDocumentText(aDocumentText);
    }

    public void setDocumentText(String aDocumentText)
    {
        documentText = aDocumentText;
        characterPositionMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        int documentTextIndex = 0;
        int whitespacelessTextIndex = 0;
        for (char c : documentText.toCharArray())
        {
            if (!Character.isWhitespace(c)) {
                characterPositionMap.put(whitespacelessTextIndex, documentTextIndex);
                sb.append(c);
                whitespacelessTextIndex++;
            }
            documentTextIndex++;
        }
        whitespacelessText = sb.toString();
    }

    public int getDocumentIndex(int aWhitespacelessIndex)
    {
        return characterPositionMap.get(aWhitespacelessIndex);
    }

    public String getDocumentText()
    {
        return documentText;
    }

    public String getWhitespacelessText()
    {
        return whitespacelessText;
    }
}
