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
package de.tudarmstadt.ukp.inception.annotation.layer.behavior;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Set;

import org.apache.uima.fit.factory.JCasFactory;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.junit.jupiter.api.Test;

public class ProtectedElementBehaviorTest
{
    @Test
    public void adjust_noXmlElements_returnsOriginal() throws Exception
    {
        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText("This is a test");

        int[] original = new int[] { 2, 4 };

        int[] adjusted = ProtectedElementBehavior.adjust(jcas.getCas(), Set.of("p"), original);

        assertArrayEquals(original, adjusted);
    }

    @Test
    public void adjust_noProtectedElements_returnsOriginal() throws Exception
    {
        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText("This is a test");

        var doc = new XmlDocument(jcas, 0, jcas.getDocumentText().length());
        doc.addToIndexes();

        var el = new XmlElement(jcas, 5, 9);
        el.setQName("e1");
        el.addToIndexes();
        doc.setRoot(el);

        int[] original = new int[] { 6, 7 };

        int[] adjusted = ProtectedElementBehavior.adjust(jcas.getCas(), Set.of("p"), original);

        assertArrayEquals(original, adjusted);
    }

    @Test
    public void adjust_protectedElement_expandsRange() throws Exception
    {
        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText("0123456789");

        var doc = new XmlDocument(jcas, 0, jcas.getDocumentText().length());
        doc.addToIndexes();

        var el = new XmlElement(jcas, 5, 9);
        el.setQName("p");
        el.addToIndexes();
        doc.setRoot(el);

        int[] original = new int[] { 6, 7 };

        int[] adjusted = ProtectedElementBehavior.adjust(jcas.getCas(), Set.of("p"), original);

        assertArrayEquals(new int[] { 5, 9 }, adjusted);
    }

    @Test
    public void adjust_nestedProtectedElements_chooseLongest() throws Exception
    {
        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText("012345678901");

        var doc = new XmlDocument(jcas, 0, jcas.getDocumentText().length());
        doc.addToIndexes();

        var inner = new XmlElement(jcas, 6, 7);
        inner.setQName("p");
        inner.addToIndexes();

        var outer = new XmlElement(jcas, 5, 9);
        outer.setQName("p");
        outer.addToIndexes();

        doc.setRoot(outer);

        int[] original = new int[] { 6, 6 };

        int[] adjusted = ProtectedElementBehavior.adjust(jcas.getCas(), Set.of("p"), original);

        // The larger (outer) element should be chosen
        assertArrayEquals(new int[] { 5, 9 }, adjusted);
    }

}
