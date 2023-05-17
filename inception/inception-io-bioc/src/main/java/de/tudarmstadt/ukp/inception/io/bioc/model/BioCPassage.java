/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.bioc.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "infons", "offset", "text", "sentences", "annotations", "relations" })
public class BioCPassage
    extends BioCAnnotationContainer
{
    private int offset;
    private String text;
    private List<BioCSentence> sentences;

    public int getOffset()
    {
        return offset;
    }

    @XmlElement(name = "offset")
    public void setOffset(int aOffset)
    {
        offset = aOffset;
    }

    public String getText()
    {
        return text;
    }

    @XmlElement(name = "text")
    public void setText(String aText)
    {
        text = aText;
    }

    public List<BioCSentence> getSentences()
    {
        return sentences;
    }

    @XmlElement(name = "sentence")
    public void setSentences(List<BioCSentence> aSentences)
    {
        sentences = aSentences;
    }
}
