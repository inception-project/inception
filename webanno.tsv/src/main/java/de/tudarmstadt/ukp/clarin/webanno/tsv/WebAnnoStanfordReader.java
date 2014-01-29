/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Reads Stanford NE annotated document using the MUC-7 Classifier and convert it to CAS
 *
 * @author Seid Muhie Yimam
 *
 */
public class WebAnnoStanfordReader
    extends JCasResourceCollectionReader_ImplBase
{

    public void convertToCas(JCas aJCas, InputStream aIs, String aEncoding)
        throws IOException

    {
        StringBuilder text = new StringBuilder();
        int sentenceBegin = 0;
        LineIterator lineIterator = IOUtils.lineIterator(aIs, aEncoding);
        int tokenBeginPosition = 0;
        String previousNamedEntity = "O";
        int namedEntityBegin = -1;
        int namedEntityEnd = -1;
        while (lineIterator.hasNext()) {
            StringTokenizer st = new StringTokenizer(lineIterator.next().trim(), " ");

            while (st.hasMoreTokens()) {
                String ne = "O";
                String token = st.nextToken();

                 if (token.startsWith("/O")) {
                    continue;
                 }

                if (token.endsWith("/O")) {
                    token = token.substring(0, token.length() - 2);
                }
                else if (token.endsWith("/PERSON")) {
                    ne = "PERSON";
                    token = token.substring(0, token.length() - 7);
                }
                else if (token.endsWith("/LOCATION")) {
                    ne = "LOCATION";
                    token = token.substring(0, token.length() - 9);
                }
                else if (token.endsWith("/ORGANIZATION")) {
                    ne = "ORGANIZATION";
                    token = token.substring(0, token.length() - 13);
                }
                else if (token.endsWith("/TIME")) {
                    ne = "TIME";
                    token = token.substring(0, token.length() - 5);
                }
                else if (token.endsWith("/DATE")) {
                    ne = "DATE";
                    token = token.substring(0, token.length() - 5);
                }
                else if (token.endsWith("/PERCENT")) {
                    ne = "PERCENT";
                    token = token.substring(0, token.length() - 8);
                }
                else if (token.endsWith("/MONEY")) {
                    ne = "MONEY";
                    token = token.substring(0, token.length() - 6);
                }

                text.append(token + " ");
                Token outToken = new Token(aJCas, tokenBeginPosition, tokenBeginPosition
                        + token.length() );
                outToken.addToIndexes();

                if (previousNamedEntity.equals("O") && ne.equals("O")) {
                    tokenBeginPosition = tokenBeginPosition + token.length() + 1;
                    continue;
                }
                if (previousNamedEntity.equals("O") && !ne.equals("O")) {
                    namedEntityBegin = tokenBeginPosition;
                    tokenBeginPosition = tokenBeginPosition + token.length() + 1;
                    namedEntityEnd = tokenBeginPosition;
                    previousNamedEntity = ne;
                    continue;
                }

                if (previousNamedEntity.equals(ne)) {
                    tokenBeginPosition = tokenBeginPosition + token.length() + 1;
                    namedEntityEnd = tokenBeginPosition;
                    continue;
                }

                NamedEntity outNamedEntity = new NamedEntity(aJCas, namedEntityBegin,
                        namedEntityEnd);
                outNamedEntity.setValue(previousNamedEntity);
                outNamedEntity.addToIndexes();

                namedEntityBegin = tokenBeginPosition;
                tokenBeginPosition = tokenBeginPosition + token.length() + 1;
                namedEntityEnd = tokenBeginPosition;
                previousNamedEntity = ne;
            }
            Sentence outSentence = new Sentence(aJCas);
            outSentence.setBegin(sentenceBegin);
            outSentence.setEnd(tokenBeginPosition);
            outSentence.addToIndexes();
            sentenceBegin = tokenBeginPosition;
        }
        aJCas.setDocumentText(text.toString());
    }

    public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
    @ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String encoding;

    @Override
    public void getNext(JCas aJCas)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);
        InputStream is = null;
        try {
            is = res.getInputStream();
            convertToCas(aJCas, is, encoding);
        }
        finally {
            closeQuietly(is);
        }

    }
}
