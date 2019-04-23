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

import static de.tudarmstadt.ukp.dkpro.core.api.resources.MappingProviderFactory.createPosMappingProvider;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.MimeTypes;
import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionUtils;
import de.tudarmstadt.ukp.dkpro.core.api.resources.MappingProvider;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;
import eu.openminted.share.annotations.api.DocumentationResource;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Reads a file in the CoNLL-U format.
 * 
 * @see <a href="http://universaldependencies.github.io/docs/format.html">CoNLL-U Format</a>
 */
@ResourceMetaData(name = "CoNLL-U Reader")
@DocumentationResource("${docbase}/format-reference.html#format-${command}")
@MimeTypeCapability({MimeTypes.TEXT_X_CONLL_U})
@TypeCapability(
        outputs = { 
                "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
                "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
                "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
                "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures",
                "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
                "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
                "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency" })
public class ConllUReader
    extends JCasResourceCollectionReader_ImplBase
{
    /**
     * Character encoding of the input data.
     */
    public static final String PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
    @ConfigurationParameter(name = PARAM_SOURCE_ENCODING, mandatory = true, 
            defaultValue = ComponentParameters.DEFAULT_ENCODING)
    private String sourceEncoding;

    /**
     * Read fine-grained part-of-speech information.
     */
    public static final String PARAM_READ_POS = ComponentParameters.PARAM_READ_POS;
    @ConfigurationParameter(name = PARAM_READ_POS, mandatory = true, defaultValue = "true")
    private boolean readPos;

    /**
     * Read coarse-grained part-of-speech information.
     */
    public static final String PARAM_READ_CPOS = ComponentParameters.PARAM_READ_CPOS;
    @ConfigurationParameter(name = PARAM_READ_CPOS, mandatory = true, defaultValue = "true")
    private boolean readCPos;

    /**
     * Treat coarse-grained part-of-speech as fine-grained part-of-speech information.
     */
    public static final String PARAM_USE_CPOS_AS_POS = "useCPosAsPos";
    @ConfigurationParameter(name = PARAM_USE_CPOS_AS_POS, mandatory = true, defaultValue = "false")
    private boolean useCPosAsPos;

    /**
     * Use this part-of-speech tag set to use to resolve the tag set mapping instead of using the
     * tag set defined as part of the model meta data. This can be useful if a custom model is
     * specified which does not have such meta data, or it can be used in readers.
     */
    public static final String PARAM_POS_TAG_SET = ComponentParameters.PARAM_POS_TAG_SET;
    @ConfigurationParameter(name = PARAM_POS_TAG_SET, mandatory = false)
    protected String posTagset;

    /**
     * Load the part-of-speech tag to UIMA type mapping from this location instead of locating
     * the mapping automatically.
     */
    public static final String PARAM_POS_MAPPING_LOCATION = 
            ComponentParameters.PARAM_POS_MAPPING_LOCATION;
    @ConfigurationParameter(name = PARAM_POS_MAPPING_LOCATION, mandatory = false)
    protected String posMappingLocation;
    
    /**
     * Read morphological features.
     */
    public static final String PARAM_READ_MORPH = ComponentParameters.PARAM_READ_MORPH;
    @ConfigurationParameter(name = PARAM_READ_MORPH, mandatory = true, defaultValue = "true")
    private boolean readMorph;

    /**
     * Read lemma information.
     */
    public static final String PARAM_READ_LEMMA = ComponentParameters.PARAM_READ_LEMMA;
    @ConfigurationParameter(name = PARAM_READ_LEMMA, mandatory = true, defaultValue = "true")
    private boolean readLemma;

    /**
     * Read syntactic dependency information.
     */
    public static final String PARAM_READ_DEPENDENCY = ComponentParameters.PARAM_READ_DEPENDENCY;
    @ConfigurationParameter(name = PARAM_READ_DEPENDENCY, mandatory = true, defaultValue = "true")
    private boolean readDependency;

    private static final String UNUSED = "_";

    private static final int ID = 0;
    private static final int FORM = 1;
    private static final int LEMMA = 2;
    private static final int CPOSTAG = 3;
    private static final int POSTAG = 4;
    private static final int FEATS = 5;
    private static final int HEAD = 6;
    private static final int DEPREL = 7;
    private static final int DEPS = 8;
    private static final int MISC = 9;
    
    public static final String META_SEND_ID = "sent_id";
    public static final String META_TEXT = "text";

    private MappingProvider posMappingProvider;

    @Override
    public void initialize(UimaContext aContext)
        throws ResourceInitializationException
    {
        super.initialize(aContext);
        
        posMappingProvider = createPosMappingProvider(posMappingLocation, posTagset, getLanguage());
    }
    
    @Override
    public void getNext(JCas aJCas)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    CompressionUtils.getInputStream(res.getLocation(), res.getInputStream()),
                    sourceEncoding));
            convert(aJCas, reader);
        }
        finally {
            closeQuietly(reader);
        }
    }

    public void convert(JCas aJCas, BufferedReader aReader)
        throws IOException
    {
        if (readPos) {
            try {
                posMappingProvider.configure(aJCas.getCas());
            }
            catch (AnalysisEngineProcessException e) {
                throw new IOException(e);
            }
        }
        
        JCasBuilder doc = new JCasBuilder(aJCas);

        while (true) {
            // Read sentence comments (if any)
            Map<String, String> comments = readSentenceComments(aReader);
            
            // Read sentence
            List<String[]> words = readSentence(aReader);
            if (words == null) {
                // End of file
                break;
            }
            
            if (words.isEmpty()) {
                 // Ignore empty sentences. This can happen when there are multiple end-of-sentence
                 // markers following each other.
                continue; 
            }

            int sentenceBegin = doc.getPosition();
            int sentenceEnd = sentenceBegin;

            int surfaceBegin = -1;
            int surfaceEnd = -1;
            String surfaceString = null;
            
            // Tokens, Lemma, POS
            Int2ObjectMap<Token> tokens = new Int2ObjectOpenHashMap<>();
            Iterator<String[]> wordIterator = words.iterator();
            while (wordIterator.hasNext()) {
                String[] word = wordIterator.next();
                if (word[ID].contains("-")) {
                    String[] fragments = word[ID].split("-");
                    surfaceBegin = Integer.valueOf(fragments[0]);
                    surfaceEnd = Integer.valueOf(fragments[1]);
                    surfaceString = word[FORM];
                    continue;
                }
                
                // Read token
                int tokenIdx = Integer.valueOf(word[ID]);
                Token token = doc.add(word[FORM], Token.class);
                tokens.put(tokenIdx, token);
                if (!StringUtils.contains(word[MISC], "SpaceAfter=No") && wordIterator.hasNext()) {
                    doc.add(" ");
                }

                // Read lemma
                if (!UNUSED.equals(word[LEMMA]) && readLemma) {
                    Lemma lemma = new Lemma(aJCas, token.getBegin(), token.getEnd());
                    lemma.setValue(word[LEMMA]);
                    lemma.addToIndexes();
                    token.setLemma(lemma);
                }

                // Read part-of-speech tag
                POS pos = null;
                String tag = useCPosAsPos ? word[CPOSTAG] : word[POSTAG];
                if (!UNUSED.equals(tag) && readPos) {
                    Type posTag = posMappingProvider.getTagType(tag);
                    pos = (POS) aJCas.getCas().createAnnotation(posTag, token.getBegin(),
                            token.getEnd());
                    pos.setPosValue(tag != null ? tag.intern() : null);
                }

                // Read coarse part-of-speech tag
                if (!UNUSED.equals(word[CPOSTAG]) && readCPos) {
                    if (pos == null) {
                        pos = new POS(aJCas, token.getBegin(), token.getEnd());
                    }
                    pos.setCoarseValue(word[CPOSTAG].intern());
                }
                
                if (pos != null) {
                    pos.addToIndexes();
                    token.setPos(pos);
                }

                // Read morphological features
                if (!UNUSED.equals(word[FEATS]) && readMorph) {
                    MorphologicalFeatures morphtag = new MorphologicalFeatures(aJCas,
                            token.getBegin(), token.getEnd());
                    morphtag.setValue(word[FEATS]);
                    morphtag.addToIndexes();
                    token.setMorph(morphtag);
                    
                    // Try parsing out individual feature values. Since the DKPro Core
                    // MorphologicalFeatures type is based on the definition from the UD project,
                    // we can do this rather straightforwardly.
                    Type morphType = morphtag.getType();
                    String[] items = word[FEATS].split("\\|");
                    for (String item : items) {
                        String[] keyValue = item.split("=");
                        StringBuilder key = new StringBuilder(keyValue[0]);
                        key.setCharAt(0, Character.toLowerCase(key.charAt(0)));
                        String value = keyValue[1];
                        
                        Feature feat = morphType.getFeatureByBaseName(key.toString());
                        if (feat != null) {
                            morphtag.setStringValue(feat, value);
                        }
                    }
                }

                // Read surface form
                if (tokenIdx == surfaceEnd) {
                    int begin = tokens.get(surfaceBegin).getBegin();
                    int end = tokens.get(surfaceEnd).getEnd();
                    SurfaceForm surfaceForm = new SurfaceForm(aJCas, begin, end);
                    surfaceForm.setValue(surfaceString);
                    surfaceForm.addToIndexes();
                    surfaceBegin = -1;
                    surfaceEnd = -1;
                    surfaceString = null;
                }
                
                sentenceEnd = token.getEnd();
            }

            // Dependencies
            if (readDependency) {
                for (String[] word : words) {
                    if (!UNUSED.equals(word[DEPREL])) {
                        int depId = Integer.valueOf(word[ID]);
                        int govId = Integer.valueOf(word[HEAD]);
    
                        // Model the root as a loop onto itself
                        makeDependency(aJCas, govId, depId, word[DEPREL], DependencyFlavor.BASIC,
                                tokens, word);
                    }
                    
                    if (!UNUSED.equals(word[DEPS])) {
                        // list items separated by vertical bar
                        String[] items = word[DEPS].split("\\|");
                        for (String item : items) {
                            String[] sItem = item.split(":");
                            
                            int depId = Integer.valueOf(word[ID]);
                            int govId = Integer.valueOf(sItem[0]);

                            makeDependency(aJCas, govId, depId, sItem[1], DependencyFlavor.ENHANCED,
                                    tokens, word);
                        }
                    }
                }
            }

            // Sentence
            Sentence sentence = new Sentence(aJCas, sentenceBegin, sentenceEnd);
            sentence.setId(comments.get(META_SEND_ID));
            sentence.addToIndexes();

            // Once sentence per line.
            doc.add("\n");
        }

        doc.close();
    }
    
    private Dependency makeDependency(JCas aJCas, int govId, int depId, String label, String flavor,
            Int2ObjectMap<Token> tokens, String[] word)
    {
        Dependency rel;

        if (govId == 0) {
            rel = new ROOT(aJCas);
            rel.setGovernor(tokens.get(depId));
            rel.setDependent(tokens.get(depId));
        }
        else {
            rel = new Dependency(aJCas);
            rel.setGovernor(tokens.get(govId));
            rel.setDependent(tokens.get(depId));
        }

        rel.setDependencyType(label);
        rel.setFlavor(flavor);
        rel.setBegin(rel.getDependent().getBegin());
        rel.setEnd(rel.getDependent().getEnd());
        rel.addToIndexes();

        return rel;
    }

    private Map<String, String> readSentenceComments(BufferedReader aReader)
        throws IOException
    {
        Map<String, String> comments = new LinkedHashMap<>();
        
        while (true) {
            // Check if the next line could be a header line
            aReader.mark(2);
            char character = (char) aReader.read();
            if ('#' == character) {
                // Read the rest of the line
                String line = aReader.readLine();
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    comments.put(parts[0].trim(), parts[1].trim());
                }
                else {
                    // Comment or unknown header line
                }
            }
            else {
                aReader.reset();
                break;
            }
        }
        
        return comments;
    }
    
    /**
     * Read a single sentence.
     */
    private static List<String[]> readSentence(BufferedReader aReader)
        throws IOException
    {
        List<String[]> words = new ArrayList<>();
        String line;
        while ((line = aReader.readLine()) != null) {
            if (StringUtils.isBlank(line)) {
                break; // End of sentence
            }
            if (line.startsWith("#")) {
                // Comment line
                continue;
            }
            String[] fields = line.split("\t");
            if (fields.length != 10) {
                throw new IOException(
                        "Invalid file format. Line needs to have 10 tab-separated fields, but it has "
                                + fields.length + ": [" + line + "]");
            }
            words.add(fields);
        }

        if (line == null && words.isEmpty()) {
            return null;
        }
        else {
            return words;
        }
    }
}
