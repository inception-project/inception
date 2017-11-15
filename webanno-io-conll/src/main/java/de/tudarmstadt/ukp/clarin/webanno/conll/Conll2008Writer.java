/*
 * Copyright 2016
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
 */
package de.tudarmstadt.ukp.clarin.webanno.conll;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.uima.fit.util.JCasUtil.indexCovered;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArgLink;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;

/**
 * Writes a file in the CoNLL-2008 format.
 * 
 * @see <a href="http://surdeanu.info/conll08/">CoNLL 2008 Shared Task: Joint Learning of 
 *      Syntactic and Semantic Dependencies</a>
 * @see <a href="http://www.aclweb.org/anthology/W08-2121">The CoNLL-2008 Shared Task on
 *      Joint Parsing of Syntactic and Semantic Dependencies</a>
 */
@ResourceMetaData(name = "CoNLL 2008 Writer")
@TypeCapability(
        inputs = { 
                "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
                "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
                "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
                "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures",
                "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
                "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
                "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency",
                "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred",
                "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg"})
public class Conll2008Writer
    extends JCasFileWriter_ImplBase
{
    private static final String UNUSED = "_";
    private static final int UNUSED_INT = -1;

    /**
     * Character encoding of the output data.
     */
    public static final String PARAM_TARGET_ENCODING = ComponentParameters.PARAM_TARGET_ENCODING;
    @ConfigurationParameter(name = PARAM_TARGET_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String targetEncoding;

    public static final String PARAM_FILENAME_EXTENSION = 
            ComponentParameters.PARAM_FILENAME_EXTENSION;
    @ConfigurationParameter(name = PARAM_FILENAME_EXTENSION, mandatory = true, defaultValue = ".conll")
    private String filenameSuffix;

    public static final String PARAM_WRITE_POS = ComponentParameters.PARAM_WRITE_POS;
    @ConfigurationParameter(name = PARAM_WRITE_POS, mandatory = true, defaultValue = "true")
    private boolean writePos;

    public static final String PARAM_WRITE_MORPH = "writeMorph";
    @ConfigurationParameter(name = PARAM_WRITE_MORPH, mandatory = true, defaultValue = "true")
    private boolean writeMorph;

    public static final String PARAM_WRITE_LEMMA = ComponentParameters.PARAM_WRITE_LEMMA;
    @ConfigurationParameter(name = PARAM_WRITE_LEMMA, mandatory = true, defaultValue = "true")
    private boolean writeLemma;

    public static final String PARAM_WRITE_DEPENDENCY = ComponentParameters.PARAM_WRITE_DEPENDENCY;
    @ConfigurationParameter(name = PARAM_WRITE_DEPENDENCY, mandatory = true, defaultValue = "true")
    private boolean writeDependency;

    public static final String PARAM_WRITE_SEMANTIC_PREDICATE = "writeSemanticPredicate";
    @ConfigurationParameter(name = PARAM_WRITE_SEMANTIC_PREDICATE, mandatory = true, defaultValue = "true")
    private boolean writeSemanticPredicate;

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new OutputStreamWriter(getOutputStream(aJCas, filenameSuffix),
                    targetEncoding));
            convert(aJCas, out);
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
        finally {
            closeQuietly(out);
        }
    }

    private void convert(JCas aJCas, PrintWriter aOut)
    {
        Map<Token, Collection<SemPred>> predIdx = indexCovered(aJCas, Token.class, SemPred.class);
        Map<SemArg, Collection<Token>> argIdx = indexCovered(aJCas, SemArg.class, Token.class);
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            HashMap<Token, Row> ctokens = new LinkedHashMap<Token, Row>();

            // Tokens
            List<Token> tokens = selectCovered(Token.class, sentence);
            
            // Check if we should try to include the FEATS in output
            List<MorphologicalFeatures> morphology = selectCovered(MorphologicalFeatures.class,
                    sentence);
            boolean useFeats = tokens.size() == morphology.size();

            List<SemPred> preds = selectCovered(SemPred.class, sentence);
            
            for (int i = 0; i < tokens.size(); i++) {
                Row row = new Row();
                row.id = i + 1;
                row.token = tokens.get(i);
                row.args = new SemArgLink[preds.size()];
                if (useFeats) {
                    row.feats = morphology.get(i);
                }
                
                // If there are multiple semantic predicates for the current token, then 
                // we keep only the first
                Collection<SemPred> predsForToken = predIdx.get(row.token);
                if (predsForToken != null && !predsForToken.isEmpty()) {
                    row.pred = predsForToken.iterator().next();
                }
                ctokens.put(row.token, row);
            }

            // Dependencies
            List<Dependency> basicDeps = selectCovered(Dependency.class, sentence).stream()
                    .filter(dep -> {
                        String flavor = FSUtil.getFeature(dep, "flavor", String.class);
                        return flavor == null || DependencyFlavor.BASIC.equals(flavor);
                    })
                    .collect(Collectors.toList());
            for (Dependency rel : basicDeps) {
                Row row =  ctokens.get(rel.getDependent());
                if (row.deprel != null) {
                    throw new IllegalStateException("Illegal basic dependency structure - token ["
                            + row.token.getCoveredText()
                            + "] is dependent of more than one dependency.");
                }
                row.deprel = rel;
            }

            // Semantic arguments
            for (int p = 0; p < preds.size(); p++) {
                FSArray args = preds.get(p).getArguments();
                for (SemArgLink arg : select(args, SemArgLink.class)) {
                    for (Token t : argIdx.get(arg.getTarget())) {
                        Row row = ctokens.get(t);
                        row.args[p] = arg;
                    }
                }
            }
            
            // Write sentence in CONLL 2009 format
            for (Row row : ctokens.values()) {
                int id = row.id;
                
                String form = row.token.getCoveredText();
                
                String lemma = UNUSED;
                if (writeLemma && (row.token.getLemma() != null)) {
                    lemma = row.token.getLemma().getValue();
                }
                String gpos = UNUSED;
                if (writePos && (row.token.getPos() != null)) {
                    POS posAnno = row.token.getPos();
                    gpos = posAnno.getPosValue();
                }

                String ppos = UNUSED;

                String split_form = UNUSED;

                String split_lemma = UNUSED;

                String pposs = UNUSED;

                int headId = UNUSED_INT;
                String deprel = UNUSED;
                if (writeDependency && (row.deprel != null)) {
                    deprel = row.deprel.getDependencyType();
                    headId = ctokens.get(row.deprel.getGovernor()).id;
                    if (headId == row.id) {
                        // ROOT dependencies may be modeled as a loop, ignore these.
                        headId = 0;
                    }
                }

                String head = UNUSED;
                if (headId != UNUSED_INT) {
                    head = Integer.toString(headId);
                }
                
                String pred = UNUSED;
                StringBuilder apreds = new StringBuilder();
                if (writeSemanticPredicate) {
                    if (row.pred != null) {
                        pred = row.pred.getCategory();
                    }
                    
                    for (SemArgLink arg : row.args) {
                        if (apreds.length() > 0) {
                            apreds.append('\t');
                        }
                        apreds.append(arg != null ? arg.getRole() : UNUSED);
                    }
                }

                aOut.printf("%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", id,
                        form, lemma, gpos, ppos, split_form, split_lemma, pposs, head, deprel, pred,
                        apreds);
            }

            aOut.println();
        }
    }

    private static final class Row
    {
        int id;
        Token token;
        MorphologicalFeatures feats;
        Dependency deprel;
        SemPred pred;
        SemArgLink[] args; // These are the arguments roles for the current token!
    }
}
