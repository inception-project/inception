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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static de.tudarmstadt.ukp.inception.search.FeatureIndexingSupport.SPECIAL_SEP;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUtils.charsToBytes;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUtils.encodeFSAddress;
import static de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState.KEY_SEARCH_STATE;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;
import static java.lang.invoke.MethodHandles.lookup;
import static mtas.analysis.util.MtasTokenizerFactory.ARGUMENT_PARSER_ARGS;
import static org.apache.commons.io.IOUtils.toCharArray;
import static org.apache.commons.lang3.StringUtils.toRootLowerCase;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectAll;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.xml.sax.SAXException;

import com.github.openjson.JSONObject;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistry;
import de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState;
import de.tudarmstadt.ukp.inception.search.model.BulkIndexingContext;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import mtas.analysis.parser.MtasParser;
import mtas.analysis.token.MtasTokenCollection;
import mtas.analysis.token.MtasTokenString;
import mtas.analysis.util.MtasConfigException;
import mtas.analysis.util.MtasConfiguration;
import mtas.analysis.util.MtasParserException;

public class MtasUimaParser
    extends MtasParser
{
    /**
     * Using a static logger here because many instances of this class may be created during
     * indexing and we do not want to waste time in setting up a separate logger for every one.
     */
    private static final Logger LOG = getLogger(lookup().lookupClass());

    /**
     * Lucene can only index terms of a size of up to 32k characters - so we filter out very long
     * annotations to avoid getting exceptions from Lucene later on. This constant determines what
     * we consider as an oversized annotation that should be filtered.
     */
    private static final int OVERSIZED_ANNOTATION_LIMIT = 30000;

    public static final String PARAM_PROJECT_ID = "projectId";

    public static final String MTAS_TOKEN_LABEL = "Token";
    public static final String MTAS_SENTENCE_LABEL = "s";

    private static final String SPECIAL_ATTR_REL_SOURCE = "source";
    private static final String SPECIAL_ATTR_REL_TARGET = "target";

    // Annotation schema and project services with knowledge base service
    private @Autowired ProjectService projectService;
    private @Autowired PreferencesService preferencesService;
    private @Autowired AnnotationSchemaService annotationSchemaService;
    private @Autowired FeatureIndexingSupportRegistry featureIndexingSupportRegistry;

    private final Map<String, AnnotationLayer> layers = new HashMap<>();
    private final Map<String, List<AnnotationFeature>> layerFeatures = new HashMap<>();

    private NavigableMap<Integer, Pair<AnnotationFS, Integer>> tokenBeginIndex;
    private NavigableMap<Integer, Pair<AnnotationFS, Integer>> tokenEndIndex;

    private AnnotationSearchState prefs;

    public MtasUimaParser(MtasConfiguration config)
    {
        super(config);

        try {
            // Perform dependency injection
            AutowireCapableBeanFactory factory = ApplicationContextProvider.getApplicationContext()
                    .getAutowireCapableBeanFactory();
            factory.autowireBean(this);
            factory.initializeBean(this, "transientParser");

            JSONObject jsonParserConfiguration = new JSONObject(
                    config.attributes.get(ARGUMENT_PARSER_ARGS));

            Optional<BulkIndexingContext> maybeIndexingContext = BulkIndexingContext.get();
            if (maybeIndexingContext.isPresent()) {
                var indexingContext = maybeIndexingContext.get();

                indexingContext.getLayers().stream().forEach(layer -> {
                    layers.put(layer.getName(), layer);
                    layerFeatures.put(layer.getName(), new ArrayList<>());
                });

                indexingContext.getFeatures().stream().forEach(feature -> {
                    layerFeatures
                            .computeIfAbsent(feature.getLayer().getName(), key -> new ArrayList<>())
                            .add(feature);
                });

                prefs = indexingContext.getIndexingSettings();
            }
            else {
                Project project = projectService
                        .getProject(jsonParserConfiguration.getLong(PARAM_PROJECT_ID));

                // Initialize and populate the hash maps for the layers and features
                annotationSchemaService.listAnnotationLayer(project).stream()
                        .filter(layer -> layer.isEnabled()) //
                        .forEach(layer -> {
                            layers.put(layer.getName(), layer);
                            layerFeatures.put(layer.getName(), new ArrayList<>());
                        });

                annotationSchemaService.listSupportedFeatures(project).stream()
                        .filter(feature -> feature.isEnabled() && feature.getLayer().isEnabled())
                        .forEach(feature -> {
                            layerFeatures.computeIfAbsent(feature.getLayer().getName(),
                                    key -> new ArrayList<>()).add(feature);
                        });

                prefs = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE, project);
            }
        }
        catch (Exception e) {
            LOG.error("Unable to initialize MtasUimaParser", e);
            throw e;
        }
    }

    // This constructor is used for testing
    public MtasUimaParser(List<AnnotationFeature> aFeaturesToIndex,
            AnnotationSchemaService aAnnotationSchemaService,
            FeatureIndexingSupportRegistry aFeatureIndexingSupportRegistry,
            AnnotationSearchState aPrefs)
    {
        super(null);

        annotationSchemaService = aAnnotationSchemaService;
        featureIndexingSupportRegistry = aFeatureIndexingSupportRegistry;
        prefs = aPrefs;

        // Initialize and populate the hash maps for the layers and features
        for (AnnotationFeature feature : aFeaturesToIndex) {
            layers.put(feature.getLayer().getName(), feature.getLayer());
            layerFeatures.computeIfAbsent(feature.getLayer().getName(), key -> new ArrayList<>())
                    .add(feature);
        }
    }

    @Override
    public MtasTokenCollection createTokenCollection(Reader aReader)
        throws MtasParserException, MtasConfigException
    {
        // try (CasStorageSession session = CasStorageSession.openNested(true)) {
        long start = System.currentTimeMillis();
        LOG.debug("Starting creation of token collection");

        CAS cas;
        try {
            cas = readCas(aReader);
        }
        catch (Exception e) {
            LOG.error("Unable to decode CAS", e);
            return new MtasTokenCollection();
        }

        try {
            createTokenCollection(cas);
            LOG.debug("Created token collection in {} ms", (System.currentTimeMillis() - start));
            return tokenCollection;
        }
        catch (Exception e) {
            LOG.error("Unable to create token collection", e);
            return new MtasTokenCollection();
        }
        // }
    }

    private CAS readCas(Reader aReader) throws UIMAException, IOException, SAXException
    {
        return getRealCas(WebAnnoCasUtil.byteArrayToCas(charsToBytes(toCharArray(aReader))));
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

        // MtasUtils.print(tokenCollection);

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
        int fsAddress = ICasUtil.getAddr(aAnnotation);

        int length = aAnnotation.getEnd() - aAnnotation.getBegin();

        if (length < 0) {
            LOG.warn(
                    "Skipping indexing of annotation with negative length: {} {} characters at [{}-{}]",
                    aAnnotation.getType().getName(), aAnnotation.getEnd() - aAnnotation.getBegin(),
                    aAnnotation.getBegin(), aAnnotation.getEnd());

            return mtasId;
        }

        if (length > OVERSIZED_ANNOTATION_LIMIT) {
            LOG.debug("Skipping indexing of very long annotation: {} {} characters at [{}-{}]",
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
            var layer = layers.get(aAnnotation.getType().getName());

            // If the layer is not in the layers index, then it is not enabled.
            if (layer == null) {
                return mtasId;
            }

            if (RelationLayerSupport.TYPE.equals(layer.getType())) {
                var adapter = (RelationAdapter) annotationSchemaService.getAdapter(layer);

                var sourceFs = FSUtil.getFeature(aAnnotation, adapter.getSourceFeatureName(),
                        AnnotationFS.class);

                var targetFs = FSUtil.getFeature(aAnnotation, adapter.getTargetFeatureName(),
                        AnnotationFS.class);

                // If the relation layer uses an attach-feature, index the annotation
                // referenced by that feature
                if (layer.getAttachFeature() != null) {
                    if (sourceFs != null) {
                        sourceFs = FSUtil.getFeature(sourceFs, layer.getAttachFeature().getName(),
                                AnnotationFS.class);
                    }

                    if (targetFs != null) {
                        targetFs = FSUtil.getFeature(targetFs, layer.getAttachFeature().getName(),
                                AnnotationFS.class);
                    }
                }

                if (sourceFs != null && targetFs != null &&
                // MTAS cannot index zero-width annotations, so we skip them here.
                        sourceFs.getBegin() != sourceFs.getEnd()
                        && targetFs.getBegin() != targetFs.getEnd()) {

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

        // If there are no features on the layer, do not attempt to index them
        var features = layerFeatures.get(aAnnotation.getType().getName());
        if (features == null) {
            return mtasId;
        }

        // Iterate over the features of this layer and index them one-by-one
        for (var feature : features) {
            var fis = featureIndexingSupportRegistry.getIndexingSupport(feature);
            if (fis.isPresent()) {
                MultiValuedMap<String, String> fieldsAndValues = fis.get().indexFeatureValue(aLayer,
                        aAnnotation, aPrefix, feature);
                for (var e : fieldsAndValues.entries()) {
                    indexFeatureValue(getIndexedName(e.getKey()), e.getValue(), mtasId++,
                            aAnnotation.getBegin(), aAnnotation.getEnd(), aRange, aFSAddress);
                }

                if (LOG.isTraceEnabled()) {
                    if (fieldsAndValues.isEmpty()) {
                        LOG.trace("FEAT[{}-{}]: [{}]: Nothing to index", aRange.getBegin(),
                                aRange.getEnd(), feature.getName());
                    }
                    else {
                        LOG.trace("FEAT[{}-{}]: [{}] = {}", aRange.getBegin(), aRange.getEnd(),
                                feature.getName(), fieldsAndValues);
                    }
                }
            }
        }

        return mtasId;
    }

    private void indexTokenText(AnnotationFS aAnnotation, Range aRange, int aMtasId)
    {
        var field = MTAS_TOKEN_LABEL;
        var value = aAnnotation.getCoveredText();
        value = prefs.isCaseSensitiveDocumentText() ? value : toRootLowerCase(value);
        var mt = new MtasTokenString(aMtasId, field, value, aRange.getBegin());
        mt.setOffset(aRange.getBeginOffset(), aRange.getEndOffset());
        mt.addPositionRange(aRange.getBegin(), aRange.getEnd());
        tokenCollection.add(mt);

        LOG.trace("TOKN[{}-{}]: {}={}", aRange.getBegin(), aRange.getEnd(), field, value);
    }

    private void indexSentenceText(AnnotationFS aAnnotation, Range aRange, int aMtasId)
    {
        var field = MTAS_SENTENCE_LABEL;
        var value = aAnnotation.getCoveredText();
        value = prefs.isCaseSensitiveDocumentText() ? value : toRootLowerCase(value);
        var mt = new MtasTokenString(aMtasId, field, value, aRange.getBegin());
        mt.setOffset(aRange.getBeginOffset(), aRange.getEndOffset());
        mt.addPositionRange(aRange.getBegin(), aRange.getEnd());
        tokenCollection.add(mt);

        LOG.trace("SENT[{}-{}]: {}={}", aRange.getBegin(), aRange.getEnd(), field, value);
    }

    private void indexAnnotationText(String aField, String aValue, Range aRange, int aMtasId,
            int aFSAddress)
    {
        var field = getIndexedName(aField);
        var value = prefs.isCaseSensitiveDocumentText() ? aValue : toRootLowerCase(aValue);
        var mt = new MtasTokenString(aMtasId, field, value, aRange.getBegin());
        mt.setOffset(aRange.getBeginOffset(), aRange.getEndOffset());
        mt.addPositionRange(aRange.getBegin(), aRange.getEnd());
        // Store the FS address as payload so we can identify which MtasTokens were generated from
        // the same FS - this is not really meant to be used to look up the FS through the stored
        // address as the CAS may be out-of-sync with the index and thus the IDs may not match
        mt.setPayload(encodeFSAddress(aFSAddress));
        tokenCollection.add(mt);

        LOG.trace("TEXT[{}-{}]: {}={}", aRange.getBegin(), aRange.getEnd(), field, aValue);
    }

    private void indexFeatureValue(String aField, String aValue, int aMtasId, int aBeginOffset,
            int aEndOffset, Range aRange, int aFSAddress)
    {
        var field = getIndexedName(aField);
        var value = prefs.isCaseSensitiveFeatureValues() ? aValue : toRootLowerCase(aValue);
        var mt = new MtasTokenString(aMtasId, field, value, aRange.getBegin());
        mt.setOffset(aRange.getBeginOffset(), aRange.getEndOffset());
        mt.addPositionRange(aRange.getBegin(), aRange.getEnd());
        // Store the FS address as payload so we can identify which MtasTokens were generated from
        // the same FS - this is not really meant to be used to look up the FS through the stored
        // address as the CAS may be out-of-sync with the index and thus the IDs may not match
        mt.setPayload(encodeFSAddress(aFSAddress));
        tokenCollection.add(mt);

        LOG.trace("FEAT[{}-{}]: {}={}", aRange.getBegin(), aRange.getEnd(), field, aValue);
    }

    /**
     * Replaces space with underscore in a {@code String}
     * 
     * @param uiName
     *            the UI name of the layer
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
