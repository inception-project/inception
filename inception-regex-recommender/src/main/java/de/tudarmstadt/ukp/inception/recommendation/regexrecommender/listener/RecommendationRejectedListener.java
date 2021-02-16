package de.tudarmstadt.ukp.inception.recommendation.regexrecommender.listener;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.RegexCounter;

public class RecommendationRejectedListener
                implements ApplicationListener<RecommendationRejectedEvent>
{
    private List<RegexCounter> counterList;
 
    @Autowired
    public RecommendationRejectedListener()
    {
        counterList = new ArrayList<RegexCounter>();
    }
    
    public void addCounter(RegexCounter aRegexCounter) 
    {   
        // TODO: this is hacky.
        // When a new RegexRecommender Object gets created
        // we create a RegexCounter for it and add it to the counterList.
        // Before we add the new RegexCounter, we write the old counter
        // to its gazeteer and then delete it. 
        if (counterList.size() > 0) {
            int i = 0;
            for (RegexCounter counter: counterList) {
                if (counter.getLayer().equals(aRegexCounter.getLayer())
                && counter.getFeature().equals(aRegexCounter.getFeature())) {
                    counter.writeToGazeteer();
                    break;
                }
                i++;
            }
            counterList.remove(i);
        }
        counterList.add(aRegexCounter);
    }
    
    
    
    @Override
    public void onApplicationEvent(RecommendationRejectedEvent aEvent)
    {   
        if (aEvent.getConfidenceExplanation().isPresent()) {
            String regex = aEvent.getConfidenceExplanation().get().replace("Based on the regex ", "");
                    
                    
            AnnotationFeature feature = aEvent.getFeature();
            String featureValue = aEvent.getRecommendedValue().toString();
            for (RegexCounter counter: counterList) {
                if (counter.getFeature().equals(feature)) {
                    counter.incrementRejected(featureValue, regex);
                }
            }
        }
    }

}
