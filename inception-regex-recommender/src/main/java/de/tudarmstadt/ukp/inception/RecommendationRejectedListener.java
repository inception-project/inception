package de.tudarmstadt.ukp.inception;
import java.io.IOException;
import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationRejectedEvent;

@Component
public class RecommendationRejectedListener
                implements ApplicationListener<RecommendationRejectedEvent>
{
    
    public Map<String, Map<String, Integer>> rejectedCount;


    public RecommendationRejectedListener()
    {
        try {
            this.rejectedCount = FileHelper.readAcceptedRejectedFile("rejected",
                    SettingsUtil.getApplicationHome().getPath() + "/accRej.txt");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public Map<String, Map<String, Integer>> getRejectedCount()
    {
        return rejectedCount;
    }

    public void setRejectedCount(Map<String, Map<String, Integer>> aRejectedCount)
    {
        this.rejectedCount = aRejectedCount;
    }

    @Override
    public void onApplicationEvent(RecommendationRejectedEvent aEvent)
    {   
        String regex = aEvent.getConfExpl().get().replace("Based on the regex ", "");
        String featureName = aEvent.getFeature().getUiName();
        Map<String, Integer> accCountRegex = rejectedCount.get(featureName);
        accCountRegex.merge(regex, 1, Integer::sum);
    }

}
