package de.tudarmstadt.ukp.inception.recommendation.regexrecommender.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.RegexCounter;

public class RecommendationAcceptedListener
                implements ApplicationListener<RecommendationAcceptedEvent>
{
    
    RegexCounter regexSet;
 
    @Autowired
    public RecommendationAcceptedListener(RegexCounter aRegexSet)
    {
        this.regexSet = aRegexSet;
    }
    
    @Override
    public void onApplicationEvent(RecommendationAcceptedEvent aEvent)
    {   
        String regex = aEvent.getConfidenceExplanation().get().replace("Based on the regex ", "");
        String featureName = aEvent.getFeature().getUiName();
        regexSet.incrementAccepted(featureName, regex);
    }
}
