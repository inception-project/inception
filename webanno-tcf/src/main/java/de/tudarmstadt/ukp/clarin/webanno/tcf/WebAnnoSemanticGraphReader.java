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
package de.tudarmstadt.ukp.clarin.webanno.tcf;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Reads textual entailment annotated document using separated with &gt; for entails and X for not
 * and convert it to CAS
 *
 * @author Seid Muhie Yimam
 *
 */
public class WebAnnoSemanticGraphReader
    extends JCasResourceCollectionReader_ImplBase
{

    public void convertToCas(JCas aJCas, InputStream aIs, String aEncoding)
        throws IOException

    {
        StringBuilder text = new StringBuilder();
        LineIterator lineIterator = IOUtils.lineIterator(aIs, aEncoding);
        int tokenBeginPosition = 0;
        while (lineIterator.hasNext()) {
            String line = lineIterator.next();
            String[] contents = line.split("\t>\t|\tX\t");
            int sentenceBegin = tokenBeginPosition;
            int chainBegin = tokenBeginPosition;
            int chainEnd = 0;
            StringTokenizer st = new StringTokenizer(contents[0]);
            while(st.hasMoreTokens()){
                String content = st.nextToken();
                Token outToken = new Token(aJCas, tokenBeginPosition, tokenBeginPosition
                        + content.length());
                outToken.addToIndexes();
                tokenBeginPosition = outToken.getEnd() +1;
                chainEnd = tokenBeginPosition;
                text.append(content + " ");
            }

            CoreferenceChain chain = new CoreferenceChain(aJCas);
            CoreferenceLink link = new CoreferenceLink(aJCas, chainBegin, chainEnd-1);
            link.setReferenceType("text");
            link.addToIndexes();
            chain.setFirst(link);

            if(line.contains("\t>\t")) {
                link.setReferenceRelation("entails");
                Token outToken = new Token(aJCas, tokenBeginPosition, tokenBeginPosition
                        + 1);
                outToken.addToIndexes();
                tokenBeginPosition = outToken.getEnd()+1;
                text.append("> ");
            }
            else {
                link.setReferenceRelation("do not entails");
                Token outToken = new Token(aJCas, tokenBeginPosition, tokenBeginPosition
                        + 1);
                outToken.addToIndexes();
                tokenBeginPosition = outToken.getEnd() +1 ;
                text.append("X ");
            }

            chainBegin = tokenBeginPosition;
            st = new StringTokenizer(contents[0]);
            while(st.hasMoreTokens()){
                String content = st.nextToken();
                Token outToken = new Token(aJCas, tokenBeginPosition, tokenBeginPosition
                        + content.length());
                outToken.addToIndexes();
                tokenBeginPosition = outToken.getEnd() +1;
                chainEnd = tokenBeginPosition;
                text.append(content + " ");

            }
            CoreferenceLink nextLink = new CoreferenceLink(aJCas, chainBegin, chainEnd-1);
            nextLink.setReferenceType("hypothesis");
            nextLink.addToIndexes();
            link.setNext(nextLink);
            chain.addToIndexes();
            text.append("\n");

            Sentence outSentence = new Sentence(aJCas);
            outSentence.setBegin(sentenceBegin);
            outSentence.setEnd(tokenBeginPosition);
            outSentence.addToIndexes();
            tokenBeginPosition  = tokenBeginPosition +1;
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
