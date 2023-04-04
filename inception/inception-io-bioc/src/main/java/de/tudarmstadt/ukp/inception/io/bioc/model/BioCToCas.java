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

import static org.apache.commons.lang3.StringUtils.repeat;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

public class BioCToCas
{
    public void parseXml(InputStream aReader, JCas aJCas) throws JAXBException, XMLStreamException
    {
        var xmlInputFactory = XmlParserUtils.newXmlInputFactory();
        var xmlEventReader = xmlInputFactory.createXMLEventReader(aReader);

        var context = JAXBContext.newInstance(BioCCollection.class);
        var unmarshaller = context.createUnmarshaller();

        var collection = unmarshaller.unmarshal(xmlEventReader, BioCCollection.class).getValue();

        var builder = new JCasBuilder(aJCas);
        readDocument(builder, collection.getDocuments().get(0));
        builder.close();
    }

    public void readDocument(JCasBuilder aBuilder, BioCDocument aDocument)
    {
        if (aDocument.getPassages() != null) {
            for (var passage : aDocument.getPassages()) {
                readPassage(aBuilder, passage);
            }
        }
    }

    public void readPassage(JCasBuilder aBuilder, BioCPassage aPassage)
    {
        if (aPassage.getText() != null) {
            if (aPassage.getOffset() - aBuilder.getPosition() >= 2) {
                aBuilder.add("\n\n");
            }

            if (aBuilder.getPosition() < aPassage.getOffset()) {
                aBuilder.add(repeat(" ", aPassage.getOffset() - aBuilder.getPosition()));
            }

            aBuilder.add(aPassage.getText());
        }
        else if (aPassage.getSentences() != null) {
            for (var sentence : aPassage.getSentences()) {
                readSentence(aBuilder, sentence);
            }
        }
    }

    public void readSentence(JCasBuilder aBuilder, BioCSentence aSentence)
    {
        if (aSentence.getText() != null) {
            if (aSentence.getOffset() - aBuilder.getPosition() >= 1) {
                aBuilder.add("\n\n");
            }

            if (aBuilder.getPosition() < aSentence.getOffset()) {
                aBuilder.add(repeat(" ", aSentence.getOffset() - aBuilder.getPosition()));
            }

            aBuilder.add(aSentence.getText(), Sentence.class).trim();
        }
    }
}
