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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import static de.tudarmstadt.ukp.inception.rendering.model.Range.rangeCoveringDocument;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.inception.rendering.model.Range;

class SlidingWindow<T extends Annotation>
    implements Iterable<List<T>>
{
    private final CAS cas;
    private final Class<T> windowType;
    private final int windowSize;
    private final int windowOverlap;

    public SlidingWindow(CAS aCas, Class<T> aWindowType, int aWindowSize, int aWindowOverlap)
    {
        this(aCas, aWindowType, aWindowSize, aWindowOverlap, rangeCoveringDocument(aCas));
    }

    public SlidingWindow(CAS aCas, Class<T> aWindowType, int aWindowSize, int aWindowOverlap,
            Range aRange)
    {
        cas = aCas;
        windowType = aWindowType;
        windowSize = aWindowSize;
        windowOverlap = aWindowOverlap;
    }

    @Override
    public Iterator<List<T>> iterator()
    {
        return new SlidingWindowIterator(cas.select(windowType).iterator());
    }

    private class SlidingWindowIterator
        implements Iterator<List<T>>
    {
        private final Iterator<T> tokenIterator;

        private List<T> nextWindow;

        public SlidingWindowIterator(Iterator<T> aTokenIterator)
        {
            tokenIterator = aTokenIterator;
            nextWindow = makeSample(tokenIterator, new LinkedList<T>(), windowSize, windowOverlap);
        }

        @Override
        public boolean hasNext()
        {
            return !nextWindow.isEmpty();
        }

        @Override
        public List<T> next()
        {
            var currentWindow = nextWindow;
            nextWindow = makeSample(tokenIterator, nextWindow, windowSize, windowOverlap);
            return currentWindow;
        }

        private List<T> makeSample(Iterator<T> aFreshTokenIterator, List<T> aTokens, int aMaxLength,
                int aOverlap)
        {
            if (!aFreshTokenIterator.hasNext()) {
                return Collections.emptyList();
            }

            var result = new LinkedList<T>();

            // Add tokens overlapping with previous sample
            var size = 0;
            if (aOverlap > 0) {
                var overlapIterator = result.descendingIterator();
                while (overlapIterator.hasNext()) {
                    var token = overlapIterator.next();
                    var tokenText = token.getCoveredText();

                    if (isBlank(tokenText)) {
                        continue;
                    }

                    size += tokenText.length();
                    if (size >= aOverlap && !result.isEmpty()) {
                        // Overlap size reached
                        break;
                    }
                    result.add(0, token);
                }
            }

            // Add fresh tokens
            var freshTokenAdded = false;
            while (aFreshTokenIterator.hasNext()) {
                var token = aFreshTokenIterator.next();
                var tokenText = token.getCoveredText();

                if (isBlank(tokenText)) {
                    continue;
                }

                size += tokenText.length();
                if (size >= aMaxLength && freshTokenAdded) {
                    // Maximum sample size reached
                    break;
                }

                result.add(token);
                freshTokenAdded = true;
            }

            if (!freshTokenAdded) {
                return Collections.emptyList();
            }

            return result;
        }
    }
}
