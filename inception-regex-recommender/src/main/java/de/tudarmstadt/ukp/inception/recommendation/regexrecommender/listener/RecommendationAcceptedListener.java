package de.tudarmstadt.ukp.inception.recommendation.regexrecommender.listener;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.RegexCounter;

public class RecommendationAcceptedListener
                implements ApplicationListener<RecommendationAcceptedEvent>
{
    
    private List<RegexCounter> counterList;
 
    @Autowired
    public RecommendationAcceptedListener()
    {
        counterList = new ArrayList<RegexCounter>();
    }
    
    public void addCounter(RegexCounter aRegexCounter) 
    {    

        counterList.removeIf(counter -> counter.getLayer().equals(aRegexCounter.getLayer())
                                        && counter.getFeature().equals(aRegexCounter.getFeature()));
        counterList.add(aRegexCounter);
        
    }
    
    @Override
    public void onApplicationEvent(RecommendationAcceptedEvent aEvent)
    {   
        if (aEvent.getConfidenceExplanation().isPresent()) {

            String regex = aEvent.getConfidenceExplanation().get().replace("Based on the regex ", "");
            AnnotationFeature feature = aEvent.getFeature();
            String featureValue = aEvent.getRecommendedValue().toString();
            for (RegexCounter counter: counterList) {
                if (counter.getFeature().equals(feature)) {
                    counter.incrementAccepted(featureValue, regex);
                }
            }
        }
    }
}
