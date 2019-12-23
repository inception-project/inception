/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.text;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * UIMA collection reader for plain text files, one sentence per line.
 */
@TypeCapability(
        outputs = {
                "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData"})
public class LineOrientedTextReader
    extends JCasResourceCollectionReader_ImplBase
{
    @Override
    public void getNext(JCas aJCas)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);

        try (InputStream is = new BufferedInputStream(res.getInputStream())) {
            aJCas.setDocumentText(IOUtils.toString(is, "UTF-8"));
        }

        String t = aJCas.getDocumentText();
        int start = 0;
        int end = t.indexOf('\n');
        while (end >= 0) {
            createSentence(aJCas, start, end);
            start = end + 1;
            if (start < t.length()) {
                end = t.indexOf('\n', start);
            }
            else {
                end = -1;
            }
        }

        if (start < t.length()) {
            createSentence(aJCas, start, t.length());
        }
    }

    protected Sentence createSentence(final JCas aJCas, final int aBegin,
            final int aEnd)
    {
        int[] span = new int[] { aBegin, aEnd };
        trim(aJCas.getDocumentText(), span);
        if (!isEmpty(span[0], span[1])) {
            Sentence seg = new Sentence(aJCas, span[0], span[1]);
            seg.addToIndexes(aJCas);
            return seg;
        }
        else {
            return null;
        }
    }
    
    /**
     * Remove trailing or leading whitespace from the annotation.
     * 
     * @param aText
     *            the text.
     * @param aSpan
     *            the offsets.
     */
    public void trim(String aText, int[] aSpan)
    {
        int begin = aSpan[0];
        int end = aSpan[1] - 1;

        while ((begin < (aText.length() - 1)) && trimChar(aText.charAt(begin))) {
            begin++;
        }
        while ((end > 0) && trimChar(aText.charAt(end))) {
            end--;
        }
        end++;

        aSpan[0] = begin;
        aSpan[1] = end;
    }

    public boolean isEmpty(int aBegin, int aEnd)
    {
        return aBegin >= aEnd;
    }

    public boolean trimChar(final char aChar)
    {
        switch (aChar) {
        case '\n':     return true; // Line break
        case '\r':     return true; // Carriage return
        case '\t':     return true; // Tab
        case '\u200E': return true; // LEFT-TO-RIGHT MARK
        case '\u200F': return true; // RIGHT-TO-LEFT MARK
        case '\u2028': return true; // LINE SEPARATOR
        case '\u2029': return true; // PARAGRAPH SEPARATOR
        default:
            return  Character.isWhitespace(aChar);
        }
    }
}
