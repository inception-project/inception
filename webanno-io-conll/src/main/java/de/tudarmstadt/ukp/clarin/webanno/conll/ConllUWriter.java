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

import static org.apache.uima.fit.util.JCasUtil.indexCovered;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.MimeTypes;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;
import eu.openminted.share.annotations.api.DocumentationResource;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Writes a file in the CoNLL-U format.
 * 
 * @see <a href="http://universaldependencies.github.io/docs/format.html">CoNLL-U Format</a>
 */
@ResourceMetaData(name = "CoNLL-U Writer")
@DocumentationResource("${docbase}/format-reference.html#format-${command}")
@MimeTypeCapability({MimeTypes.TEXT_X_CONLL_U})
@TypeCapability(
        inputs = {
                "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
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
    @ConfigurationParameter(name = PARAM_TARGET_ENCODING, mandatory = true, 
            defaultValue = ComponentParameters.DEFAULT_ENCODING)
    private String targetEncoding;

    /**
     * Use this filename extension.
     */
    public static final String PARAM_FILENAME_EXTENSION = 
            ComponentParameters.PARAM_FILENAME_EXTENSION;
    @ConfigurationParameter(name = PARAM_FILENAME_EXTENSION, mandatory = true, defaultValue = ".conll")
    private String filenameSuffix;

    /**
     * Write fine-grained part-of-speech information.
     */
    public static final String PARAM_WRITE_POS = ComponentParameters.PARAM_WRITE_POS;
    @ConfigurationParameter(name = PARAM_WRITE_POS, mandatory = true, defaultValue = "true")
    private boolean writePos;

    /**
     * Write coarse-grained part-of-speech information.
     */
    public static final String PARAM_WRITE_CPOS = ComponentParameters.PARAM_WRITE_CPOS;
    @ConfigurationParameter(name = PARAM_WRITE_CPOS, mandatory = true, defaultValue = "true")
    private boolean writeCPos;

    /**
     * Write morphological features.
     */
    public static final String PARAM_WRITE_MORPH = ComponentParameters.PARAM_WRITE_MORPH;
    @ConfigurationParameter(name = PARAM_WRITE_MORPH, mandatory = true, defaultValue = "true")
    private boolean writeMorph;

    /**
     * Write lemma information.
     */
    public static final String PARAM_WRITE_LEMMA = ComponentParameters.PARAM_WRITE_LEMMA;
    @ConfigurationParameter(name = PARAM_WRITE_LEMMA, mandatory = true, defaultValue = "true")
    private boolean writeLemma;

    /**
     * Write syntactic dependency information.
     */
    public static final String PARAM_WRITE_DEPENDENCY = ComponentParameters.PARAM_WRITE_DEPENDENCY;
    @ConfigurationParameter(name = PARAM_WRITE_DEPENDENCY, mandatory = true, defaultValue = "true")
    private boolean writeDependency;
    
    /**
     * Write text covered by the token instead of the token form.
     */
    public static final String PARAM_WRITE_COVERED_TEXT = 
            ComponentParameters.PARAM_WRITE_COVERED_TEXT;
    @ConfigurationParameter(name = PARAM_WRITE_COVERED_TEXT, mandatory = true, defaultValue = "true")
    private boolean writeCovered;
    
    /**
     * Include the full sentence text as a comment in front of each sentence.
     */
    public static final String PARAM_WRITE_TEXT_COMMENT = "writeTextComment";
    @ConfigurationParameter(name = PARAM_WRITE_TEXT_COMMENT, mandatory = true, defaultValue = "true")
    private boolean writeTextHeader;

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        try (PrintWriter out = new PrintWriter(
                new OutputStreamWriter(getOutputStream(aJCas, filenameSuffix), targetEncoding));) {
            convert(aJCas, out);
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
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
            Map<Token, Row> ctokens = new LinkedHashMap<>();

            // Comments
            if (sentence.getId() != null) {
                aOut.printf("# %s = %s\n", ConllUReader.META_SEND_ID, sentence.getId());
            }
            if (writeTextHeader) {
                aOut.printf("# %s = %s\n", ConllUReader.META_TEXT, sentence.getCoveredText());
            }
            
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
                if (StringUtils.isBlank(rel.getFlavor())
                        || DependencyFlavor.BASIC.equals(rel.getFlavor())) {
                    ctokens.get(rel.getDependent()).deprel = rel;
                }
                else {
                    ctokens.get(rel.getDependent()).deps.add(rel);
                }
            }

            // Write sentence in CONLL-U format
            for (Row row : ctokens.values()) {
                
                String form = row.token.getCoveredText();
                if (!writeCovered) {
                    form = row.token.getText();
                }
                
                String lemma = UNUSED;
                if (writeLemma && (row.token.getLemma() != null)) {
                    lemma = row.token.getLemma().getValue();
                }

                String pos = UNUSED;
                if (writePos && (row.token.getPos() != null)
                    && row.token.getPos().getPosValue() != null) {
                    POS posAnno = row.token.getPos();
                    pos = posAnno.getPosValue();
                }

                String cpos = UNUSED;
                if (writeCPos && (row.token.getPos() != null)
                        && row.token.getPos().getCoarseValue() != null) {
                    POS posAnno = row.token.getPos();
                    cpos = posAnno.getCoarseValue();
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
                        form, lemma, cpos, pos, feats, head, deprel, deps,
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
