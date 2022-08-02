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
package de.tudarmstadt.ukp.inception.pdfeditor;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parse substitutionTable.xml and create a map
 * 
 * @deprecated Superseded by the new PDF editor
 */
@Deprecated
public class SubstitutionTableParser
    extends DefaultHandler
{

    private Map<String, String> substitutionTable = new HashMap<>();

    private boolean inTable = false;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    {
        if (qName.equals("substitutionTable")) {
            inTable = true;
        }
        else if (inTable && qName.equals("substitution")) {
            String orig = attributes.getValue("orig");
            String subst = attributes.getValue("subst");
            substitutionTable.put(orig, subst);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
        if (qName.equalsIgnoreCase("substitutionTable")) {
            inTable = false;
        }
    }

    public Map<String, String> getSubstitutionTable()
    {
        return substitutionTable;
    }
}
