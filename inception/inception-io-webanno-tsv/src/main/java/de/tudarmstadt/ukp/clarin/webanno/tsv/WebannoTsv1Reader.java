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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.parameter.ComponentParameters;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Reads a specific TSV File (9 TAB separated) annotation and change it to CAS object. Example of
 * Input Files: <br>
 * 1 Heutzutage heutzutage ADV _ _ 2 ADV _ _ <br>
 * Columns are separated by a TAB character and sentences are separated by a blank new line.
 *
 *
 */
public class WebannoTsv1Reader
    extends JCasResourceCollectionReader_ImplBase
{

    private String fileName;

    public void convertToCas(JCas aJCas, InputStream aIs, String aEncoding) throws IOException

    {
        StringBuilder text = new StringBuilder();
        Map<Integer, String> tokens = new HashMap<>();
        Map<Integer, String> pos = new HashMap<>();
        Map<Integer, String> lemma = new HashMap<>();
        Map<Integer, String> namedEntity = new HashMap<>();
        Map<Integer, String> dependencyFunction = new HashMap<>();
        Map<Integer, Integer> dependencyDependent = new HashMap<>();

        List<Integer> firstTokenInSentence = new ArrayList<>();

        DocumentMetaData documentMetadata = DocumentMetaData.get(aJCas);
        fileName = documentMetadata.getDocumentTitle();
        setAnnotations(aIs, aEncoding, text, tokens, pos, lemma, namedEntity, dependencyFunction,
                dependencyDependent, firstTokenInSentence);

        aJCas.setDocumentText(text.toString());

        Map<String, Token> tokensStored = new HashMap<>();

        createToken(aJCas, text, tokens, pos, lemma, tokensStored);

        createNamedEntity(namedEntity, aJCas, tokens, tokensStored);

        createDependency(aJCas, tokens, dependencyFunction, dependencyDependent, tokensStored);

        createSentence(aJCas, firstTokenInSentence, tokensStored);
    }

    /**
     * Create {@link Token} in the {@link CAS}. If the lemma and pos columns are not empty it will
     * create {@link Lemma} and {@link POS} annotations
     */
    private void createToken(JCas aJCas, StringBuilder text, Map<Integer, String> tokens,
            Map<Integer, String> pos, Map<Integer, String> lemma, Map<String, Token> tokensStored)
    {
        int tokenBeginPosition = 0;
        int tokenEndPosition = 0;

        for (int i = 1; i <= tokens.size(); i++) {
            tokenBeginPosition = text.indexOf(tokens.get(i), tokenBeginPosition);
            Token outToken = new Token(aJCas, tokenBeginPosition,
                    text.indexOf(tokens.get(i), tokenBeginPosition) + tokens.get(i).length());
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
                    begin = tokensStored.get("t_" + i).getBegin() > tokensStored
                            .get("t_" + dependencyDependent.get(i)).getBegin()
                                    ? tokensStored.get("t_" + dependencyDependent.get(i)).getBegin()
                                    : tokensStored.get("t_" + i).getBegin();
                    end = tokensStored.get("t_" + i).getEnd() < tokensStored
                            .get("t_" + dependencyDependent.get(i)).getEnd()
                                    ? tokensStored.get("t_" + dependencyDependent.get(i)).getEnd()
                                    : tokensStored.get("t_" + i).getEnd();
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
                outSentence
                        .setBegin(tokensStored.get("t_" + firstTokenInSentence.get(i)).getBegin());
                outSentence.setEnd(tokensStored.get("t_" + (tokensStored.size())).getEnd());
                outSentence.addToIndexes();
            }
            else if (i == 0) {
                outSentence
                        .setBegin(tokensStored.get("t_" + firstTokenInSentence.get(i)).getBegin());
                outSentence
                        .setEnd(tokensStored.get("t_" + firstTokenInSentence.get(i + 1)).getEnd());
                outSentence.addToIndexes();
            }
            else {
                outSentence.setBegin(
                        tokensStored.get("t_" + firstTokenInSentence.get(i)).getEnd() + 1);
                outSentence
                        .setEnd(tokensStored.get("t_" + firstTokenInSentence.get(i + 1)).getEnd());
                outSentence.addToIndexes();
            }
        }
    }

    /**
     * Iterate through all lines and get available annotations<br>
     * First column is sentence number and a blank new line marks end of a sentence<br>
     * The Second column is the token <br>
     * The third column is the lemma annotation <br>
     * The fourth column is the POS annotation <br>
     * The fifth column is used for Named Entity annotations (Multiple annotations separated by |
     * character) <br>
     * The sixth column is the origin token number of dependency parsing <br>
     * The seventh column is the function/type of the dependency parsing <br>
     * eighth and ninth columns are undefined currently
     */
    private void setAnnotations(InputStream aIs, String aEncoding, StringBuilder text,
            Map<Integer, String> tokens, Map<Integer, String> pos, Map<Integer, String> lemma,
            Map<Integer, String> namedEntity, Map<Integer, String> dependencyFunction,
            Map<Integer, Integer> dependencyDependent, List<Integer> firstTokenInSentence)
        throws IOException
    {
        int tokenNumber = 0;
        boolean first = true;
        int base = 0;

        LineIterator lineIterator = IOUtils.lineIterator(aIs, aEncoding);
        boolean textFound = false;
        StringBuilder tmpText = new StringBuilder();
        while (lineIterator.hasNext()) {
            String line = lineIterator.next().trim();
            if (line.startsWith("#text=")) {
                text.append(line.substring(6)).append("\n");
                textFound = true;
                continue;
            }
            if (line.startsWith("#")) {
                continue; // it is a comment line
            }
            int count = StringUtils.countMatches(line, "\t");
            if (line.isEmpty()) {
                continue;
            }
            if (count != 9) { // not a proper TSV file
                getUimaContext().getLogger().log(Level.INFO, "This is not a valid TSV File");
                throw new IOException(fileName + " This is not a valid TSV File");
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

                // for backward compatibility
                tmpText.append(token).append(" ");

                tokens.put(tokenNumber, token);
                lemma.put(tokenNumber, lineTk.nextToken());
                pos.put(tokenNumber, lineTk.nextToken());
                String ne = lineTk.nextToken();
                lineTk.nextToken();// make it compatible with prev WebAnno TSV reader
                namedEntity.put(tokenNumber, (ne.equals("_") || ne.equals("-")) ? "O" : ne);
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
        if (!textFound) {
            text.append(tmpText);
        }
    }

    public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
    @ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String encoding;

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);
        try (InputStream is = res.getInputStream()) {
            convertToCas(aJCas, is, encoding);
        }
    }

    /**
     * Creates Named Entities from CoNLL BIO format to CAS format
     */
    private void createNamedEntity(Map<Integer, String> aNamedEntityMap, JCas aJCas,
            Map<Integer, String> aTokensMap, Map<String, Token> aJcasTokens)
    {

        Map<Integer, NamedEntity> indexedNeAnnos = new LinkedHashMap<>();

        for (int i = 1; i <= aTokensMap.size(); i++) {
            if (aNamedEntityMap.get(i).equals("O")) {
                continue;
            }
            int index = 1;// to maintain multiple span ne annotation in the same index
            for (String ne : aNamedEntityMap.get(i).split("\\|")) {

                if (ne.equals("O")) { // for annotations such as B_LOC|O|I_PER and the like
                    index++;
                }
                else if (ne.startsWith("B_") || ne.startsWith("B-")) {
                    NamedEntity outNamedEntity = new NamedEntity(aJCas,
                            aJcasTokens.get("t_" + i).getBegin(),
                            aJcasTokens.get("t_" + i).getEnd());
                    outNamedEntity.setValue(ne.substring(2));
                    outNamedEntity.addToIndexes();
                    indexedNeAnnos.put(index, outNamedEntity);
                    index++;
                }
                else if (ne.startsWith("I_") || ne.startsWith("I-")) {
                    NamedEntity outNamedEntity = indexedNeAnnos.get(index);
                    outNamedEntity.setEnd(aJcasTokens.get("t_" + i).getEnd());
                    outNamedEntity.addToIndexes();
                    index++;
                }
                else {
                    // NE is not in IOB format. store one NE per token. No way to detect multiple
                    // token NE
                    NamedEntity outNamedEntity = new NamedEntity(aJCas,
                            aJcasTokens.get("t_" + i).getBegin(),
                            aJcasTokens.get("t_" + i).getEnd());
                    outNamedEntity.setValue(ne);
                    outNamedEntity.addToIndexes();
                    indexedNeAnnos.put(index, outNamedEntity);
                    index++;
                }
            }
        }
    }
}
