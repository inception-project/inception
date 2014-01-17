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
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Writes a specific TSV File (9 TAB separated) annotation from the CAS object. Example of output
 * file:<br>
 * <br>
 * 1 Heutzutage heutzutage ADV _ _ 2 ADV _ _ <br>
 * <br>
 * First column: token Number, in a sentence <br>
 * second Column: the token <br>
 * third column: the lemma <br>
 * fourth column: the POS <br>
 * fifth/sixth xolumn: Named Entity annotations in BIO(the sixth column is used to encode nested
 * Named Entity) <br>
 * seventh column: the target token for a dependency parsing <br>
 * eighth column: the function of the dependency parsing <br>
 * ninth and tenth column: Not Yet Known
 *
 * Columns are separated by TAB character and sentences are separated by a blank new line
 *
 * @author Seid Muhie Yimam
 *
 */

public class WebannoTsvWriter
    extends JCasFileWriter_ImplBase
{

    /**
     * Name of configuration parameter that contains the character encoding used by the input files.
     */
    public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
    @ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String encoding;

    public static final String PARAM_FILENAME_SUFFIX = "filenameSuffix";
    @ConfigurationParameter(name = PARAM_FILENAME_SUFFIX, mandatory = true, defaultValue = ".tsv")
    private String filenameSuffix;

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        OutputStream docOS = null;
        try {
            docOS = getOutputStream(aJCas, filenameSuffix);
            convertToTsv(aJCas, docOS, encoding);
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
        finally {
            closeQuietly(docOS);
        }

    }

    private void convertToTsv(JCas aJCas, OutputStream aOs, String aEncoding)
        throws IOException
    {
        // StringBuilder conllSb = new StringBuilder();

        int sentId = 1;
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            IOUtils.write("#id=" + sentId++ + "\n", aOs, aEncoding);
            // Map of token and the dependent (token address used as a Key)
            Map<Integer, Integer> dependentMap = new HashMap<Integer, Integer>();
            // Map of governor token address and its token position
            Map<Integer, Integer> dependencyMap = new HashMap<Integer, Integer>();
            // Map of goverenor token address and its dependency function value
            Map<Integer, String> dependencyTypeMap = new HashMap<Integer, String>();

            for (Dependency dependecny : selectCovered(Dependency.class, sentence)) {
                dependentMap.put(dependecny.getDependent().getAddress(), dependecny.getGovernor()
                        .getAddress());
            }

            // List of governors (heads), that will be used, if ROOT is not explicitly added,
            // we should add it.
            List<Integer> governorAddresses = new ArrayList<Integer>();

            for (Dependency dependecny : selectCovered(Dependency.class, sentence)) {
                governorAddresses.add(dependecny.getGovernor().getAddress());
            }

            int i = 1;
            for (Token token : selectCovered(Token.class, sentence)) {
                dependencyMap.put(token.getAddress(), i);
                i++;
            }

            for (Dependency dependecny : selectCovered(Dependency.class, sentence)) {
                dependencyTypeMap.put(dependecny.getDependent().getAddress(),
                        dependecny.getDependencyType());
            }

            int j = 1;
            // Add named Entity to a token
            Map<Integer, Map<Integer, String>> neAnnos = new HashMap<Integer, Map<Integer, String>>();

            createNEColumn(sentence, neAnnos);
            int max = 0;
            for (Map<Integer, String> neAnno : neAnnos.values()) {
                if (neAnno.size() > max) {
                    max = neAnno.size();
                }
            }
            Map<Integer, Integer> nestedNe = new HashMap<Integer, Integer>();
            for (Token token : selectCovered(Token.class, sentence)) {

                String lemma = token.getLemma() == null ? "_" : token.getLemma().getValue();
                String pos = token.getPos() == null ? "_" : token.getPos().getPosValue();
                String dependent = "_";

                String neAnnotations = "O";
                Map<Integer, String> ne = neAnnos.get(token.getAddress());
                List<Integer> prevAnnos = new ArrayList<Integer>();

                // ========================GET the Previous Named Entity annotations
                // positions=================

                if (ne != null) {
                    neAnnotations = "";
                    for (Integer address : ne.keySet()) {
                        if (nestedNe.get(address) != null) {
                            prevAnnos.add(nestedNe.get(address));
                            ne.put(address, "I_" + ne.get(address));
                        }
                    }

                    // get unoccupied possition for ne annotation now

                    int index = 1;
                    for (Integer address : ne.keySet()) {
                        if (nestedNe.get(address) == null) {
                            while (true) {
                                if (prevAnnos.contains(index)) {
                                    index++;
                                }
                                else {
                                    ne.put(address, "B_" + ne.get(address));
                                    nestedNe.put(address, index);
                                    prevAnnos.add(index);
                                    index++;
                                    break;
                                }
                            }
                        }
                    }

                    Collections.sort(prevAnnos);
                    for (int indexRange = 1; indexRange <= Collections.max(prevAnnos); indexRange++) {
                        boolean added = false;
                        for (Integer address : ne.keySet()) {
                            if (nestedNe.get(address) == indexRange) {
                                neAnnotations = neAnnotations + ne.get(address) + "||";
                                added = true;
                                break;
                            }
                        }
                        if (!added) {
                            neAnnotations = neAnnotations + "O||";
                        }
                    }

                }
                int lastSeparator = neAnnotations.lastIndexOf("||");
                if (lastSeparator > 0) {
                    neAnnotations = new StringBuilder(neAnnotations).replace(lastSeparator,
                            lastSeparator + 2, "").toString();
                }
                String type = dependencyTypeMap.get(token.getAddress()) == null ? "_"
                        : dependencyTypeMap.get(token.getAddress());

                if (dependentMap.get(token.getAddress()) != null) {
                    if (dependencyMap.get(dependentMap.get(token.getAddress())) != null) {
                        dependent = "" + dependencyMap.get(dependentMap.get(token.getAddress()));
                    }
                }
                // ROOT was not explicitly mentioned in the annotation
                else if (governorAddresses.contains(token.getAddress())) {
                    dependent = "0";
                    type = "ROOT";
                }

                if (dependentMap.get(token.getAddress()) != null
                        && dependencyMap.get(dependentMap.get(token.getAddress())) != null
                        && j == dependencyMap.get(dependentMap.get(token.getAddress()))) {
                    IOUtils.write(j + "\t" + token.getCoveredText() + "\t" + lemma + "\t" + pos
                            + "\t" + neAnnotations + "\t" + 0 + "\t" + type + "\t_\t_\n", aOs,
                            aEncoding);
                }
                else {
                    IOUtils.write(j + "\t" + token.getCoveredText() + "\t" + lemma + "\t" + pos
                            + "\t" + neAnnotations + "\t" + dependent + "\t" + type + "\t_\t_\n",
                            aOs, aEncoding);
                }
                j++;
            }
            IOUtils.write("\n", aOs, aEncoding);
        }

        // add the text at the bottom, hence, no need to re-construct texts as well as token
        // positions
        IOUtils.write("#text=" + aJCas.getDocumentText() + "\n\n", aOs, aEncoding);
    }

    /**
     * If a named entity covers multiple span, it will be recorded only to the first token, with the
     * beign/end offsets attached to it
     */

    private void createNEColumn(Sentence sentence,
            Map<Integer, Map<Integer, String>> tokenNamedEntityMap)
    {
        for (NamedEntity namedEntity : selectCovered(NamedEntity.class, sentence)) {
            for (Token token : selectCovered(Token.class, sentence)) {
                if (namedEntity.getBegin() <= token.getBegin()
                        && namedEntity.getEnd() >= token.getEnd()) {
                    if (tokenNamedEntityMap.get(token.getAddress()) == null) {

                        if (tokenNamedEntityMap.size() == 0) {
                            Map<Integer, String> neAnnoMaps = new LinkedHashMap<Integer, String>();
                            neAnnoMaps.put(namedEntity.getAddress(), namedEntity.getValue());
                            tokenNamedEntityMap.put(token.getAddress(), neAnnoMaps);
                        }
                        else {
                            Map<Integer, String> neAnnoMaps = new LinkedHashMap<Integer, String>();
                            neAnnoMaps.put(namedEntity.getAddress(), namedEntity.getValue());
                            tokenNamedEntityMap.put(token.getAddress(), neAnnoMaps);
                        }
                    }
                    else {
                        Map<Integer, String> neAnnoMaps = tokenNamedEntityMap.get(token
                                .getAddress());
                        neAnnoMaps.put(namedEntity.getAddress(), namedEntity.getValue());
                        tokenNamedEntityMap.put(token.getAddress(), neAnnoMaps);

                    }
                }
            }
        }
    }

}
