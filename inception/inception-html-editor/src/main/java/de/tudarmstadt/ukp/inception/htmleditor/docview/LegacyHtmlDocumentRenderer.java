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
package de.tudarmstadt.ukp.inception.htmleditor.docview;

import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.util.string.Strings;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Div;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlXmlDocumentIFrameViewFactory;

/**
 * @deprecated Use {@link XHtmlXmlDocumentIFrameViewFactory} instead
 */
@Deprecated
public class LegacyHtmlDocumentRenderer
{
    public String renderHtmlDocumentStructure(CAS aCas)
        throws IOException, TransformerConfigurationException, CASException, SAXException
    {
        StringBuilder buf = new StringBuilder(
                Strings.escapeMarkup(aCas.getDocumentText().replace('\r', ' ')));

        List<Node> nodes = new ArrayList<>();
        for (AnnotationFS div : select(aCas, getType(aCas, Div.class))) {
            if (div.getType().getName().equals(Paragraph.class.getName())) {
                Node startNode = new Node();
                startNode.position = div.getBegin();
                startNode.type = "<p>";
                nodes.add(startNode);

                Node endNode = new Node();
                endNode.position = div.getEnd();
                endNode.type = "</p>";
                nodes.add(endNode);
            }
            if (div.getType().getName().equals(Heading.class.getName())) {
                Node startNode = new Node();
                startNode.position = div.getBegin();
                startNode.type = "<h1>";
                nodes.add(startNode);

                Node endNode = new Node();
                endNode.position = div.getEnd();
                endNode.type = "</h1>";
                nodes.add(endNode);
            }
        }

        // Sort backwards
        nodes.sort((a, b) -> {
            return b.position - a.position;
        });

        for (Node n : nodes) {
            buf.insert(n.position, n.type);
        }

        return buf.toString();
    }

    private static class Node
    {
        int position;
        String type;
    }
}
