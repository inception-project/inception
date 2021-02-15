package de.tudarmstadt.ukp.inception.recommendation.regexrecommender;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;


/*
 * Keeps track of regexes, their feature values
 * and the number of times recommendations based on
 * those regexes have been accepted or rejected by the user.
 * 
 */
public class RegexCounter
{
    
    private Map<String, Map<String, Pair<Integer, Integer>>> regexCounts;
    
    private AnnotationFeature feature;
    private AnnotationLayer layer;
    
    public RegexCounter(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        regexCounts = new HashMap<String, Map<String, Pair<Integer, Integer>>>();
        feature = aFeature;
        layer = aLayer;
    }
    
    public AnnotationFeature getFeature()
    {
        return feature;
    }
    
    public AnnotationLayer getLayer()
    {
        return layer;
    }
    
    public Set<String> getKeys() {
        return regexCounts.keySet();
    }
    
    public Collection<Map<String, Pair<Integer, Integer>>> getValues() {
        return regexCounts.values();
    }
    
    public void incrementAccepted(String aFeatureValue, String aRegex)
    {
        Pair<Integer, Integer> currentCount = regexCounts.get(aFeatureValue).get(aRegex);
        currentCount.setLeft(currentCount.getLeft() + 1);
    }
    
    public void incrementRejected(String aFeatureValue, String aRegex)
    {
        Pair<Integer, Integer> currentCount = regexCounts.get(aFeatureValue).get(aRegex);
        currentCount.setRight(currentCount.getRight() + 1);
    }
    
    public Map<String, Pair<Integer, Integer>> get(String aFeatureName)
    {
        return regexCounts.get(aFeatureName);
    }
    
    public void putIfFeatureAbsent(String aFeatureValue,
                            String aRegex,
                            Integer aAcceptedCount,
                            Integer aRejectedCount) 
    {    
        Map<String, Pair<Integer, Integer>> regexCountMap =
                new HashMap<String, Pair<Integer, Integer>>();
        regexCountMap.put(aRegex, new Pair<Integer, Integer> (aAcceptedCount, aRejectedCount));
        regexCounts.putIfAbsent(aFeatureValue, regexCountMap);
    }
    
    public void putIfRegexAbsent(String aFeatureValue,
                                 String aRegex,
                                 Integer aAccCount,
                                 Integer aRejCount)
    {   
        regexCounts.putIfAbsent(aFeatureValue, new HashMap<String, Pair<Integer, Integer>>());
        Map<String, Pair<Integer, Integer>> regexCountMap = regexCounts.get(aFeatureValue);
        regexCountMap.putIfAbsent(aRegex, new Pair<Integer, Integer>(aAccCount, aRejCount));       
    }
    
    public Set<String> getRegexes(String aFeatureValue) {
        
        return regexCounts.get(aFeatureValue).keySet();
    }
    
    public boolean containsByRegex(String aFeatureValue, String aLemmaString) {
        
        boolean contained = false;
        if (!regexCounts.keySet().contains(aFeatureValue)) {
            return false;
        }
        for (String item: getRegexes(aFeatureValue)) {
            if (aLemmaString.matches(item)) {
                contained = true;
                break;
            }                   
        }
        return contained;
    }
    
    public int size(String aFeatureValue) {
        return regexCounts.get(aFeatureValue).keySet().size();
    }
    
    public void add(String aFeatureValue, String aRegex) {
        if (regexCounts.containsKey(aFeatureValue)) {
            regexCounts.get(aFeatureValue).put(aRegex, new Pair<Integer, Integer>(1,0));
        } else {
            Map<String, Pair<Integer, Integer>> newRegex = 
                    new HashMap<String, Pair<Integer, Integer>>();
            Pair<Integer, Integer> counts = new Pair<Integer, Integer> (1,0);
            newRegex.put(aRegex, counts);
            regexCounts.put(aFeatureValue, newRegex);
        }
    }
    
    public void addWithMsgBox(String aFeatureValue, String aLemmaString) {
                               
        if (!containsByRegex(aFeatureValue, aLemmaString)) {
            String regexString = getFromInputBox("During training the annotation " + aFeatureValue 
                                                 + " based on " + aLemmaString + " was found. \n"
                                                 + " Please enter regex or discard", "Trainer");
            if (regexString == null) {
                add(aFeatureValue, aLemmaString);
            } else {
                add(aFeatureValue, regexString);
            }
        }
    }
    
    public void remove(String aFeatureValue, String aRegex) {
        regexCounts.get(aFeatureValue).remove(aRegex);            
    }
    
    public void removeWithMsgBox(String aFeatureValue, String aRegex, Integer aPos, Integer aNeg) {
            
        if (getFromBooleanBox("The regex: " + aRegex + " has been accepted " + aPos.toString() + " times and "
                + "rejected " + aNeg.toString() + "times.\n The category was " + aFeatureValue + 
                ".\n Do you wish to delete this regex?")) {
                
            remove(aFeatureValue, aRegex);
        }
    }
    
    public boolean contains(String aFeatureValue, String aRegex) {
        return regexCounts.get(aFeatureValue).containsKey(aRegex);
    }
    
    private String getFromInputBox(String aInfoMessage, String aTitleBar)
    {    
        JFrame f = new JFrame();
        f.setAlwaysOnTop(true);
        return JOptionPane.showInputDialog(f,
                                           aInfoMessage,
                                           "InfoBox: " + aTitleBar,
                                           JOptionPane.QUESTION_MESSAGE);
    }
    
    private Boolean getFromBooleanBox(String aInfoMessage)
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
}

