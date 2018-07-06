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

import static java.util.Arrays.asList;

import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.wicket.ajax.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
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
    private final Logger log = LoggerFactory.getLogger(getClass());

    // Annotation layers being indexed by Mtas
    private Map<String, AnnotationLayer> layers;
    private Map<String, ArrayList<AnnotationFeature>> layerFeatures;

    // Annotation schema and project services with knowledge base service
    private AnnotationSchemaService annotationSchemaService;
    private ProjectService projectService;
    private KnowledgeBaseService kbService;
    private FeatureSupportRegistry featureSupportRegistry;
    
    // Project id
    Project project;

    final private String MTAS_SENTENCE_LABEL = "s";

    public MtasUimaParser(MtasConfiguration config)
    {
        super(config);
        annotationSchemaService = ApplicationContextProvider.getApplicationContext()
                .getBean(AnnotationSchemaService.class);
        projectService = ApplicationContextProvider.getApplicationContext()
                .getBean(ProjectService.class);
        kbService = ApplicationContextProvider.getApplicationContext()
                .getBean(KnowledgeBaseService.class);
        featureSupportRegistry = ApplicationContextProvider.getApplicationContext()
                .getBean(FeatureSupportRegistry.class);
        if (config.attributes.get(MtasTokenizerFactory.ARGUMENT_PARSER_ARGS) != null) {
            // Read parser argument that contains the projectId
            JSONObject jsonParserConfiguration = new JSONObject(
                    config.attributes.get(MtasTokenizerFactory.ARGUMENT_PARSER_ARGS));
            project = projectService.getProject(jsonParserConfiguration.getInt("projectId"));
            // Initialize and populate the hash maps for the layers and features
            layers = new HashMap<String, AnnotationLayer>();
            layerFeatures = new HashMap<String, ArrayList<AnnotationFeature>>();
            for (AnnotationLayer layer : annotationSchemaService.listAnnotationLayer(project)) {
                if (layer.isEnabled()) {
                    layers.put(layer.getName(), layer);
                    ArrayList<AnnotationFeature> features = new ArrayList<AnnotationFeature>();
                    for (AnnotationFeature feature : annotationSchemaService
                            .listAnnotationFeature(layer)) {
                        features.add(feature);
                    }
                    layerFeatures.put(layer.getName(), features);
                }
            }
        }
    }

    @Override
    public MtasTokenCollection createTokenCollection(Reader reader)
        throws MtasParserException, MtasConfigException
    {
        long start = System.currentTimeMillis();

        tokenCollection = new MtasTokenCollection();
        if (project == null) {
            return tokenCollection;
        }
        try {
            TypeSystemDescription builtInTypes = TypeSystemDescriptionFactory
                    .createTypeSystemDescription();
            TypeSystemDescription projectTypes = annotationSchemaService.getProjectTypes(project);
            TypeSystemDescription allTypes = CasCreationUtils
                    .mergeTypeSystems(asList(projectTypes, builtInTypes));
            JCas jcas = JCasFactory.createJCas(allTypes);
            String xmi = IOUtils.toString(reader);
            // Get the annotations from the XMI are back in the CAS.
            XmiCasDeserializer.deserialize(new ByteArrayInputStream(xmi.getBytes()), jcas.getCas());
            Set<Annotation> processed = new HashSet<>();
            int mtasId = 0;
            int tokenNum = 0;
            // Build indexes over the token start and end positions such that we can quickly locate
            // tokens based on their offsets.
            NavigableMap<Integer, Integer> tokenBeginIndex = new TreeMap<>();
            NavigableMap<Integer, Integer> tokenEndIndex = new TreeMap<>();
            for (Token token : JCasUtil.select(jcas, Token.class)) {
                tokenBeginIndex.put(token.getBegin(), tokenNum);
                tokenEndIndex.put(token.getEnd(), tokenNum);
                tokenNum++;
            }
            // Loop over the annotations
            for (Annotation annotation : JCasUtil.select(jcas, Annotation.class)) {
                if (processed.contains(annotation)) {
                    continue;
                }
                String annotationName = annotation.getType().getName();
                String annotationUiName = layers.containsKey(annotationName)
                        ? layers.get(annotationName).getUiName()
                        : "";
                // Get begin of the first token. Special cases:
                // 1) if the first token starts after the first char. For example, when there's
                // a space or line break in the beginning of the document.
                // 2) if the last token ends before the last char. Same as above.
                int beginToken = 0;
                if (tokenBeginIndex.floorEntry(annotation.getBegin()) == null) {
                    beginToken = tokenBeginIndex.firstEntry().getValue();
                }
                else {
                    beginToken = tokenBeginIndex.floorEntry(annotation.getBegin()).getValue();
                }
                int endToken = 0;
                if (tokenEndIndex.ceilingEntry(annotation.getEnd() - 1) == null) {
                    endToken = tokenEndIndex.lastEntry().getValue();
                }
                else {
                    endToken = tokenEndIndex.ceilingEntry(annotation.getEnd() - 1).getValue();
                }
                // Special case: token values must be indexed
                if (annotation instanceof Token) {
                    MtasToken mtasToken = new MtasTokenString(mtasId++,
                            annotationUiName + MtasToken.DELIMITER + annotation.getCoveredText(),
                            beginToken);
                    mtasToken.setOffset(annotation.getBegin(), annotation.getEnd());
                    mtasToken.addPositionRange(beginToken, endToken);
                    tokenCollection.add(mtasToken);
                } // Special case: sentences must be indexed
                else if (annotation instanceof Sentence) {
                    MtasToken mtasSentence = new MtasTokenString(mtasId++,
                            MTAS_SENTENCE_LABEL + MtasToken.DELIMITER + annotation.getCoveredText(),
                            beginToken);
                    mtasSentence.setOffset(annotation.getBegin(), annotation.getEnd());
                    mtasSentence.addPositionRange(beginToken, endToken);
                    tokenCollection.add(mtasSentence);
                }
                else {
                    // Other annotation types - annotate the features
                    if (layers.get(annotationName) != null) {
                        // Add the UI annotation name to the index as an annotation.
                        // Replace spaces with underscore in the UI name.
                        MtasToken mtasAnnotation = new MtasTokenString(mtasId++,
                                annotationUiName.replace(" ", "_") + MtasToken.DELIMITER,
                                beginToken);
                        mtasAnnotation.setOffset(annotation.getBegin(), annotation.getEnd());
                        mtasAnnotation.addPositionRange(beginToken, endToken);
                        tokenCollection.add(mtasAnnotation);

                        // Get features for this annotation, if it is indexed. First comes the
                        // internal feature name, then the UI feature name
                        for (AnnotationFeature feature : layerFeatures.get(annotationName)) {
                            // Test if the internal feature is a primitive feature
                            if (!WebAnnoCasUtil.isPrimitiveFeature(annotation, feature.getName())) {
                                continue;
                            }
                            mtasId = indexFeatureValue(tokenCollection, feature, annotation,
                                    beginToken, endToken, mtasId, annotationUiName);

                            if (feature.getType().startsWith("kb:")) {
                                mtasId = indexConcept(tokenCollection, feature, annotation,
                                        beginToken, endToken, mtasId, annotationUiName);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("Unable to index document", e);
        }

        log.debug("Created token collection in {}ms", (System.currentTimeMillis() - start));

        return tokenCollection;
    }

    private int indexFeatureValue(MtasTokenCollection aTokenCollection, AnnotationFeature aFeature,
            AnnotationFS aAnnotation, int aBeginToken, int aEndToken, int aMtasId,
            String annotationUiName)
    {
        int mtasId = aMtasId;
        
        String featureValue = WebAnnoCasUtil.getFeature(aAnnotation, aFeature.getName());

        // Add the UI annotation.feature name to the index as an annotation.
        // Replace spaces with underscore in the UI name.
        addToIndex(aTokenCollection, annotationUiName + "." + aFeature.getUiName(), featureValue,
                mtasId++, aAnnotation.getBegin(), aAnnotation.getEnd(), aBeginToken, aEndToken);
        
        return mtasId;
    }
    
    private int indexConcept(MtasTokenCollection aTokenCollection, AnnotationFeature aFeature,
            AnnotationFS aAnnotation, int aBeginToken, int aEndToken, int aMtasId,
            String aAnnotationUiName)
    {
        int mtasId = aMtasId;
        
        // Is it a concept feature?
        // ConceptFeatureSupport.PREFIX
        if (!aFeature.getType().startsWith("kb:")) {
            return mtasId;
        }
        
        // Returns KB IRI label after checking if the
        // feature type is associated with KB and feature value is not null
        FeatureSupport<?> featSup = featureSupportRegistry.getFeatureSupport(aFeature);
        KBHandle featureObject = featSup.getFeatureValue(aFeature, aAnnotation);
        
        // Feature value is not set
        if (featureObject == null) {
            return mtasId;
        }

        // === BEGIN NEEDS REFACTORING =====================================================
        // See comment below.
        Optional<KBObject> kbObject = KBUtility.readKBIdentifier(kbService, project,
                    WebAnnoCasUtil.getFeature(aAnnotation, aFeature.getName()));
        // === END NEEDS REFACTORING =======================================================

        if (kbObject.isPresent()) {
            String objectType;

            // === BEGIN NEEDS REFACTORING =====================================================
            // As part of issue #244, this needs to be refactored for a more reliable method of
            // detecting whether an IRI refers to a class or to an instance.
            // 
            if (kbObject.get() instanceof KBConcept) {
                objectType = IndexingConstants.INDEX_KB_CONCEPT;
            }
            else if (kbObject.get() instanceof KBInstance) {
                objectType = IndexingConstants.INDEX_KB_INSTANCE;
            }
            else {
                throw new IllegalStateException("Unknown KB object: [" + kbObject.get() + "]");
            }

            // Indexing UI label with type i.e Concept/Instance
            addToIndex(aTokenCollection,
                    aAnnotationUiName + "." + aFeature.getUiName() + "." + objectType,
                    featureObject.getUiLabel(), mtasId++, aAnnotation.getBegin(),
                    aAnnotation.getEnd(), aBeginToken, aEndToken);
            // === END NEEDS REFACTORING =======================================================

            // Indexing <feature>=<UI label>
            addToIndex(aTokenCollection, aAnnotationUiName + "." + aFeature.getUiName(),
                    featureObject.getUiLabel(), mtasId++, aAnnotation.getBegin(),
                    aAnnotation.getEnd(), aBeginToken, aEndToken);

            // Indexing <feature>=<URI>
            addToIndex(aTokenCollection, aAnnotationUiName + "." + aFeature.getUiName(),
                    kbObject.get().getIdentifier(), mtasId++, aAnnotation.getBegin(),
                    aAnnotation.getEnd(), aBeginToken, aEndToken);

            // Indexing UI label without type and layer for generic search
            addToIndex(aTokenCollection, IndexingConstants.KB_ENTITY, featureObject.getUiLabel(),
                    mtasId++, aAnnotation.getBegin(), aAnnotation.getEnd(), aBeginToken, aEndToken);
        }
        
        return mtasId;
    }
    
    private void addToIndex(MtasTokenCollection aTokenCollection, String aField, String aValue,
            int aMtasId, int aBeginOffset, int aEndOffset, int aBeginPosition, int aEndPosition)
    {
        String indexedStr = getIndexedName(aField) + MtasToken.DELIMITER + aValue;
        log.debug("Indexed String with type for : {}", indexedStr);
        MtasToken mtasAnnotationTypeFeatureLabel = new MtasTokenString(aMtasId++, indexedStr,
                aBeginPosition);
        mtasAnnotationTypeFeatureLabel.setOffset(aBeginOffset, aEndOffset);
        mtasAnnotationTypeFeatureLabel.addPositionRange(aBeginPosition, aEndPosition);
        aTokenCollection.add(mtasAnnotationTypeFeatureLabel);
    }
    
    /**
     * Replaces space with underscore in a {@code String}
     * @param uiName
     * @return String replacing the input string spaces with '_' 
     */
    public String getIndexedName(String uiName)
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
}
