package de.tudarmstadt.ukp.inception.recommendation.regexrecommender;

import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability.TRAINING_REQUIRED;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.Gazeteer;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.gazeteer.GazeteerEntryImpl;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.gazeteer.GazeteerServiceImpl;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.listener.RecommendationAcceptedListener;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.listener.RecommendationRejectedListener;

//TODO:
// fix evaluation
// fix mouse hovering problem
// write tests


public class RegexRecommender
        extends RecommendationEngine
{       
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final GazeteerServiceImpl gazeteerService;
    private final RegexRecommenderTraits traits;
    private final RegexCounter regexCounter;  
    private final String gazeteerName; 
    
    public RegexRecommender(Recommender aRecommender,
            RegexRecommenderTraits aTraits,
            RecommendationAcceptedListener aRecAccListener,
            RecommendationRejectedListener aRecRejListener) {
        
        this(aRecommender, aTraits, aRecAccListener, aRecRejListener, null);
    }
     
    public RegexRecommender(Recommender aRecommender,
                            RegexRecommenderTraits aTraits,
                            RecommendationAcceptedListener aRecAccListener,
                            RecommendationRejectedListener aRecRejListener,
                            GazeteerServiceImpl aGazeteerService) {
        
        super(aRecommender);
        
        this.traits = aTraits;
        this.gazeteerService = aGazeteerService;
        this.gazeteerName = "New Regexes for Layer: "
                + aRecommender.getLayer().getUiName()
                + " Feature: "
                + aRecommender.getFeature().getUiName();
        
        // add a new Gazeteer that collects new Regexes
        // we need something to remember these, otherwise
        // the user will be asked about all of his new Annotations
        // at every startup
        Gazeteer myGaz = null;
        if (!gazeteerService.existsGazeteer(aRecommender, gazeteerName)) {
            myGaz = new Gazeteer(gazeteerName, aRecommender);
            gazeteerService.createOrUpdateGazeteer(myGaz);
            InputStream is = InputStream.nullInputStream();
            try {
                gazeteerService.importGazeteerFile(myGaz, is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            List<Gazeteer> gazeteers = gazeteerService.listGazeteers(recommender);
            for (Gazeteer gaz: gazeteers) {
                if (gaz.getName().equals(gazeteerName)) {
                    myGaz = gaz;
                    break;
                }
            }
        }
        
        this.regexCounter = new RegexCounter(aRecommender.getLayer(), aRecommender.getFeature(), aGazeteerService, myGaz);
        aRecAccListener.addCounter(regexCounter);
        aRecRejListener.addCounter(regexCounter);
        
        pretrain();
        
    }

    @Override
    public RecommendationEngineCapability getTrainingCapability() 
    {
        return TRAINING_REQUIRED;
    }
    
    /* returns the concatenation of the lemmas that are covered by aAnnotation, with 
    * whitespace. This is important because whitespace might not always be one space
    */
    private String getUnderlyingLemmaString(CAS aCas, AnnotationFS aAnnotation)
    {
        Type lemmaType = CasUtil.getAnnotationType(aCas, Lemma.class);
        Feature lemmaFeature = lemmaType.getFeatureByBaseName("value");
        String docText = aCas.getDocumentText();
        List<AnnotationFS> lemmaList = CasUtil.selectCovered(aCas,
                                                             lemmaType,
                                                             aAnnotation.getBegin(),
                                                             aAnnotation.getEnd());
        StringBuilder lemmaString = new StringBuilder("");
        boolean firstLemma = true;
        CharSequence spaceBetween;
        AnnotationFS previousLemma = null;
        
        for (AnnotationFS lemma: lemmaList) {
            
            // spaceBetween is the whitespace between two lemmas
            if (firstLemma) {
                spaceBetween = docText.subSequence(aAnnotation.getBegin(), lemma.getBegin());
            } else {
                spaceBetween = docText.subSequence(previousLemma.getEnd(), lemma.getBegin());
            }
            firstLemma = false;
            lemmaString.append(spaceBetween);
            lemmaString.append(lemma.getFeatureValueAsString(lemmaFeature));
            previousLemma = lemma;
        }
        return lemmaString.toString();
    }
    
        
    /*
     * loads regexes from gazeteers
     */
    private void pretrain()
    {   
        System.out.println("PRETRAIN");
        for (Gazeteer gaz : gazeteerService.listGazeteers(recommender)) {
            try {                  
                for (GazeteerEntryImpl entry: gazeteerService.readGazeteerFile(gaz)) {
                    regexCounter.putIfRegexAbsent(entry.label,
                                                  entry.regex,
                                                  entry.acceptedCount,
                                                  entry.rejectedCount);
                }
            }
            catch (IOException e) {
                log.info("Unable to load gazeteer [{}] for recommender [{}]({}) in project [{}]({})",
                        gaz.getName(), gaz.getRecommender().getName(),
                        gaz.getRecommender().getId(),
                        gaz.getRecommender().getProject().getName(),
                        gaz.getRecommender().getProject().getId(), e);
            }
        }
    }
   
    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
            throws RecommendationException
    {   
        // pretrain with gazeteers
        pretrain();
                
        for (CAS cas : aCasses) {
            getPredictedType(cas);
            Type myType = getPredictedType(cas);
            Feature myFeature = getPredictedFeature(cas);
            // We get a list of all our Annotations in the cas.
            Collection<AnnotationFS> annos = CasUtil.select(cas, myType);
            // For each annotation we get the lemmatized text that the annotation spans.
            // Then we ask the user if she wants to add the new lemmas to regexCounter
            // except if we already recommended that annotation.
            for (AnnotationFS anno : annos) {
                String lemmaString = getUnderlyingLemmaString(cas, anno);
                String featureValue = anno.getFeatureValueAsString(myFeature);
                if (!(featureValue == null)) {
                    regexCounter.addWithMsgBox(anno.getFeatureValueAsString(myFeature), lemmaString);
                }
            }        
        }
        // we check for each feature value and each regex
        // in the regexCounter whether it produces too many
        // false Annotations. If it does, we ask the user
        // if she wants to remove the regex.
        // This could be replaced by automatic removal when
        // the accuracy of a regex becomes too small.
        for (String featureValue: regexCounter.getKeys()) {
            Map<String, Pair<Integer, Integer>> accRejCount = regexCounter.get(featureValue);
            
            for (Map.Entry<String, Pair<Integer, Integer>> entry: accRejCount.entrySet()) {
                String regex = entry.getKey(); 
                double pos = entry.getValue().getLeft().doubleValue();
                double neg = entry.getValue().getRight().doubleValue();
                
                Double accuracy = pos / (pos + neg);
                if (accuracy < 0.1 && pos + neg > 10 ) {
                    regexCounter.removeWithMsgBox(featureValue,
                                                  regex,
                                                  Double.valueOf(pos).intValue(),
                                                  Double.valueOf(neg).intValue());
                }
            }
        }
    }
    
    
    
  
    
    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {    
        return !regexCounter.getKeys().isEmpty();
    }
    
    
    private List<Annotation> extractAnnotations(List<CAS> aCasses)
    {
        List<Annotation> annotations = new ArrayList<>();

        for (CAS cas : aCasses) {
            Type annotationType = CasUtil.getType(cas, layerName);
            Feature predictedFeature = annotationType.getFeatureByBaseName(featureName);

            for (AnnotationFS ann : CasUtil.select(cas, annotationType)) {
                String label = ann.getFeatureValueAsString(predictedFeature);
                annotations.add(new Annotation(label, ann.getBegin(), ann.getEnd()));
            }
        }
        return annotations;
    }

    
    /*
     * returns a list of Annotation Objects.
     * Each Annotation Object is a prediction for the user.
     */
    private List<Annotation> getPredictions(CAS aCas)
    {   
        Type lemmaType = CasUtil.getAnnotationType(aCas, Lemma.class);
        Feature lemmaFeature = lemmaType.getFeatureByBaseName("value");

        Collection<AnnotationFS> lemmas = CasUtil.select(aCas, lemmaType);
        
        // The String in this pair is the lemmatized document.
        // The map is a mapping from character Index
        // in the lemmatized doc to character Index in the token-doc.
        // Doc = "The president's wife is beatifull"
        // LemmatizedDoc = "the president wife be beatifull"
        // Map = 0-0, 1-1, 2-2, 3-3 ... 12-12, 13-15, 14-16, ...
        // we need this since we check for regex matches on the 
        // lemmatized document, but add predictions with token
        // indices.
        Pair<String, Map<Integer, Integer>> pair = this.createLemmatizedDoc(lemmas,
                                                                            aCas.getDocumentText(),
                                                                            lemmaFeature,
                                                                            lemmaType);
        String lemmatizedDoc = pair.getLeft();
        Map<Integer, Integer> lemmaTokenMapping = pair.getRight();
        
        List<Annotation> predictions = new ArrayList<>(); 
        for (String featureValue: regexCounter.getKeys()) {
            for (String regex: regexCounter.getRegexes(featureValue) ) {
    
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(lemmatizedDoc);
                
                while (matcher.find()) {
                    // the matches are matches in the lemmatized doc.
                    // we use the lemmaToken Mapping to get the corresponding
                    // token start and end indices 
                    int tokenBegin = lemmaTokenMapping.get(matcher.start());
                    int tokenEnd = lemmaTokenMapping.get(matcher.end());
                    Annotation anno = new Annotation(featureValue, regex, tokenBegin, tokenEnd);
                    predictions.add(anno);
                }
            }
        }
        return predictions;
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {    
        List<Annotation> predictions = getPredictions(aCas);
        Type predictedType = getPredictedType(aCas);

        Feature predictedFeature = getPredictedFeature(aCas);
        Feature scoreFeature = getScoreFeature(aCas);
        Feature scoreExplanationFeature = getScoreExplanationFeature(aCas);
        Feature isPredictionFeature = getIsPredictionFeature(aCas);       
        
        for (Annotation ann : predictions) {

            AnnotationFS annotation = aCas.createAnnotation(predictedType, ann.begin, ann.end);
            annotation.setStringValue(predictedFeature, ann.label);         
            double pos = regexCounter.get(ann.label).get(ann.regex).getLeft().doubleValue();
            double neg = regexCounter.get(ann.label).get(ann.regex).getRight().doubleValue();
            Double score = pos / (pos + neg);
            if (!score.isInfinite() && !score.isNaN()) {
                annotation.setDoubleValue(scoreFeature, pos / (pos + neg));                
            } else {
                annotation.setDoubleValue(scoreFeature, 1.0);                
            }
            annotation.setStringValue(scoreExplanationFeature, ann.explanation);
            annotation.setBooleanValue(isPredictionFeature, true);
            aCas.addFsToIndexes(annotation);
        }
    }
    
    /*
     * returns a {@code Pair<String, ArrayList<Integer>>}.
     * The String is a concatenation of all the lemmas in
     * the document, with whitespace. The ArrayList is a
     * mapping from character Index in the lemmaString
     * to character Index in the tokenString.
     * We need this mapping when we create new Annotations,
     * since the begin and end indices are indices in the 
     * token string.
     */
    private Pair<String, Map<Integer, Integer>> createLemmatizedDoc(Collection<AnnotationFS> aLemmas,
            String aDocText,
            Feature aLemmaFeature,
            Type aLemmaType)
    {   
    
        StringBuilder lemmatizedDoc = new StringBuilder("");
        Map<Integer, Integer> lemmaTokenMapping = new HashMap<>();
        AnnotationFS previousLemma = null;
        CharSequence spaceBetween;
        int nextIndex = 0;
        int offset = 0;
                
        for (AnnotationFS lemma: aLemmas) {
            
            if (previousLemma == null) {
                spaceBetween = aDocText.subSequence(0, lemma.getBegin());
            } else {
                spaceBetween = aDocText.subSequence(previousLemma.getEnd(), lemma.getBegin());
            }
            
            String lemmaText = lemma.getFeatureValueAsString(aLemmaFeature);
            String tokenText = lemma.getCoveredText();
            
            lemmatizedDoc.append(spaceBetween);
            for (Integer i: range(nextIndex, spaceBetween.length())) {
                lemmaTokenMapping.put(i, i+offset);
            }
            
            nextIndex += spaceBetween.length();
            lemmatizedDoc.append(lemmaText);

            if (lemmaText.length() > tokenText.length()) {
                for (Integer i: range(nextIndex, lemmaText.length())) {
                    lemmaTokenMapping.put(i, i + offset);
                }
                nextIndex += tokenText.length();
                offset += tokenText.length()-lemmaText.length();
                for (Integer i: range(nextIndex, lemmaText.length() - tokenText.length())) {
                    lemmaTokenMapping.put(i, i+offset);
                }                     
                nextIndex += lemmaText.length() - tokenText.length();
                        
            } else  {            
                for (Integer i: range(nextIndex, lemmaText.length())) {
                    lemmaTokenMapping.put(i, i + offset);
                }
                nextIndex += lemmaText.length();
                offset += tokenText.length() - lemmaText.length();
            }
            previousLemma = lemma;          
        }
        
        String strlemmatizedDoc = lemmatizedDoc.toString();
        return new Pair<>(strlemmatizedDoc, lemmaTokenMapping);
        
    }
   
        
    private List<Integer> range(int aBegin, int aLength)
    {
        List<Integer> numbers = Stream.iterate(aBegin, n -> n + 1) 
                  .limit(aLength)
                  .collect(Collectors.toList());
        return numbers;
    }

  
    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
            throws RecommendationException
    {
        
        //TODO: implement the evaluate function.
        //this is just dummy code to skip the evaluation
        EvaluationResult result = new EvaluationResult(1, 1, 1);
        result.setEvaluationSkipped(true);
        return result;
        
        
    }
    
    
    private static class Annotation 
    {
        private final String label;
        private final String explanation;
        private final String regex;
        private final int begin;
        private final int end;

        private Annotation(String aLabel, int aBegin, int aEnd)
        {
            this(aLabel, "NO REGEX", aBegin, aEnd);
        }
        
        private Annotation(String aLabel, String aRegex, int aBegin, 
                int aEnd)
        {
            label = aLabel;
            explanation = "Based on the regex " + aRegex;
            regex = aRegex;
            begin = aBegin;
            end = aEnd;
        }    
        
        private int length()
        {
            return end - begin;
        }
   
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + begin;
            result = prime * result + end;
            result = prime * result + ((explanation == null) ? 0 : explanation.hashCode());
            result = prime * result + ((label == null) ? 0 : label.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object aObj)
        {
            if (this == aObj)
                return true;
            if (aObj == null)
                return false;
            if (getClass() != aObj.getClass())
                return false;
            Annotation other = (Annotation) aObj;
            if (begin != other.begin)
                return false;
            if (end != other.end)
                return false;
            if (label == null) {
                if (other.label != null)
                    return false;
            } else if (!label.equals(other.label))
                return false;
            return true;
        }

        @Override
        public String toString()
        {
            return "Label: " +
                    this.label +
                    " Explanation: " +
                    this.explanation +
                    " Begin: " +
                    this.begin +
                    " End: " +
                    this.end;
        }
        
    }

    
    

}
