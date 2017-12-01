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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_ADJ;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_ADP;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_ADV;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_CONJ;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_DET;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_NOUN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_NUM;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_PRON;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_PROPN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_PUNCT;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_VERB;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_X;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Writes a file in the CoNLL-U format.
 * 
 * <ol>
 * <li>ID - <b>(ignored)</b> Word index, integer starting at 1 for each new sentence; may be a range
 * for tokens with multiple words.</li>
 * <li>FORM - <b>(Token)</b> Word form or punctuation symbol.</li>
 * <li>LEMMA - <b>(Lemma)</b> Lemma or stem of word form.</li>
 * <li>CPOSTAG - <b>(unused)</b> Google universal part-of-speech tag from the universal POS tag set.
 * </li>
 * <li>POSTAG - <b>(POS)</b> Language-specific part-of-speech tag; underscore if not available.</li>
 * <li>FEATS - <b>(MorphologicalFeatures)</b> List of morphological features from the universal
 * feature inventory or from a defined language-specific extension; underscore if not available.
 * </li>
 * <li>HEAD - <b>(Dependency)</b> Head of the current token, which is either a value of ID or zero
 * (0).</li>
 * <li>DEPREL - <b>(Dependency)</b> Universal Stanford dependency relation to the HEAD (root iff
 * HEAD = 0) or a defined language-specific subtype of one.</li>
 * <li>DEPS - <b>(Dependency)</b> List of secondary dependencies (head-deprel pairs).</li>
 * <li>MISC - <b>(unused)</b> Any other annotation.</li>
 * </ol>
 * 
 * Sentences are separated by a blank new line.
 * 
 * @see <a href="http://universaldependencies.github.io/docs/format.html">CoNLL-U Format</a>
 */
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures",
        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency" })
