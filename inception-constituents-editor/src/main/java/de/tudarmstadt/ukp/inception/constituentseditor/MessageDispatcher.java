package de.tudarmstadt.ukp.inception.constituentseditor;

import java.util.Collection;

import org.apache.wicket.Application;
import org.apache.wicket.protocol.ws.api.IWebSocketConnection;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.TextMessage;
import org.apache.wicket.protocol.ws.api.registry.IKey;

public interface MessageDispatcher
{

    /**
     * Get all the connected clients from WebSocketConnectionRegistry
     * 
     * @return a collection with all connections
     */
    Collection<IWebSocketConnection> getConnectedClients();

    /**
     * Multicast a message
     * 
     * @param message
     *            message to be multicasted
     * @param filename
     *            the artifact ID to which message belongs
     */
    void sendToAllConnectedClients(String message, String filename) throws Exception;

    /**
     * This method remove a connection from the clientsRegistry corresponding which disconnected
     * 
     * @param sessionID
     *            session ID
     * @param clientKey
     *            client key
     */
    void removeClient(String sessionID, IKey clientKey) throws Exception;

    /**
     * This method checks for slow connection, synchronization of client state and processes the
     * request
     * 
     * @param handler
     *            the handler to the client requesting
     * @param msg
     *            the message received
     */
    void handleMessage(WebSocketRequestHandler handler, TextMessage msg, String sessionID,
            IKey clientKey)
        throws Exception;

    /**
     * Initiate request processing after wait finished and conflicts are resolved
     */
    void waitFinished(Application application) throws Exception;

    /**
     * Add a wait of 500ms for handling concurrent requests from multiple clients
     */
    void schedulewait(Application application);

}
