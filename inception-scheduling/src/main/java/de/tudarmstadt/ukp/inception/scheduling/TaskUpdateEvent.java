package de.tudarmstadt.ukp.inception.scheduling;


public class TaskUpdateEvent
{
    private final String username;
    private final double progress;
    private final TaskState state;
    private final long recommenderId;
    private final boolean active;
    
    //TODO: add evaluationResult?
    
    public TaskUpdateEvent(String aName, TaskState aState, double aProgress, long aRecommenderId,
            boolean aActive)
    {
        username = aName;
        state = aState;
        progress = aProgress;
        recommenderId = aRecommenderId;
        active = aActive;
    }

    public TaskUpdateEvent(String aUsername, TaskState aState, long aRecommenderId)
    {
        this(aUsername, aState, 1, aRecommenderId, true);
    }

    public String getUsername()
    {
        return username;
    }

    public double getProgress()
    {
        return progress;
    }

    public TaskState getState()
    {
        return state;
    }

    public long getRecommenderId()
    {
        return recommenderId;
    }
    
    public boolean isActive()
    {
        return active;
    }
}
