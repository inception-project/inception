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
package de.tudarmstadt.ukp.inception.io.bioc.xml;

import org.apache.uima.cas.CAS;
import org.dkpro.core.api.xml.type.XmlElement;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.MetaDataStringField;
import de.tudarmstadt.ukp.inception.io.bioc.BioCReader;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.Cas2SaxEvents;

public class Cas2BioCSaxEvents
    extends Cas2SaxEvents
{

    public Cas2BioCSaxEvents(ContentHandler aHandler)
    {
        super(aHandler);
    }

    @Override
    public void processChildren(XmlElement aElement) throws SAXException
    {
        if (BioCReader.E_COLLECTION.equals(aElement.getQName())) {
            processMetaDataField(aElement.getCAS(), "source");
            processMetaDataField(aElement.getCAS(), "date");
            processMetaDataField(aElement.getCAS(), "key");
        }

        super.processChildren(aElement);
    }

    private void processMetaDataField(CAS aCas, String aKey) throws SAXException
    {
        var field = aCas.select(MetaDataStringField.class).filter(f -> aKey.equals(f.getKey()))
                .findFirst().get();

        handler.startElement(field.getKey());
        handler.characters(field.getValue());
    }
}