public class ConllUWriter
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

    public static final String PARAM_FILENAME_SUFFIX = "filenameSuffix";
    @ConfigurationParameter(name = PARAM_FILENAME_SUFFIX, mandatory = true, defaultValue = ".conll")
    private String filenameSuffix;

    public static final String PARAM_WRITE_POS = ComponentParameters.PARAM_WRITE_POS;
    @ConfigurationParameter(name = PARAM_WRITE_POS, mandatory = true, defaultValue = "true")
    private boolean writePos;

    public static final String PARAM_WRITE_MORPH = ComponentParameters.PARAM_WRITE_MORPH;
    @ConfigurationParameter(name = PARAM_WRITE_MORPH, mandatory = true, defaultValue = "true")
    private boolean writeMorph;

    public static final String PARAM_WRITE_LEMMA = ComponentParameters.PARAM_WRITE_LEMMA;
    @ConfigurationParameter(name = PARAM_WRITE_LEMMA, mandatory = true, defaultValue = "true")
    private boolean writeLemma;

    public static final String PARAM_WRITE_DEPENDENCY = ComponentParameters.PARAM_WRITE_DEPENDENCY;
    @ConfigurationParameter(name = PARAM_WRITE_DEPENDENCY, mandatory = true, defaultValue = "true")
    private boolean writeDependency;

    private Map<Class<?>, String> dkpro2ud = new HashMap<>();
    
    {
        dkpro2ud.put(POS_ADJ.class, "ADJ");
        dkpro2ud.put(POS_ADV.class, "ADV");
        dkpro2ud.put(POS_DET.class, "DET");
        dkpro2ud.put(POS_NUM.class, "NUM");
        dkpro2ud.put(POS_CONJ.class, "CONJ");
        dkpro2ud.put(POS_NOUN.class, "NOUN");
        dkpro2ud.put(POS_PROPN.class, "PROPN");
        dkpro2ud.put(POS_X.class, "X");
        dkpro2ud.put(POS_ADP.class, "ADP");
        dkpro2ud.put(POS_PRON.class, "PRON");
        dkpro2ud.put(POS_VERB.class, "VERB");
        dkpro2ud.put(POS_PUNCT.class, "PUNCT");
    }
    
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
        Map<SurfaceForm, Collection<Token>> surfaceIdx = indexCovered(aJCas, SurfaceForm.class,
                Token.class);
        Int2ObjectMap<SurfaceForm> surfaceBeginIdx = new Int2ObjectOpenHashMap<>();
        for (SurfaceForm sf : select(aJCas, SurfaceForm.class)) {
            surfaceBeginIdx.put(sf.getBegin(), sf);
        }
        
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            HashMap<Token, Row> ctokens = new LinkedHashMap<>();

            // Tokens
            List<Token> tokens = selectCovered(Token.class, sentence);
            
            for (int i = 0; i < tokens.size(); i++) {
                Row row = new Row();
                row.id = i + 1;
                row.token = tokens.get(i);
                row.noSpaceAfter = (i + 1 < tokens.size())
                        && row.token.getEnd() == tokens.get(i + 1).getBegin();
                ctokens.put(row.token, row);
            }

            // Dependencies
            for (Dependency rel : selectCovered(Dependency.class, sentence)) {
                String flavor = FSUtil.getFeature(rel, "flavor", String.class);
                if (StringUtils.isBlank(flavor) || DependencyFlavor.BASIC.equals(flavor)) {
                    ctokens.get(rel.getDependent()).deprel = rel;
                }
                else {
                    ctokens.get(rel.getDependent()).deps.add(rel);
                }
            }

            // Write sentence in CONLL-U format
            for (Row row : ctokens.values()) {
                String lemma = UNUSED;
                if (writeLemma && (row.token.getLemma() != null)) {
                    lemma = row.token.getLemma().getValue();
                }

                String pos = UNUSED;
                String cpos = UNUSED;
                if (writePos && (row.token.getPos() != null)) {
                    POS posAnno = row.token.getPos();
                    pos = posAnno.getPosValue();
                    cpos = dkpro2ud.get(posAnno.getClass());
                    if (StringUtils.isBlank(cpos)) {
                        cpos = pos;
                    }
                }
                
                int headId = UNUSED_INT;
                String deprel = UNUSED;
                String deps = UNUSED;
                if (writeDependency) {
                    if ((row.deprel != null)) {
                        deprel = row.deprel.getDependencyType();
                        headId = ctokens.get(row.deprel.getGovernor()).id;
                        if (headId == row.id) {
                            // ROOT dependencies may be modeled as a loop, ignore these.
                            headId = 0;
                        }
                    }
                    
                    StringBuilder depsBuf = new StringBuilder();
                    for (Dependency d : row.deps) {
                        if (depsBuf.length() > 0) {
                            depsBuf.append('|');
                        }
                        // Resolve self-looping root to 0-indexed root
                        int govId = ctokens.get(d.getGovernor()).id;
                        if (govId == row.id) {
                            govId = 0;
                        }
                        depsBuf.append(govId);
                        depsBuf.append(':');
                        depsBuf.append(d.getDependencyType());
                    }
                    if (depsBuf.length() > 0) {
                        deps = depsBuf.toString();
                    }
                }
                
                String head = UNUSED;
                if (headId != UNUSED_INT) {
                    head = Integer.toString(headId);
                }
                
                String feats = UNUSED;
                if (writeMorph && (row.token.getMorph() != null)) {
                    feats = row.token.getMorph().getValue();
                }
                
                String misc = UNUSED;
                if (row.noSpaceAfter) {
                    misc = "SpaceAfter=No";
                }

                SurfaceForm sf = surfaceBeginIdx.get(row.token.getBegin());
                if (sf != null) {
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    List<Token> covered = (List) surfaceIdx.get(sf);
                    int id1 = ctokens.get(covered.get(0)).id;
                    int id2 = ctokens.get(covered.get(covered.size() - 1)).id;
                    aOut.printf("%d-%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", id1, id2,
                            sf.getValue(), UNUSED, UNUSED, UNUSED, UNUSED, UNUSED, UNUSED, UNUSED,
                            UNUSED);
                }
                
                aOut.printf("%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", row.id,
                        row.token.getCoveredText(), lemma, cpos, pos, feats, head, deprel, deps,
                        misc);
            }

            aOut.println();
        }
    }

    private static final class Row
    {
        int id;
        Token token;
        boolean noSpaceAfter;
        Dependency deprel;
        List<Dependency> deps = new ArrayList<>();
    }
}
