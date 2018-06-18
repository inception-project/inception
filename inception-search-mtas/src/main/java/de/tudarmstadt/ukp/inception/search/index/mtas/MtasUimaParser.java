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
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.impl.XmiCasDeserializer;
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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import mtas.analysis.parser.MtasParser;
import mtas.analysis.token.MtasToken;
import mtas.analysis.token.MtasTokenCollection;
import mtas.analysis.token.MtasTokenString;
import mtas.analysis.util.MtasConfigException;
import mtas.analysis.util.MtasConfiguration;
import mtas.analysis.util.MtasParserException;
import mtas.analysis.util.MtasTokenizerFactory;

public class MtasUimaParser extends MtasParser {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Annotation layers being indexed by Mtas
    private HashMap<String, AnnotationLayer> layers;
    private HashMap<String, ArrayList<AnnotationFeature>> layerFeatures;

    // Annotation schema and project services with knowledge base service
    private AnnotationSchemaService annotationSchemaService;
    private ProjectService projectService;
    private KnowledgeBaseService kbService;
    
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
        MtasTokenCollection tokenCollection = new MtasTokenCollection();
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
                } else {
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
                            String featureValue = "";
                            // Test if the internal feature name is a primitive feature
                            if (WebAnnoCasUtil.isPrimitiveFeature(annotation, feature.getName())) {
                                // Get the feature value using the internal name.
                                // Cast to Object so that the proper valueOf signature is used by
                                // the compiler, otherwise it will think that a String argument is
                                // char[].
                                featureValue = String.valueOf((Object) WebAnnoCasUtil
                                        .getFeature(annotation, feature.getName()));
                                
                                // Add the UI annotation.feature name to the index as an annotation.
                                // Replace spaces with underscore in the UI name.
                                MtasToken mtasAnnotationFeature = new MtasTokenString(mtasId++,
                                        getIndexedName(annotationUiName) + "."
                                        + getIndexedName(feature.getUiName()) 
                                        + MtasToken.DELIMITER + featureValue, beginToken);
                                mtasAnnotationFeature.setOffset(annotation.getBegin(),
                                        annotation.getEnd());
                                mtasAnnotationFeature.addPositionRange(beginToken, endToken);
                                tokenCollection.add(mtasAnnotationFeature);
                                // Returns KB IRI label after checking if the  
                                // feature type is associated with KB and feature value is not null
                                String labelStr = "";
                                if (feature.getType().contains(IndexingConstants.KB)
                                        && (!featureValue.equals("null"))) {
                                    labelStr = getUILabel(featureValue);
                                }
                                if (!labelStr.isEmpty()) {
                                    String kbType = labelStr.split(MtasToken.DELIMITER)[0];
                                    String kbValue = labelStr.split(MtasToken.DELIMITER)[1];
                                    if (kbType.equals(IndexingConstants.kbConcept)) {
                                        kbType = IndexingConstants.indexKBConcept;
                                    }
                                    else if (kbType.equals(IndexingConstants.kbInstance)) {
                                        kbType = IndexingConstants.indexKBInstance;
                                    }
                                    // Index IRI feature value with their labels along with 
                                    // annotation.feature name  
                                    String indexedStr = getIndexedName(annotationUiName) + "."
                                            + getIndexedName(feature.getUiName()) + "."
                                            + kbType + MtasToken.DELIMITER + kbValue;

                                    // Indexing UI annotation with type i.e Concept/Instance
                                    log.debug("Indexed String with type for : {}", indexedStr);
                                    MtasToken mtasAnnotationTypeFeatureLabel = new MtasTokenString(
                                            mtasId++, indexedStr, beginToken);
                                    mtasAnnotationTypeFeatureLabel.setOffset(annotation.getBegin(),
                                            annotation.getEnd());
                                    mtasAnnotationTypeFeatureLabel.addPositionRange(beginToken,
                                            endToken);
                                    tokenCollection.add(mtasAnnotationTypeFeatureLabel);
                                    indexedStr = getIndexedName(annotationUiName) + "."
                                            + getIndexedName(feature.getUiName())
                                            + MtasToken.DELIMITER + kbValue;
                                    
                                    // Indexing UI annotation without type i.e Concept/Instance
                                    log.debug("Indexed String without type for : {}", indexedStr);
                                    MtasToken mtasAnnotationFeatureLabel = new MtasTokenString(
                                            mtasId++, indexedStr, beginToken);
                                    mtasAnnotationFeatureLabel.setOffset(annotation.getBegin(),
                                            annotation.getEnd());
                                    mtasAnnotationFeatureLabel.addPositionRange(beginToken,
                                            endToken);
                                    tokenCollection.add(mtasAnnotationFeatureLabel);
                                    
                                    // Indexing UI annotation without type and layer for generic search
                                    indexedStr = IndexingConstants.kbEntity + MtasToken.DELIMITER + kbValue;
                                    log.debug("Indexed String without type and label for : {}", indexedStr);
                                    MtasToken mtasAnnotationKBEntity = new MtasTokenString(
                                            mtasId++, indexedStr, beginToken);
                                    mtasAnnotationKBEntity.setOffset(annotation.getBegin(),
                                            annotation.getEnd());
                                    mtasAnnotationKBEntity.addPositionRange(beginToken,
                                            endToken);
                                    tokenCollection.add(mtasAnnotationKBEntity);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unable to index document", e);
        }
        return tokenCollection;
    }

    /**
     * Replaces space with underscore n a {@code String}
     * @param {@code String} uiName
     * @return {@code String}
     */
    public String getIndexedName(String uiName)
    {
        String indexedName = uiName.replace(" ", "_");
        return indexedName;
    }

    @Override
    public String printConfig() {
        return null;
    }
    /**
     * 
     * Takes in {@code IRI} for identifier as {@code String} and returns the label String 
     * Eg:- InputParameter :- {@code http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#RoseDAnjou} 
     * Returned :- {@link KBConcept} + {@link MtasToken.DELIMITER} + RoseDAnjou
     * @param String iri 
     * @return String {@link KBObject}+{@link MtasToken.DELIMITER}
     *          +{@link KBObject.get().getUiLabel()}
     */
    public String getUILabel(String aIRI) {
        StringBuilder labelStr = new StringBuilder();
        System.out.println();
        Optional<KBObject> kbObject = KBUtility.readKBIdentifier(kbService,project, aIRI);
        if (kbObject.isPresent()) {
            labelStr.append(kbObject.get().getClass().getSimpleName())
            .append(MtasToken.DELIMITER).append(kbObject.get().getUiLabel());
        } else {
            return labelStr.toString();
        }
        return labelStr.toString();
    }
    
    /**
     * Method Implementation to get MtasToken (To be done when subclass and class 
     * semantics to be included
     * @return
     */
    public MtasToken getMtasTokenKBFeature() {
        return null;
    }

    class SimpleAnnotationLayer {
        private ArrayList<Pair<String, String>> features;
        private String LayerName;
        private String LayerUiName;
        private String LayerShorName;

        public ArrayList<Pair<String, String>> getFeatures() {
            return features;
        }

        public void setFeatures(ArrayList<Pair<String, String>> features) {
            this.features = features;
        }

        public String getLayerName() {
            return LayerName;
        }

        public void setLayerName(String layerName) {
            LayerName = layerName;
        }

        public String getLayerUiName() {
            return LayerUiName;
        }

        public void setLayerUiName(String layerUiName) {
            LayerUiName = layerUiName;
        }

        public String getLayerShorName() {
            return LayerShorName;
        }

        public void setLayerShorName(String layerShorName) {
            LayerShorName = layerShorName;
        }
    }
}
