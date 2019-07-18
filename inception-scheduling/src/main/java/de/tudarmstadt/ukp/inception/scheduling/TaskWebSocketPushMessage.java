package de.tudarmstadt.ukp.inception.scheduling;

import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;

public class TaskWebSocketPushMessage
    implements IWebSocketPushMessage
{
    private static final long serialVersionUID = 1L;
    
    public TaskWebSocketPushMessage(double aProgress, TaskState aState, long aRecommenderId,
            boolean aActive)
    {
        // TODO Auto-generated constructor stub
    }

}
