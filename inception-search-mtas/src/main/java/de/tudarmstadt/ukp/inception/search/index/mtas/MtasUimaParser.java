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
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
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
    private final Logger log = LoggerFactory.getLogger(getClass());

    final private String MTAS_TOKEN_LABEL = "Token";
    final private String MTAS_SENTENCE_LABEL = "s";
    
    // Annotation schema and project services with knowledge base service
    private @Autowired AnnotationSchemaService annotationSchemaService;
    private @Autowired ProjectService projectService;
    private @Autowired FeatureIndexingSupportRegistry featureIndexingSupportRegistry;
    
    // Project id
    private final Project project;

    // Annotation layers being indexed by Mtas
    private Map<String, AnnotationLayer> layers;
    private Map<String, List<AnnotationFeature>> layerFeatures;
    
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

        JCas jcas;
        try {
            jcas = readCas(aReader);
        }
        catch (Exception e) {
            log.error("Unable to decode CAS", e);
            return new MtasTokenCollection();
        }

        try {
            createTokenCollection(jcas);
            log.debug("Created token collection in {}ms", (System.currentTimeMillis() - start));
            return tokenCollection;
        }
        catch (Exception e) {
            log.error("Unable to create token collection", e);
            return new MtasTokenCollection();
        }
    }
    
    private JCas readCas(Reader reader) throws UIMAException, IOException, SAXException
    {
        TypeSystemDescription builtInTypes = TypeSystemDescriptionFactory
                .createTypeSystemDescription();
        TypeSystemDescription projectTypes = annotationSchemaService.getProjectTypes(project);
        TypeSystemDescription allTypes = CasCreationUtils
                .mergeTypeSystems(asList(projectTypes, builtInTypes));
        JCas jcas = JCasFactory.createJCas(allTypes);
        String xmi = IOUtils.toString(reader);
        // Get the annotations from the XMI are back in the CAS.
        XmiCasDeserializer.deserialize(new ByteArrayInputStream(xmi.getBytes()), jcas.getCas());

        return jcas;
    }
    
    public MtasTokenCollection createTokenCollection(JCas aJCas)
    {
        // Initialize state
        tokenCollection = new MtasTokenCollection();
        int mtasId = 0;
        int tokenNum = 0;
        
        // Build indexes over the token start and end positions such that we can quickly locate
        // tokens based on their offsets.
        NavigableMap<Integer, Integer> tokenBeginIndex = new TreeMap<>();
        NavigableMap<Integer, Integer> tokenEndIndex = new TreeMap<>();
        for (Token token : JCasUtil.select(aJCas, Token.class)) {
            tokenBeginIndex.put(token.getBegin(), tokenNum);
            tokenEndIndex.put(token.getEnd(), tokenNum);
            tokenNum++;
        }
        
        // Loop over the annotations
        for (Annotation annotation : JCasUtil.select(aJCas, Annotation.class)) {
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
            
            mtasId = indexAnnotation(tokenCollection, annotation, beginToken, endToken, mtasId);
        }
        
        return tokenCollection;
    }
    
    private int indexAnnotation(MtasTokenCollection aTokenCollection, AnnotationFS aAnnotation,
            int aBeginToken, int aEndToken, int aMtasId)
    {
        int mtasId = aMtasId;
        
        // Special case: token values must be indexed
        if (aAnnotation instanceof Token) {
            indexTokenText(aAnnotation, aBeginToken, aEndToken, mtasId++);
        } 
        // Special case: sentences must be indexed
        else if (aAnnotation instanceof Sentence) {
            indexSentenceText(aAnnotation, aBeginToken, aEndToken, mtasId++);
        }
        else {
            AnnotationLayer layer = layers.get(aAnnotation.getType().getName());
            
            // If the layer is not in the layers index, then it is not enabled.
            if (layer == null) {
                return mtasId;
            }
            
            // Index the annotation text
            indexAnnotationText(layer, aAnnotation, aBeginToken, aEndToken, mtasId++);
            
            // Iterate over the features of this layer and index them one-by-one
            for (AnnotationFeature feature : layerFeatures.get(aAnnotation.getType().getName())) {
                Optional<FeatureIndexingSupport> fis = featureIndexingSupportRegistry
                        .getIndexingSupport(feature);
                if (fis.isPresent()) {
                    Map<String, String> fieldsAndValues = fis.get().indexFeatureValue(feature,
                            aAnnotation);
                    for (Entry<String, String> e : fieldsAndValues.entrySet()) {
                        indexFeatureValue(e.getKey(), e.getValue(), mtasId++,
                                aAnnotation.getBegin(), aAnnotation.getEnd(), aBeginToken,
                                aEndToken);
                    }
                }
            }
        }
        
        return mtasId;
    }

    private void indexTokenText(AnnotationFS aAnnotation, int aBeginToken, int aEndToken,
            int aMtasId)
    {
        MtasToken mtasToken = new MtasTokenString(aMtasId, MTAS_TOKEN_LABEL,
                aAnnotation.getCoveredText(), aBeginToken);
        mtasToken.setOffset(aAnnotation.getBegin(), aAnnotation.getEnd());
        mtasToken.addPositionRange(aBeginToken, aEndToken);
        tokenCollection.add(mtasToken);
    }

    private void indexSentenceText(AnnotationFS aAnnotation, int aBeginToken, int aEndToken,
            int aMtasId)
    {
        MtasToken mtasSentence = new MtasTokenString(aMtasId, MTAS_SENTENCE_LABEL,
                aAnnotation.getCoveredText(), aBeginToken);
        mtasSentence.setOffset(aAnnotation.getBegin(), aAnnotation.getEnd());
        mtasSentence.addPositionRange(aBeginToken, aEndToken);
        tokenCollection.add(mtasSentence);
    }

    private void indexAnnotationText(AnnotationLayer aLayer, AnnotationFS aAnnotation,
            int aBeginToken, int aEndToken, int aMtasId)
    {
        MtasToken mtasSentence = new MtasTokenString(aMtasId, getIndexedName(aLayer.getUiName()),
                aAnnotation.getCoveredText(), aBeginToken);
        mtasSentence.setOffset(aAnnotation.getBegin(), aAnnotation.getEnd());
        mtasSentence.addPositionRange(aBeginToken, aEndToken);
        tokenCollection.add(mtasSentence);
    }

    private void indexFeatureValue(String aField, String aValue, int aMtasId, int aBeginOffset,
            int aEndOffset, int aBeginPosition, int aEndPosition)
    {
        MtasToken mtasAnnotationTypeFeatureLabel = new MtasTokenString(aMtasId,
                getIndexedName(aField), aValue, aBeginPosition);
        mtasAnnotationTypeFeatureLabel.setOffset(aBeginOffset, aEndOffset);
        mtasAnnotationTypeFeatureLabel.addPositionRange(aBeginPosition, aEndPosition);
        tokenCollection.add(mtasAnnotationTypeFeatureLabel);
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
