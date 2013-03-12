/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.conll;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;
import org.uimafit.descriptor.ConfigurationParameter;

import com.ibm.icu.text.CharsetDetector;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Reads a specific Conll File (9 TAB separated) annotation and change it to CAS object. Example of Input Files: 1
 * Heutzutage heutzutage ADV _ _ 2 ADV _ _ First column: token Number, in a sentence second Column:
 * the token third column: the lemma forth column: the POS fifth/sixth xolumn: Not Yet known seventh
 * column: the target token for a dependency parsing eighth column: the function of the dependency
 * parsing ninth and tenth column: Not Yet Known
 *
 * Sentences are separated by a blank new line
 *
 * @author Seid Muhie Yimam
 *
 */
public class ConllReader
    extends JCasResourceCollectionReader_ImplBase
{

    public void convertToCas(JCas aJCas, String aDocument)
        throws IOException

    {
        StringBuilder text = new StringBuilder();
        int tokenNumber = 0;

        Map<Integer, String> tokens = new HashMap<Integer, String>();
        Map<Integer, String> pos = new HashMap<Integer, String>();
        Map<Integer, String> lemma = new HashMap<Integer, String>();
        Map<Integer, String> namedEntity = new HashMap<Integer, String>();
        Map<Integer, String> dependencyFunction = new HashMap<Integer, String>();
        Map<Integer, Integer> dependencyDependent = new HashMap<Integer, Integer>();

        List<Integer> firstTokenInSentence = new ArrayList<Integer>();

        StringTokenizer sentToknizer = new StringTokenizer(aDocument, "\n");
        boolean first = true;
        int base = 0;
        while (sentToknizer.hasMoreElements()) {
            String line = sentToknizer.nextToken().trim();
            int count = StringUtils.countMatches(line, "\t");
            if (count != 9) {// not a proper conll file
                getUimaContext().getLogger().log(Level.INFO, "This is not valid conll File");
                throw new IOException("This is not valid conll File");
            }
            StringTokenizer lineTk = new StringTokenizer(line, "\t");

            if (first) {
                tokenNumber = Integer.parseInt(line.substring(0, line.indexOf("\t")));
                firstTokenInSentence.add(tokenNumber);
                first = false;
            }
            else {
                int lineNumber = Integer.parseInt(line.substring(0, line.indexOf("\t")));
                if (lineNumber == 1) {
                    base = tokenNumber;
                    firstTokenInSentence.add(base);
                }
                tokenNumber = base + Integer.parseInt(line.substring(0, line.indexOf("\t")));
            }

            while (lineTk.hasMoreElements()) {
                lineTk.nextToken();
                String token = lineTk.nextToken();
                text.append(token + " ");
                tokens.put(tokenNumber, token);
                lemma.put(tokenNumber, lineTk.nextToken());
                pos.put(tokenNumber, lineTk.nextToken());
                namedEntity.put(tokenNumber, lineTk.nextToken());
                lineTk.nextToken();
                int dependent = Integer.parseInt(lineTk.nextToken());
                dependencyDependent.put(tokenNumber, dependent == 0 ? 0 : base + dependent);
                dependencyFunction.put(tokenNumber, lineTk.nextToken());
                lineTk.nextToken();
                lineTk.nextToken();
            }
        }

        aJCas.setDocumentText(text.toString());

        int tokenBeginPosition = 0;
        int tokenEndPosition = 0;
        Map<String, Token> tokensStored = new HashMap<String, Token>();

        for (int i = 1; i <= tokens.size(); i++) {
            tokenBeginPosition = text.indexOf(tokens.get(i), tokenBeginPosition);
            Token outToken = new Token(aJCas, tokenBeginPosition, text.indexOf(tokens.get(i),
                    tokenBeginPosition) + tokens.get(i).length());
            tokenEndPosition = text.indexOf(tokens.get(i), tokenBeginPosition)
                    + tokens.get(i).length();
            tokenBeginPosition = tokenEndPosition;
            outToken.addToIndexes();

            POS outPos = new POS(aJCas, outToken.getBegin(), outToken.getEnd());
            outPos.setPosValue(pos.get(i));
            outPos.addToIndexes();
            outToken.setPos(outPos);

            Lemma outLemma = new Lemma(aJCas, outToken.getBegin(), outToken.getEnd());
            outLemma.setValue(lemma.get(i));
            outLemma.addToIndexes();
            outToken.setLemma(outLemma);

            tokensStored.put("t_" + i, outToken);
        }

        for (int i = 1; i <= tokens.size(); i++) {
            Dependency outDependency = new Dependency(aJCas);
            outDependency.setDependencyType(dependencyFunction.get(i));
            outDependency.setBegin(tokensStored.get("t_" + i).getBegin());
            outDependency.setEnd(tokensStored.get("t_" + i).getEnd());
            outDependency.setGovernor(tokensStored.get("t_" + i));
            if (dependencyDependent.get(i) == 0) {
                outDependency.setDependent(tokensStored.get("t_" + i));
            }
            else {
                outDependency.setDependent(tokensStored.get("t_" + dependencyDependent.get(i)));
            }
            outDependency.addToIndexes();
        }

        for (int i = 0; i < firstTokenInSentence.size(); i++) {
            Sentence outSentence = new Sentence(aJCas);
            if (i == firstTokenInSentence.size() - 1) {
                outSentence.setBegin(tokensStored.get("t_" + firstTokenInSentence.get(i)).getEnd());
                outSentence.setEnd(tokensStored.get("t_" + (tokensStored.size())).getEnd());
                outSentence.addToIndexes();
                break;
            }
            if (i == 0) {
                outSentence.setBegin(tokensStored.get("t_" + firstTokenInSentence.get(i))
                        .getBegin());
                outSentence.setEnd(tokensStored.get("t_" + firstTokenInSentence.get(i + 1))
                        .getEnd());
                outSentence.addToIndexes();
            }
            else {
                outSentence
                        .setBegin(tokensStored.get("t_" + firstTokenInSentence.get(i)).getEnd() + 1);
                outSentence.setEnd(tokensStored.get("t_" + firstTokenInSentence.get(i + 1))
                        .getEnd());
                outSentence.addToIndexes();
            }
        }
    }

    public static final String ENCODING_AUTO = "auto";
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
            is = new BufferedInputStream(res.getInputStream());

            if (ENCODING_AUTO.equals(encoding)) {
                CharsetDetector detector = new CharsetDetector();
                convertToCas(aJCas, IOUtils.toString(detector.getReader(is, null)));
            }
            else {
                CharsetDetector detector = new CharsetDetector();
                convertToCas(aJCas, IOUtils.toString(detector.getReader(is, encoding)));
            }
        }
        finally {
            closeQuietly(is);
        }

    }
}
