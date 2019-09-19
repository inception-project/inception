/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.inception.search.FeatureIndexingSupport.SPECIAL_SEP;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUtils.charsToBytes;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUtils.encodeFSAddress;
import static org.apache.commons.io.IOUtils.toCharArray;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectAll;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.xml.sax.SAXException;

import com.github.openjson.JSONObject;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupport;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistry;
import mtas.analysis.parser.MtasParser;
import mtas.analysis.token.MtasToken;
import mtas.analysis.token.MtasTokenCollection;
import mtas.analysis.token.MtasTokenString;
import mtas.analysis.util.MtasConfigException;
import mtas.analysis.util.MtasConfiguration;
import mtas.analysis.util.MtasParserException;
import mtas.analysis.util.MtasTokenizerFactory;

public class MtasUimaParser
    extends MtasParser
{
    /** 
     * Lucene can only index terms of a size of up to 32k characters - so we filter out
     * very long annotations to avoid getting exceptions from Lucene later on. This constant
     * determines what we consider as an oversized annotation that should be filtered.
     */
    private static final int OVERSIZED_ANNOTATION_LIMIT = 30000;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String MTAS_TOKEN_LABEL = "Token";
    public static final String MTAS_SENTENCE_LABEL = "s";
    
    private static final String SPECIAL_ATTR_REL_SOURCE = "source";
    private static final String SPECIAL_ATTR_REL_TARGET = "target";
    
    // Annotation schema and project services with knowledge base service
    private @Autowired AnnotationSchemaService annotationSchemaService;
    private @Autowired ProjectService projectService;
    private @Autowired FeatureIndexingSupportRegistry featureIndexingSupportRegistry;
    
    // Project id
    private final Project project;

    // Annotation layers being indexed by Mtas
    private Map<String, AnnotationLayer> layers;
    private Map<String, List<AnnotationFeature>> layerFeatures;

    private NavigableMap<Integer, Pair<AnnotationFS, Integer>> tokenBeginIndex;
    private NavigableMap<Integer, Pair<AnnotationFS, Integer>> tokenEndIndex;

    public MtasUimaParser(MtasConfiguration config)
    {
        super(config);
        
        // Perform dependency injection
        AutowireCapableBeanFactory factory = ApplicationContextProvider.getApplicationContext()
                .getAutowireCapableBeanFactory();
        factory.autowireBean(this);
        factory.initializeBean(this, "transientParser");
        
        // Process configuration
        // Read parser argument that contains the projectId
        JSONObject jsonParserConfiguration = new JSONObject(
                config.attributes.get(MtasTokenizerFactory.ARGUMENT_PARSER_ARGS));
        project = projectService.getProject(jsonParserConfiguration.getInt("projectId"));
        
        // Initialize and populate the hash maps for the layers and features
        initLayerAndFeatureCache();
    }
    
    // This constructor is used for testing
    public MtasUimaParser(Project aProject, AnnotationSchemaService aAnnotationSchemaService,
            FeatureIndexingSupportRegistry aFeatureIndexingSupportRegistry)
    {
        super(null);
        
        projectService = null;
        project = aProject;
        annotationSchemaService = aAnnotationSchemaService;
        featureIndexingSupportRegistry = aFeatureIndexingSupportRegistry;
        
        // Initialize and populate the hash maps for the layers and features
        initLayerAndFeatureCache();
    }

    private void initLayerAndFeatureCache()
    {
        // Initialize and populate the hash maps for the layers and features
        layers = new HashMap<String, AnnotationLayer>();
        layerFeatures = new HashMap<String, List<AnnotationFeature>>();
        for (AnnotationLayer layer : annotationSchemaService.listAnnotationLayer(project)) {
            if (layer.isEnabled()) {
                layers.put(layer.getName(), layer);
                List<AnnotationFeature> features = new ArrayList<AnnotationFeature>();
                for (AnnotationFeature feature : annotationSchemaService
                        .listAnnotationFeature(layer)) {
                    features.add(feature);
                }
                layerFeatures.put(layer.getName(), features);
            }
        }
    }
    
    @Override
    public MtasTokenCollection createTokenCollection(Reader aReader)
        throws MtasParserException, MtasConfigException
    {
        long start = System.currentTimeMillis();
        log.debug("Starting creation of token collection");

        CAS cas;
        try {
            cas = readCas(aReader);
        }
        catch (Exception e) {
            log.error("Unable to decode CAS", e);
            return new MtasTokenCollection();
        }

        try {
            createTokenCollection(cas);
            log.debug("Created token collection in {} ms", (System.currentTimeMillis() - start));
            return tokenCollection;
        }
        catch (Exception e) {
            log.error("Unable to create token collection", e);
            return new MtasTokenCollection();
        }
    }
    
    private CAS readCas(Reader aReader) throws UIMAException, IOException, SAXException
    {
        CAS cas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);

        try (InputStream in = new ByteArrayInputStream(charsToBytes(toCharArray(aReader)))) {
            CasIOUtils.load(in, cas);
        }
        
        return cas;
    }

    public MtasTokenCollection createTokenCollection(CAS aJCas)
    {
        // Initialize state
        tokenCollection = new MtasTokenCollection();
        int mtasId = 0;
        int tokenNum = 0;
        
        // Build indexes over the token start and end positions such that we can quickly locate
        // tokens based on their offsets.
        tokenBeginIndex = new TreeMap<>();
        tokenEndIndex = new TreeMap<>();
        for (AnnotationFS token : select(aJCas, getType(aJCas, Token.class))) {
            tokenBeginIndex.put(token.getBegin(), Pair.of(token, tokenNum));
            tokenEndIndex.put(token.getEnd(), Pair.of(token, tokenNum));
            tokenNum++;
        }
        
        // Loop over the annotations
        for (AnnotationFS annotation : selectAll(aJCas)) {
            // MTAS cannot index zero-width annotations, so we skip them here.
            if (annotation.getBegin() == annotation.getEnd()) {
                continue;
            }
            mtasId = indexAnnotation(tokenCollection, annotation, mtasId);
        }
        
        return tokenCollection;
    }
    
    private Range getRange(AnnotationFS aAnnotation)
    {
        // Get begin of the first token. Special cases:
        // 1) if the first token starts after the first char. For example, when there's
        // a space or line break in the beginning of the document.
        // 2) if the last token ends before the last char. Same as above.
        Pair<AnnotationFS, Integer> beginToken;
        if (tokenBeginIndex.floorEntry(aAnnotation.getBegin()) == null) {
            beginToken = tokenBeginIndex.firstEntry().getValue();
        }
        else {
            beginToken = tokenBeginIndex.floorEntry(aAnnotation.getBegin()).getValue();
        }
        
        Pair<AnnotationFS, Integer> endToken;
        if (tokenEndIndex.ceilingEntry(aAnnotation.getEnd()) == null) {
            endToken = tokenEndIndex.lastEntry().getValue();
        }
        else {
            endToken = tokenEndIndex.ceilingEntry(aAnnotation.getEnd()).getValue();
        }
        return new Range(beginToken.getValue(), endToken.getValue(), beginToken.getKey().getBegin(),
                endToken.getKey().getEnd());
    }
    
    private int indexAnnotation(MtasTokenCollection aTokenCollection, AnnotationFS aAnnotation,
            int aMtasId)
    {
        int mtasId = aMtasId;
        int fsAddress = getAddr(aAnnotation);
        if (aAnnotation.getEnd() - aAnnotation.getBegin() > OVERSIZED_ANNOTATION_LIMIT) {
            log.trace("Skipping indexing of very long annotation: {} {} characters at [{}-{}]",
                    aAnnotation.getType().getName(), aAnnotation.getEnd() - aAnnotation.getBegin(),
                    aAnnotation.getBegin(), aAnnotation.getEnd());
            
            return mtasId;
        }
        
        // Special case: token values must be indexed
        if (aAnnotation instanceof Token) {
            indexTokenText(aAnnotation, getRange(aAnnotation), mtasId++);
        } 
        // Special case: sentences must be indexed
        else if (aAnnotation instanceof Sentence) {
            indexSentenceText(aAnnotation, getRange(aAnnotation), mtasId++);
        }
        else {
            AnnotationLayer layer = layers.get(aAnnotation.getType().getName());
            
            // If the layer is not in the layers index, then it is not enabled.
            if (layer == null) {
                return mtasId;
            }
            
            if (WebAnnoConst.RELATION_TYPE.equals(layer.getType())) {
                RelationAdapter adapter = (RelationAdapter) annotationSchemaService
                        .getAdapter(layer);

                AnnotationFS sourceFs = FSUtil.getFeature(aAnnotation,
                        adapter.getSourceFeatureName(), AnnotationFS.class);

                AnnotationFS targetFs = FSUtil.getFeature(aAnnotation,
                        adapter.getTargetFeatureName(), AnnotationFS.class);

                if (
                        sourceFs != null && targetFs != null && 
                        // MTAS cannot index zero-width annotations, so we skip them here.
                        sourceFs.getBegin() != sourceFs.getEnd() &&
                        targetFs.getBegin() != targetFs.getEnd()
                ) {
                    // If the relation layer uses an attach-feature, index the annotation
                    // referenced by that feature
                    if (layer.getAttachFeature() != null) {
                        sourceFs = FSUtil.getFeature(sourceFs, layer.getAttachFeature().getName(),
                                AnnotationFS.class);
                        targetFs = FSUtil.getFeature(targetFs, layer.getAttachFeature().getName(),
                                AnnotationFS.class);
                    }

                    Range range = getRange(targetFs);
                    
                    // Index the source annotation text (equals the target)
                    indexAnnotationText(layer.getUiName(), targetFs.getCoveredText(), range,
                            mtasId++, fsAddress);
                    indexAnnotationText(layer.getUiName() + SPECIAL_SEP + SPECIAL_ATTR_REL_TARGET,
                            targetFs.getCoveredText(), range, mtasId++, fsAddress);
                    
                    // Index the target annotation text
                    indexAnnotationText(layer.getUiName() + SPECIAL_SEP + SPECIAL_ATTR_REL_SOURCE,
                            sourceFs.getCoveredText(), range, mtasId++, fsAddress);

                    // Index the relation features
                    mtasId = indexFeatures(aAnnotation, layer.getUiName(), range, mtasId,
                        fsAddress);

                    // Index the source features
                    mtasId = indexFeatures(sourceFs, layer.getUiName(),
                            SPECIAL_SEP + SPECIAL_ATTR_REL_SOURCE, range, mtasId, fsAddress);
                    
                    // Index the target features
                    mtasId = indexFeatures(targetFs, layer.getUiName(),
                            SPECIAL_SEP + SPECIAL_ATTR_REL_TARGET, range, mtasId, fsAddress);
                }
            }
            else {
                Range range = getRange(aAnnotation);
                
                // Index the annotation text
                indexAnnotationText(layer.getUiName(), aAnnotation.getCoveredText(), range,
                        mtasId++, fsAddress);
                
                // Iterate over the features of this layer and index them one-by-one
                mtasId = indexFeatures(aAnnotation, layer.getUiName(), range, mtasId, fsAddress);
            }
        }
        
        return mtasId;
    }

    private int indexFeatures(AnnotationFS aAnnotation, String aLayer, Range aRange, int aMtasId,
        int aFSAddress)
    {
        return indexFeatures(aAnnotation, aLayer, "", aRange, aMtasId, aFSAddress);
    }

    private int indexFeatures(AnnotationFS aAnnotation, String aLayer, String aPrefix, Range aRange,
            int aMtasId, int aFSAddress)
    {
        int mtasId = aMtasId;

        // Iterate over the features of this layer and index them one-by-one
        for (AnnotationFeature feature : layerFeatures.get(aAnnotation.getType().getName())) {
            Optional<FeatureIndexingSupport> fis = featureIndexingSupportRegistry
                    .getIndexingSupport(feature);
            if (fis.isPresent()) {
                MultiValuedMap<String, String> fieldsAndValues = fis.get()
                        .indexFeatureValue(aLayer, aAnnotation, aPrefix, feature);
                for (Entry<String, String> e : fieldsAndValues.entries()) {
                    indexFeatureValue(e.getKey(), e.getValue(), mtasId++,
                            aAnnotation.getBegin(), aAnnotation.getEnd(), aRange, aFSAddress);
                }
                
                log.trace("FEAT[{}-{}]: {}", aRange.getBegin(), aRange.getEnd(), fieldsAndValues);
            }
        }
        
        return mtasId;
    }

    private void indexTokenText(AnnotationFS aAnnotation, Range aRange, int aMtasId)
    {
        MtasToken mtasToken = new MtasTokenString(aMtasId, MTAS_TOKEN_LABEL,
                aAnnotation.getCoveredText(), aRange.getBegin());
        mtasToken.setOffset(aAnnotation.getBegin(), aAnnotation.getEnd());
        mtasToken.addPositionRange(aRange.getBegin(), aRange.getEnd());
        tokenCollection.add(mtasToken);
    }

    private void indexSentenceText(AnnotationFS aAnnotation, Range aRange, int aMtasId)
    {
        MtasToken mtasSentence = new MtasTokenString(aMtasId, MTAS_SENTENCE_LABEL,
                aAnnotation.getCoveredText(), aRange.getBegin());
        mtasSentence.setOffset(aAnnotation.getBegin(), aAnnotation.getEnd());
        mtasSentence.addPositionRange(aRange.getBegin(), aRange.getEnd());
        tokenCollection.add(mtasSentence);
    }

    private void indexAnnotationText(String aField, String aValue, Range aRange,
            int aMtasId, int aFSAddress)
    {
        String field = getIndexedName(aField);

        MtasToken mtasSentence = new MtasTokenString(aMtasId, field, aValue, aRange.getBegin());
        mtasSentence.setOffset(aRange.getBeginOffset(), aRange.getEndOffset());
        mtasSentence.addPositionRange(aRange.getBegin(), aRange.getEnd());
        // Store the FS address as payload so we can identify which MtasTokens were generated from
        // the same FS - this is not really meant to be used to look up the FS through the stored
        // address as the CAS may be out-of-sync with the index and thus the IDs may not match
        mtasSentence.setPayload(encodeFSAddress(aFSAddress));
        tokenCollection.add(mtasSentence);
        
        log.trace("TEXT[{}-{}]: {}={}", aRange.getBegin(), aRange.getEnd(), field, aValue);
    }

    private void indexFeatureValue(String aField, String aValue, int aMtasId, int aBeginOffset,
            int aEndOffset, Range aRange, int aFSAddress)
    {
        MtasToken mtasAnnotationTypeFeatureLabel = new MtasTokenString(aMtasId,
                getIndexedName(aField), aValue, aRange.getBegin());
        mtasAnnotationTypeFeatureLabel.setOffset(aRange.getBeginOffset(), aRange.getEndOffset());
        mtasAnnotationTypeFeatureLabel.addPositionRange(aRange.getBegin(), aRange.getEnd());
        // Store the FS address as payload so we can identify which MtasTokens were generated from
        // the same FS - this is not really meant to be used to look up the FS through the stored
        // address as the CAS may be out-of-sync with the index and thus the IDs may not match
        mtasAnnotationTypeFeatureLabel.setPayload(encodeFSAddress(aFSAddress));
        tokenCollection.add(mtasAnnotationTypeFeatureLabel);
    }
    
    /**
     * Replaces space with underscore in a {@code String}
     * @param uiName
     * @return String replacing the input string spaces with '_' 
     */
    public static String getIndexedName(String uiName)
    {
        String indexedName = uiName.replace(" ", "_");
        return indexedName;
    }

    @Override
    public String printConfig()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    private static class Range
    {
        private final int begin;
        private final int end;
        private final int beginOffset;
        private final int endOffset;

        public Range(int aBegin, int aEnd, int aBeginOffset, int aEndOffset)
        {
            super();
            begin = aBegin;
            end = aEnd;
            beginOffset = aBeginOffset;
            endOffset = aEndOffset;
        }

        public int getBegin()
        {
            return begin;
        }

        public int getEnd()
        {
            return end;
        }
        
        public int getBeginOffset()
        {
            return beginOffset;
        }
        
        public int getEndOffset()
        {
            return endOffset;
        }
    }
}
