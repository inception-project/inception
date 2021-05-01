package de.tudarmstadt.ukp.inception.experimental.editor.websocket;

import org.springframework.context.ApplicationEvent;

import java.security.Principal;
import java.util.List;

public interface WebsocketController
{
    /***
     * Push messages on received application events to named user
     */
    public void onApplicationEvent(ApplicationEvent aEvent);

    /**
     * Return path of topic that a client can subscribe to
     * @return
     */
    public String getTopicChannel();

    /**
     * Return the most recent logged events to the subscribing client
     * @param aPrincipal the subscribing client
     * @return the most recent events
     */
}
