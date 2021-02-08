package de.tudarmstadt.ukp.inception;

import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability.TRAINING_REQUIRED;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.LabelPair;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;


// tag::classDefinition[]

public class RegexRecommender
        extends RecommendationEngine
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private RecommendationAcceptedListener acceptedListener;
    private RecommendationRejectedListener rejectedListener;
    private RegexSet regexSet;
    private String filename;
    private String uiFeatureName;
     
    public RegexRecommender(Recommender aRecommender,
                            RecommendationAcceptedListener aRecAccListener,
                            RecommendationRejectedListener aRecRejListener) {
        
        super(aRecommender);
        String filename = FileHelper.exportDornseiffFile("LemmatizedZeit.txt");
        this.uiFeatureName = aRecommender.getFeature().getUiName();
        this.filename = filename;
        this.regexSet = new RegexSet(FileHelper.readDornseiffFile(filename, uiFeatureName));
        this.acceptedListener = aRecAccListener;
        this.rejectedListener = aRecRejListener;
    }
    
    @Override
    public RecommendationEngineCapability getTrainingCapability() 
    {
        return TRAINING_REQUIRED;
    }
   
    public void train(List<CAS> aCasses)
    {
        
        for (CAS cas : aCasses) {
            getPredictedType(cas);
            Type dornseiffType = getPredictedType(cas);
            Feature myFeature = getPredictedFeature(cas);
            
            // We get a list of all dornseiffAnnotations in the cas.
            Collection<AnnotationFS> dornseiffAnnos = CasUtil.select(cas, dornseiffType);
            // we get rid of Annotations that don't have the feature of our recommender
            List<AnnotationFS> filteredDornseiffAnnos  = dornseiffAnnos.stream()
                                          .filter(anno -> !(anno.getStringValue(myFeature) == null))
                                          .collect(Collectors.toList());
            
            // for each annotation we get the lemmatized text, that the annotation spans.
            // Then we ask the user if she wants to add the new lemmas to regexSet.
            for (AnnotationFS ann : filteredDornseiffAnnos) {
                String lemmaString = getUnderlyingLemmaString(cas, ann);
                regexSet.addWithMsgBox(lemmaString);
            }        
        }
        // we check for each regex in the regexSet whether it produces
        // too many false Annotations.
        // if it does, we ask the user if she wants to remove the regex.
        Set<Map.Entry<String, Integer>> rejListEntrySet = rejectedListener.
                                            getRejectedCount().
                                            get(uiFeatureName).
                                            entrySet();
        
        for (Map.Entry<String, Integer> entry: rejListEntrySet) {
            String regex = entry.getKey(); 
            if (regexSet.contains(regex)) {
                double neg = entry.getValue().doubleValue();
                double pos = acceptedListener.getAcceptedCount().
                        get(uiFeatureName).
                        get(regex).doubleValue();
                
                Double accuracy = pos / (pos + neg);
                if (accuracy < 0.1 && pos + neg > 10 ) {
                    regexSet.removeWithMsgBox(regex, Double.valueOf(pos).intValue(),
                                                          Double.valueOf(neg).intValue());
                }
            }
        }
        regexSet.write();        
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
            
            //spaceBetween is the whitespace between two lemmas
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
   
    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
            throws RecommendationException
    {    
        train(aCasses);
    }
  
    
    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {    
        return this.regexSet.size() > 0;
    }
    
    /*
     * Takes a Cas and returns a list of Annotation Objects that describe
     * the Annotations of this recommenders type in the Cas.
     */
    private List<Annotation> extractAnnotations(CAS aCas)
    {
        List<Annotation> annotations = new ArrayList<>();
        Type annotationType = getPredictedType(aCas); 
        Feature predictedFeature = getPredictedFeature(aCas);

        for (AnnotationFS ann : CasUtil.select(aCas, annotationType)) {
            String label = ann.getFeatureValueAsString(predictedFeature);
            if (isNotEmpty(label)) {
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
        
        //The String is the lemmatizedDocument, The ArrayList is a mapping from character Index
        //in the lemmatizedDoc to character Index in the normal Doc.
        //Doc = "The president's wife is beatifull"
        //LemmatizedDoc = "the president wife be beatifull"
        //List = 0 1 2 3 4 5 6 7 8 9 10 11 12 13 16 ...
        //this helps us translate between indexes in lemmatized Doc and tokenized Doc.
        //See the difference between "president" and "president's"
        
        Pair<String, ArrayList<Integer>> pair = this.createLemmatizedDoc(lemmas,
                                                                         aCas.getDocumentText(),
                                                                         lemmaFeature,
                                                                         lemmaType);
        String docText = pair.getLeft();
        List<Integer> lemmaTokenMapping = pair.getRight();
        
        List<Annotation> predictions = new ArrayList<>();
                
        for (String item: regexSet.getRegexes() ) {

            Pattern pattern = Pattern.compile(item);
            Matcher matcher = pattern.matcher(docText);
            
            while (matcher.find()) {
                //the matches are matches in the lemmatized doc.
                //we use the lemmaToken Mapping to get the start and
                //end indices in the normal doc.
                int tokenBegin = lemmaTokenMapping.get(matcher.start());
                int tokenEnd = lemmaTokenMapping.get(matcher.end());
                Annotation anno = new Annotation(this.uiFeatureName, item, tokenBegin, tokenEnd);
                predictions.add(anno);
            }
        }
        return predictions;
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {    
        
        List<Annotation> predictions = getPredictions(aCas);
        // We sort the predictions by length, long to short, because 
        // we want to go through them one by one. If a later one is contained
        // in an earlier one, we can discard it.
        // Example: "Der langjährige prozess"
        // would get an annotation for lang and for langjährig
        // but we can discard with lang
        sortListLongToShort(predictions);
        Type predictedType = getPredictedType(aCas);

        Feature predictedFeature = getPredictedFeature(aCas);
        Feature scoreFeature = getScoreFeature(aCas);
        Feature scoreExplanationFeature = getScoreExplanationFeature(aCas);
        Feature isPredictionFeature = getIsPredictionFeature(aCas);       
        
        int count = 0;
        for (Annotation ann : predictions) {
            boolean add = true;
            for (Annotation longerAnno: predictions.subList(0, count)) {
                if (longerAnno.containsInSpan(ann)) {
                    add = false;
                    System.out.println("Rejected" + ann.toString());
                    System.out.println("Because of:" + longerAnno.toString());
                    break;
                }
            }
            count ++;
            if (add) {
                AnnotationFS annotation = aCas.createAnnotation(predictedType, ann.begin, ann.end);
                annotation.setStringValue(predictedFeature, ann.label);
                //here we calculate a confidence score for our prediction
                //the confidence score is accuracy, estimated by previously rejected
                //and accepted suggestions of the same type and same regex
                //TODO: Include annotations that were already there and are not saved 
                // in the accepted/rejected listeners.
                //Maybe it would be best to initialize the listeners with the 
                // annotations that are already there
                
                double pos = acceptedListener.getAcceptedCount().
                                                get(uiFeatureName).
                                                getOrDefault(ann.regex, 0).
                                                doubleValue();
                
                double neg = rejectedListener.getRejectedCount().
                                                get(uiFeatureName).
                                                getOrDefault(ann.regex, 0).
                                                doubleValue();
                
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
    }
    
    private void sortListLongToShort(List<Annotation> aList)
    {
        aList.sort(Comparator.comparing(Annotation::length).reversed());
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
    
    private Pair<String, ArrayList<Integer>> createLemmatizedDoc(Collection<AnnotationFS> aLemmas,
                                                                 String aDocText,
                                                                 Feature aLemmaFeature,
                                                                 Type aLemmaType)
    {
      
        StringBuilder lemmatizedDoc = new StringBuilder("");
        ArrayList<Integer> lemmaTokenMapping = new ArrayList<>();
        AnnotationFS previousLemma = null;
        CharSequence spaceBetween;
        boolean firstLemma = true;
        int nextNumber = 0;
        
        for (AnnotationFS lemma: aLemmas) {
            
            if (firstLemma) {
                spaceBetween = aDocText.subSequence(0, lemma.getBegin());
            } else {
                spaceBetween = aDocText.subSequence(previousLemma.getEnd(), lemma.getBegin());
            }
            firstLemma = false;
            
            String lemmaText = lemma.getFeatureValueAsString(aLemmaFeature);
            String tokenText = lemma.getCoveredText();
            
            lemmatizedDoc.append(spaceBetween);
            lemmaTokenMapping.addAll(range(nextNumber, spaceBetween.length()));
            nextNumber += spaceBetween.length();
            
            lemmatizedDoc.append(lemmaText);
            
            if (lemmaText.length() > tokenText.length()) {
   
                lemmaTokenMapping.addAll(range(nextNumber, tokenText.length()));
                nextNumber += tokenText.length();
                lemmaTokenMapping.addAll(listOf(nextNumber - 1,
                                         lemmaText.length() - tokenText.length()));        
                        
            } else if (lemmaText.length() == tokenText.length()) {
                
                lemmaTokenMapping.addAll(range(nextNumber, tokenText.length()));
                nextNumber += tokenText.length();
                
            } else if (lemmaText.length() < tokenText.length()) {
                lemmaTokenMapping.addAll(range(nextNumber, lemmaText.length()));
                nextNumber += tokenText.length();
            }
            previousLemma = lemma;          
        }
 
        lemmaTokenMapping.add(Integer.valueOf(lemmaTokenMapping.size()));
        String strlemmatizedDoc = lemmatizedDoc.toString();
        
        Pair<String, ArrayList<Integer>> pair = new Pair<>(strlemmatizedDoc, lemmaTokenMapping);
        return pair;
    }

    
    private List<Integer> range(int aBegin, int aLength)
    {
        List<Integer> numbers = Stream.iterate(aBegin, n -> n + 1) 
                  .limit(aLength)
                  .collect(Collectors.toList());
        return numbers;
    }
    
    private List<Integer> listOf(int aNumber, int aLength)
    {
        List<Integer> numbers = Stream.iterate(aNumber, n -> n) 
                  .limit(aLength)
                  .collect(Collectors.toList());
        return numbers;
    }

  
    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
            throws RecommendationException
    {
               
        List<CAS> trainingCases = new ArrayList<>();
        List<CAS> testCases = new ArrayList<>();
        
        for (CAS cas : aCasses) {
            switch (aDataSplitter.getTargetSet(cas)) {
            case TRAIN:
                trainingCases.add(cas);
                break;
            case TEST:
                testCases.add(cas);
                break;
            case IGNORE:
                break;
            }
        }

        int trainingSetSize = 0;

        for (CAS cas: trainingCases) {
            trainingSetSize += extractAnnotations(cas).size();
        }

        int testSetSize = 0;
        for (CAS cas: testCases) {
            testSetSize += extractAnnotations(cas).size();
        }
        double trainRatio = 1.0;
                
        if (trainingSetSize < 1 || testSetSize < 1) {
            log.info("Not enough data to evaluate, skipping!");
            EvaluationResult result = new EvaluationResult(trainingSetSize,
                    testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            return result;
        }
        
        train(trainingCases);
        
        List<LabelPair> labelPairs = new ArrayList<>();
        for (CAS cas: testCases) {
            List<Annotation> goldAnnotations = extractAnnotations(cas);
            List<Annotation> predictions = getPredictions(cas);
            for (Annotation anno: goldAnnotations) {
                if (predictions.contains(anno)) {
                    labelPairs.add(new LabelPair(anno.label, anno.label));
                    predictions.remove(anno);
                } else {
                    labelPairs.add(new LabelPair(anno.label, "None"));
                }
            }
            for (Annotation prediction: predictions) {
                if (goldAnnotations.contains(prediction)) {
                    labelPairs.add(new LabelPair(prediction.label, prediction.label));
                    goldAnnotations.remove(prediction);
                } else {
                    labelPairs.add(new LabelPair("None", prediction.label));
                }
            }
        }

        // evaluation: collect predicted and gold labels for evaluation
        EvaluationResult result = labelPairs.stream()
                .collect(EvaluationResult.collector(trainingSetSize, testSetSize, trainRatio));
        
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
            this(aLabel, "not given", aBegin, aEnd);
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
        
        public boolean containsInSpan(Annotation aShorter)
        {
            
            if (this.begin <= aShorter.begin && this.end > aShorter.end) {
                return true;
            } else if (this.begin < aShorter.begin && this.end >= aShorter.end) {
                return true;
            }
            return false;
        }
    }

    private class Pair<L,R>
    {
        private L left;
        private R right;

        public Pair(L aLeft, R aRight)
        {
            assert left != null;
            assert right != null;

            this.left = aLeft;
            this.right = aRight;
        }

        public L getLeft()
        {
            return left;
        }
        public R getRight()
        {
            return right;
        }
          
        public void setLeft(L aItem)
        {
            this.left = aItem;
        }
        
        public void setRight(R aItem)
        {
            this.right = aItem;
        }

        @Override
        public int hashCode()
        {
            return left.hashCode() ^ right.hashCode();
        }

        @Override
        public boolean equals(Object aObj)
        {
            if (!(aObj instanceof Pair)) return false;
            Pair pairo = (Pair) aObj;
            return this.left.equals(pairo.getLeft()) &&
                   this.right.equals(pairo.getRight());
        }

    }
    
    public String getFromInputBox(String aInfoMessage, String aTitleBar)
    {    
        JFrame f = new JFrame();
        f.setAlwaysOnTop(true);
        return JOptionPane.showInputDialog(f,
                                           aInfoMessage,
                                           "InfoBox: " + aTitleBar,
                                           JOptionPane.QUESTION_MESSAGE);
    }
    
    public Boolean getFromBooleanBox(String aInfoMessage)
    {    
        JFrame f = new JFrame();
        f.setAlwaysOnTop(true);
        int i =  JOptionPane.showConfirmDialog(f, aInfoMessage);
        
        if (i == 0) { 
            return true;
        } else {
            return false;
        }
    }
    
    public class RegexSet {
        
        Set<String> regexes;
        
        private RegexSet(Set<String> aRegexes)
        {
            this.regexes = aRegexes;
        }
        
        private Set<String> getRegexes() {
            return regexes;
        }
        
        private boolean containsByRegex(String aLemmaString) {
            boolean contained = false;
            for (String item: regexes) {
                if (aLemmaString.matches(item)) {
                    contained = true;
                    break;
                }                   
            }
            return contained;
        }
        
        private void write() {
            try {
                FileHelper.writeList(regexes, uiFeatureName, filename);
                FileHelper.writeAccRejFile(acceptedListener, rejectedListener);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        private int size() {
            return regexes.size();
        }
        
        private void add(String aRegex) {
            regexes.add(aRegex);
            Map<String, Integer> accMap = acceptedListener.getAcceptedCount().get(uiFeatureName);
            accMap.putIfAbsent(aRegex, 1);
            Map<String, Integer> rejMap = rejectedListener.getRejectedCount().get(uiFeatureName);
            rejMap.putIfAbsent(aRegex, 0);
        }
        
        private void addWithMsgBox(String aLemmaString) {
                                   
            if (!containsByRegex(aLemmaString)) {
                String regexString = getFromInputBox("During training the annotation " + uiFeatureName + " based on " + aLemmaString + " was found. \n Please enter regex or discard", "Trainer");
                if (regexString == null) {
                    add(aLemmaString);
                } else {
                    add(regexString);
                }
            }
        }
        
        private void remove(String aRegex) {
            regexes.remove(aRegex);            
        }
        
        private void removeWithMsgBox(String aRegex, Integer aPos, Integer aNeg) {
                
            if (getFromBooleanBox("The regex: " + aRegex + " has been accepted " + aPos.toString() + " times and "
                    + "rejected " + aNeg.toString() + "times.\n The category was " + featureName + 
                    ".\n Do you wish to delete this regex?")) {
                    
                remove(aRegex);
            }
        }
        
        private boolean contains(String aRegex) {
            return regexes.contains(aRegex);
        }
    }
}
