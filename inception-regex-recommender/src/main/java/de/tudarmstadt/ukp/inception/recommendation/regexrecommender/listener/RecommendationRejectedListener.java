package de.tudarmstadt.ukp.inception.recommendation.regexrecommender.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.RegexCounter;

public class RecommendationRejectedListener
                implements ApplicationListener<RecommendationRejectedEvent>
{
    RegexCounter regexSet;
 
    @Autowired
    public RecommendationRejectedListener(RegexCounter aRegexSet)
    {
        this.regexSet = aRegexSet;
    }
    
    @Override
    public void onApplicationEvent(RecommendationRejectedEvent aEvent)
    {   
        String regex = aEvent.getConfidenceExplanation().get().replace("Based on the regex ", "");
        String featureName = aEvent.getRecommendedValue().toString();
        regexSet.incrementRejected(featureName, regex);
    }

}
