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
package de.tudarmstadt.ukp.clarin.webanno.text;

import static org.dkpro.core.api.resources.CompressionUtils.getInputStream;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * UIMA collection reader for plain text files, whitespace-separated tokens, one sentence per line.
 */
@TypeCapability(outputs = { "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData" })
public class PretokenizedLineOrientedTextReader
    extends JCasResourceCollectionReader_ImplBase
{
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Whether to remove a byte-order mark from the start of the text.
     */
    public static final String PARAM_INCLUDE_BOM = "includeBom";
    @ConfigurationParameter(name = PARAM_INCLUDE_BOM, mandatory = true, defaultValue = "false")
    private boolean includeBom;

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        var res = nextFile();
        initCas(aJCas, res);

        try (var is = BOMInputStream.builder() //
                .setInclude(includeBom) //
                .setInputStream(getInputStream(res.getLocation(), res.getInputStream())) //
                .get()) {
            aJCas.setDocumentText(IOUtils.toString(is, "UTF-8"));
        }

        var t = aJCas.getDocumentText();
        var start = 0;
        var end = t.indexOf('\n');
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

    protected Sentence createSentence(final JCas aJCas, final int aBegin, final int aEnd)
    {
        var span = new int[] { aBegin, aEnd };
        trim(aJCas.getDocumentText(), span);
        if (!isEmpty(span[0], span[1])) {
            Sentence seg = new Sentence(aJCas, span[0], span[1]);
            seg.addToIndexes(aJCas);

            var whitespaceMatcher = WHITESPACE.matcher(seg.getCoveredText());
            int prevBegin = 0;
            while (whitespaceMatcher.find()) {
                int end = whitespaceMatcher.start();
                createToken(aJCas, seg.getBegin() + prevBegin, seg.getBegin() + end);
                prevBegin = whitespaceMatcher.end();
            }

            if (prevBegin < aEnd) {
                createToken(aJCas, seg.getBegin() + prevBegin, seg.getEnd());
            }

            return seg;
        }
        else {
            return null;
        }
    }

    protected Token createToken(final JCas aJCas, final int aBegin, final int aEnd)
    {
        var span = new int[] { aBegin, aEnd };
        trim(aJCas.getDocumentText(), span);
        if (!isEmpty(span[0], span[1])) {
            var seg = new Token(aJCas, span[0], span[1]);
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
        var begin = aSpan[0];
        var end = aSpan[1] - 1;

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
        case '\n':
            return true; // Line break
        case '\r':
            return true; // Carriage return
        case '\t':
            return true; // Tab
        case '\u200E':
            return true; // LEFT-TO-RIGHT MARK
        case '\u200F':
            return true; // RIGHT-TO-LEFT MARK
        case '\u2028':
            return true; // LINE SEPARATOR
        case '\u2029':
            return true; // PARAGRAPH SEPARATOR
        default:
            return Character.isWhitespace(aChar);
        }
    }
}
