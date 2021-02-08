package de.tudarmstadt.ukp.inception;

import java.io.IOException;
import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationAcceptedEvent;

@Component
public class RecommendationAcceptedListener
                implements ApplicationListener<RecommendationAcceptedEvent>
{
    
    public Map<String, Map<String, Integer>> acceptedCount;
    
    public Map<String, Map<String, Integer>> getAcceptedCount()
    {
        return acceptedCount;
    }

    public void setAcceptedCount(Map<String, Map<String, Integer>> aAcceptedCount)
    {
        this.acceptedCount = aAcceptedCount;
    }

    public RecommendationAcceptedListener()
    {
        try {
            this.acceptedCount = FileHelper.readAcceptedRejectedFile("accepted",
                    SettingsUtil.getApplicationHome().getPath() + "/accRej.txt");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(acceptedCount.toString());
    }
    
    @Override
    public void onApplicationEvent(RecommendationAcceptedEvent aEvent)
    {   
        String regex = aEvent.getConfExpl().get().replace("Based on the regex ", "");
        String featureName = aEvent.getFeature().getUiName();
        Map<String, Integer> accCountRegex = acceptedCount.get(featureName);
        accCountRegex.merge(regex, 1, Integer::sum);
        System.out.println("accepted " + regex);
    }
}
