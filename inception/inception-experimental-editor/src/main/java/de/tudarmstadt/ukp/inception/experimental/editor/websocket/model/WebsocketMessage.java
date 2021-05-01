package de.tudarmstadt.ukp.inception.experimental.editor.websocket.model;

import org.apache.wicket.markup.html.WebMarkupContainer;

public class WebsocketMessage
{
    private String websocketMessage;

    public WebsocketMessage(String aWebsocketMessage)
    {
        this.websocketMessage = aWebsocketMessage;
    }

    public String getWebsocketMessage()
    {
        return websocketMessage;
    }
}
