/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.externalsearch.cluster;

import static java.util.Arrays.asList;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;

public class ExternalSearchSentenceExtractor {
    private static String TYPE_NAME_UNIT = "Unit";
    private static String FEATURE_NAME_SCORE = "score";
    private List<ExternalSearchResult> externalSearchResults;
    private ExternalSearchService externalSearchService;
    
    private JCas doc;
    private AnalysisEngine splitter;
    private AnalysisEngine marker;
    private AnalysisEngine scorer;
    
    public ExternalSearchSentenceExtractor(List<ExternalSearchResult> aExternalSearchResults,
            ExternalSearchService aExternalSearchService, String query)
        throws Exception
    {
        externalSearchResults = aExternalSearchResults;
        externalSearchService = aExternalSearchService;
        
        // Set up custom type system
        TypeSystemDescription customTypes = getResourceSpecifierFactory()
                .createTypeSystemDescription();
        TypeDescription tdUnit = customTypes.addType(TYPE_NAME_UNIT, "",
                TYPE_NAME_ANNOTATION);
        tdUnit.addFeature(FEATURE_NAME_SCORE, "", TYPE_NAME_DOUBLE);
    
        // Set up processing components
        splitter = createEngine(BreakIteratorSegmenter.class);
        marker = createEngine(UnitByQueryWordAnnotator.class,
                UnitByQueryWordAnnotator.PARAM_QUERY_WORD, query);
        scorer = createEngine(GoodnessScoreAnnotator.class,
                UnitByQueryWordAnnotator.PARAM_QUERY_WORD, query);

        doc = createJCas(mergeTypeSystems(asList(customTypes, createTypeSystemDescription())));
    }
    
    public List<ExtractedUnit> extractSentences()
            throws Exception
    {
        // Process text files
        List<ExtractedUnit> relevantSentences = new ArrayList<>();
        for (ExternalSearchResult result: externalSearchResults) {
            // Clear contents so we can process the next file
            doc.reset();
            doc.setDocumentText(externalSearchService.getDocumentText(
                    result.getRepository(), result.getCollectionId(), result.getDocumentId()));
    
            // Annotate sentences and tokens
            splitter.process(doc);
    
            // Annotate sentences containing the query word
            marker.process(doc);
    
            // Annotate units with goodness score
            scorer.process(doc);
    
            // Extract sentences
            Type tUnit = doc.getTypeSystem().getType(TYPE_NAME_UNIT);
            Feature fScore = tUnit.getFeatureByBaseName(FEATURE_NAME_SCORE);
            for (AnnotationFS unit : CasUtil.select(doc.getCas(), tUnit)) {
                relevantSentences.add(new ExtractedUnit(unit.getCoveredText(),
                        unit.getDoubleValue(fScore), result));
            }
        }
        return relevantSentences;
    }
}
