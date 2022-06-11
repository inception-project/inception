package de.tudarmstadt.ukp.inception.recommendation.tasks;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.scheduling.Task;

public abstract class RecommendationTask_ImplBase
    extends Task
{
    private final List<LogMessage> logMessages = new ArrayList<>();

    public RecommendationTask_ImplBase(Project aProject, String aTrigger)
    {
        super(aProject, aTrigger);
    }

    public RecommendationTask_ImplBase(User aUser, Project aProject, String aTrigger)
    {
        super(aUser, aProject, aTrigger);
    }

    public void inheritLog(List<LogMessage> aLogMessages)
    {
        logMessages.addAll(aLogMessages);
    }

    public void inheritLog(RecommendationTask_ImplBase aOther)
    {
        logMessages.addAll(aOther.logMessages);
    }

    public List<LogMessage> getLogMessages()
    {
        return logMessages;
    }

    public void info(String aFormat, Object... aValues)
    {
        logMessages.add(LogMessage.info(this, aFormat, aValues));
    }

    public void warn(String aFormat, Object... aValues)
    {
        logMessages.add(LogMessage.warn(this, aFormat, aValues));
    }

    public void error(String aFormat, Object... aValues)
    {
        logMessages.add(LogMessage.error(this, aFormat, aValues));
    }
}
