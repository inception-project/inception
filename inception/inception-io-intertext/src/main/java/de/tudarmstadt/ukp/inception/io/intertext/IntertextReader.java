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
package de.tudarmstadt.ukp.inception.io.intertext;

import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.getAttributeValue;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.hasAttributeWithValue;

import java.io.IOException;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.xml.type.XmlElement;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlDocumentReader;

public class IntertextReader
    extends XmlDocumentReader
{
    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        super.getNext(aJCas);

        aJCas.select(XmlElement.class) //
                .filter(e -> "div".equals(e.getQName()))
                .filter(e -> hasAttributeWithValue(e, "class", "s"))
                .forEach(e -> createSentence(e));
    }

    private void createSentence(XmlElement aElement)
    {
        var sentence = new Sentence(aElement.getJCas(), aElement.getBegin(), aElement.getEnd());
        sentence.trim();

        getAttributeValue(aElement, "data-it-id").ifPresent(sentence::setId);

        sentence.addToIndexes();
    }
}
