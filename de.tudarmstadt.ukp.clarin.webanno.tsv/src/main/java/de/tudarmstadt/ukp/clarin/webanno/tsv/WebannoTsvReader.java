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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Reads a specific TSV File (9 TAB separated) annotation and change it to CAS object. Example of
 * Input Files: <br>1   Heutzutage  heutzutage  ADV _   _   2   ADV _   _ <br>
 * Columns are separated by a TAB character and sentences are separated by a blank new line
 * see the {@link WebannoTsvReader#setAnnotations(InputStream, String, StringBuilder, Map, Map, Map, Map, Map, Map, Map, List)}
 * @author Seid Muhie Yimam
 *
 */
public class WebannoTsvReader
    extends JCasResourceCollectionReader_ImplBase
{

    public void convertToCas(JCas aJCas, InputStream aIs, String aEncoding)
        throws IOException

    {
        StringBuilder text = new StringBuilder();
        Map<Integer, String> tokens = new HashMap<Integer, String>();
        Map<Integer, String> pos = new HashMap<Integer, String>();
        Map<Integer, String> lemma = new HashMap<Integer, String>();
        Map<Integer, String> namedEntity1 = new HashMap<Integer, String>();
        Map<Integer, String> namedEntity2 = new HashMap<Integer, String>();
        Map<Integer, String> dependencyFunction = new HashMap<Integer, String>();
        Map<Integer, Integer> dependencyDependent = new HashMap<Integer, Integer>();

        List<Integer> firstTokenInSentence = new ArrayList<Integer>();

        setAnnotations(aIs, aEncoding, text, tokens, pos, lemma, namedEntity1, namedEntity2,
                dependencyFunction, dependencyDependent, firstTokenInSentence);

        aJCas.setDocumentText(text.toString());

        Map<String, Token> tokensStored = new HashMap<String, Token>();

        createToken(aJCas, text, tokens, pos, lemma, tokensStored);

        createNamedEntity(namedEntity1, aJCas, tokens, tokensStored);
        // For Nested Named Entity
        createNamedEntity(namedEntity2, aJCas, tokens, tokensStored);

        createDependency(aJCas, tokens, dependencyFunction, dependencyDependent, tokensStored);

        createSentence(aJCas, firstTokenInSentence, tokensStored);
    }

    /**
     * Create {@link Token} in the {@link CAS}. If the lemma and pos columns are not empty
     * it will create {@link Lemma} and {@link POS} annotations
     */
    private void createToken(JCas aJCas, StringBuilder text, Map<Integer, String> tokens,
            Map<Integer, String> pos, Map<Integer, String> lemma, Map<String, Token> tokensStored)
    {
        int tokenBeginPosition = 0;
        int tokenEndPosition = 0;

        for (int i = 1; i <= tokens.size(); i++) {
            tokenBeginPosition = text.indexOf(tokens.get(i), tokenBeginPosition);
            Token outToken = new Token(aJCas, tokenBeginPosition, text.indexOf(tokens.get(i),
                    tokenBeginPosition) + tokens.get(i).length());
            tokenEndPosition = text.indexOf(tokens.get(i), tokenBeginPosition)
                    + tokens.get(i).length();
            tokenBeginPosition = tokenEndPosition;
            outToken.addToIndexes();

            // Add pos to CAS if exist
            if (!pos.get(i).equals("_")) {
                POS outPos = new POS(aJCas, outToken.getBegin(), outToken.getEnd());
                outPos.setPosValue(pos.get(i));
                outPos.addToIndexes();
                outToken.setPos(outPos);
            }

            // Add lemma if exist
            if (!lemma.get(i).equals("_")) {
                Lemma outLemma = new Lemma(aJCas, outToken.getBegin(), outToken.getEnd());
                outLemma.setValue(lemma.get(i));
                outLemma.addToIndexes();
                outToken.setLemma(outLemma);
            }
            tokensStored.put("t_" + i, outToken);
        }
    }

    /**
     * add dependency parsing to CAS
     */
    private void createDependency(JCas aJCas, Map<Integer, String> tokens,
            Map<Integer, String> dependencyFunction, Map<Integer, Integer> dependencyDependent,
            Map<String, Token> tokensStored)
    {
        for (int i = 1; i <= tokens.size(); i++) {
            if (dependencyFunction.get(i) != null) {
                Dependency outDependency = new Dependency(aJCas);
                outDependency.setDependencyType(dependencyFunction.get(i));

                // if span A has (start,end)= (20, 26) and B has (start,end)= (30, 36)
                // arc drawn from A to B, dependency will have (start, end) = (20, 36)
                // arc drawn from B to A, still dependency will have (start, end) = (20, 36)
                int begin = 0, end = 0;
                // if not ROOT
                if (dependencyDependent.get(i) != 0) {
                    begin = tokensStored.get("t_" + i).getBegin() > tokensStored.get(
                            "t_" + dependencyDependent.get(i)).getBegin() ? tokensStored.get(
                            "t_" + dependencyDependent.get(i)).getBegin() : tokensStored.get(
                            "t_" + i).getBegin();
                    end = tokensStored.get("t_" + i).getEnd() < tokensStored.get(
                            "t_" + dependencyDependent.get(i)).getEnd() ? tokensStored.get(
                            "t_" + dependencyDependent.get(i)).getEnd() : tokensStored
                            .get("t_" + i).getEnd();
                }
                else {
                    begin = tokensStored.get("t_" + i).getBegin();
                    end = tokensStored.get("t_" + i).getEnd();
                }

                outDependency.setBegin(begin);
                outDependency.setEnd(end);
                outDependency.setDependent(tokensStored.get("t_" + i));
                if (dependencyDependent.get(i) == 0) {
                    outDependency.setGovernor(tokensStored.get("t_" + i));
                }
                else {
                    outDependency.setGovernor(tokensStored.get("t_" + dependencyDependent.get(i)));
                }
                outDependency.addToIndexes();
            }
        }
    }

    /**
     * Add sentence layer to CAS
     */
    private void createSentence(JCas aJCas, List<Integer> firstTokenInSentence,
            Map<String, Token> tokensStored)
    {
        for (int i = 0; i < firstTokenInSentence.size(); i++) {
            Sentence outSentence = new Sentence(aJCas);
            // Only last sentence, and no the only sentence in the document (i!=0)
            if (i == firstTokenInSentence.size() - 1 && i != 0) {
                outSentence.setBegin(tokensStored.get("t_" + firstTokenInSentence.get(i)).getEnd());
                outSentence.setEnd(tokensStored.get("t_" + (tokensStored.size())).getEnd());
                outSentence.addToIndexes();
                break;
            }
            if (i == firstTokenInSentence.size() - 1 && i == 0) {
                outSentence.setBegin(tokensStored.get("t_" + firstTokenInSentence.get(i))
                        .getBegin());
                outSentence.setEnd(tokensStored.get("t_" + (tokensStored.size())).getEnd());
                outSentence.addToIndexes();
            }
            else if (i == 0) {
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

    /**
     * Iterate through all lines and get available annotations<br>
     * First column is sentence number  and a blank new line marks end of a sentence<br>
     * The Second column is the token <br>
     * The third column is the lemma annotation <br>
     * The fourth column is the POS annotation <br>
     * The fifth and sixth columns are Named Entity annotations (sixth column nested NE) <br>
     * The seventh column is the origin token number of dependency parsing <br>
     * The eighth column is the function/type of the dependency parsing <br>
     * Ninth and tenth columns are undefind currently
     */
    private void setAnnotations(InputStream aIs, String aEncoding, StringBuilder text,
            Map<Integer, String> tokens, Map<Integer, String> pos, Map<Integer, String> lemma,
            Map<Integer, String> namedEntity1, Map<Integer, String> namedEntity2,
            Map<Integer, String> dependencyFunction, Map<Integer, Integer> dependencyDependent,
            List<Integer> firstTokenInSentence)
        throws IOException
    {
        int tokenNumber = 0;
        boolean first = true;
        int base = 0;

        LineIterator lineIterator = IOUtils.lineIterator(aIs, aEncoding);
        while (lineIterator.hasNext()) {
            String line = lineIterator.next().trim();
            int count = StringUtils.countMatches(line, "\t");
            if (line.isEmpty()) {
                continue;
            }
            if (count != 9) {// not a proper TSV file
                getUimaContext().getLogger().log(Level.INFO, "This is not a valid TSV File");
                throw new IOException("This is not a valid TSV File");
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
                String ne1 = lineTk.nextToken();
                String ne2 = lineTk.nextToken();
                namedEntity1.put(tokenNumber, ne1.equals("_") ? "O" : ne1);
                namedEntity2.put(tokenNumber, ne2.equals("_") ? "O" : ne2);
                String dependentValue = lineTk.nextToken();
                if (NumberUtils.isDigits(dependentValue)) {
                    int dependent = Integer.parseInt(dependentValue);
                    dependencyDependent.put(tokenNumber, dependent == 0 ? 0 : base + dependent);
                    dependencyFunction.put(tokenNumber, lineTk.nextToken());
                }
                else {
                    lineTk.nextToken();
                }
                lineTk.nextToken();
                lineTk.nextToken();
            }
        }
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

    /**
     * Creates Named Entities from CoNLL BIO format to CAS format
     */
    private void createNamedEntity(Map<Integer, String> aNamedEntityMap, JCas aJCas,
            Map<Integer, String> aTokensMap, Map<String, Token> aJcasTokens)
    {
        String previousNamedEntity = "O";
        int namedEntityBegin = -1;
        int namedEntityEnd = -1;

        for (int i = 1; i <= aTokensMap.size(); i++) {
            if (previousNamedEntity.equals("O") && aNamedEntityMap.get(i).equals("O")) {
                continue;
            }

            if (!aNamedEntityMap.get(i).equals("O") && namedEntityBegin == -1) {
                // First Named Entity
                namedEntityBegin = aJcasTokens.get("t_" + i).getBegin();
                namedEntityEnd = aJcasTokens.get("t_" + i).getEnd();
                previousNamedEntity = aNamedEntityMap.get(i);
            }
            else if (!previousNamedEntity.equals("O")) {
                // Named Entity continues
                if (aNamedEntityMap.get(i).startsWith("I_")) {
                    namedEntityEnd = aJcasTokens.get("t_" + i).getEnd();
                }
                else if (aNamedEntityMap.get(i).equals("O")) {

                    NamedEntity outNamedEntity = new NamedEntity(aJCas, namedEntityBegin,
                            namedEntityEnd);
                    outNamedEntity.setValue(previousNamedEntity.substring(2));
                    outNamedEntity.addToIndexes();

                    previousNamedEntity = "O";
                }
                // Different named entity
                else if (aNamedEntityMap.get(i).startsWith("B_")) {

                    NamedEntity outNamedEntity = new NamedEntity(aJCas, namedEntityBegin,
                            namedEntityEnd);
                    outNamedEntity.setValue(previousNamedEntity.substring(2));
                    outNamedEntity.addToIndexes();

                    namedEntityBegin = aJcasTokens.get("t_" + i).getBegin();
                    namedEntityEnd = aJcasTokens.get("t_" + i).getEnd();
                    previousNamedEntity = aNamedEntityMap.get(i);
                }
            }
            else if (!aNamedEntityMap.get(i).equals("O")) {
                // First Named Entity
                namedEntityBegin = aJcasTokens.get("t_" + i).getBegin();
                namedEntityEnd = aJcasTokens.get("t_" + i).getEnd();
                previousNamedEntity = aNamedEntityMap.get(i);
            }
        }
        // If the last token have a named Entity with Multiple span, add it
        int lastTokenIndex = aTokensMap.size();
        String lastNamedEntity = aNamedEntityMap.get(lastTokenIndex);
        if (lastNamedEntity.startsWith("I_")) {
            NamedEntity outNamedEntity = new NamedEntity(aJCas, namedEntityBegin, namedEntityEnd);
            outNamedEntity.setValue(previousNamedEntity.substring(2));
            outNamedEntity.addToIndexes();
        }
    }
}
