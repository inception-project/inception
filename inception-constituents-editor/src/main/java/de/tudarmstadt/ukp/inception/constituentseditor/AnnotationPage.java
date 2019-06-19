/*
 * Thesis : Collaborative Web-based Tool for the Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 */

package de.tudarmstadt.ukp.inception.constituentseditor;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.ClosedMessage;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.TextMessage;
import org.apache.wicket.protocol.ws.api.registry.IKey;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

/**
 * Class AnnotationPage An instance of this class created whenever a client accesses the
 * AnnotationPage This class mainly adds WebSocketBehaviour to a web page and handles WebSocket
 * events
 * 
 * @author Asha Joshi
 */
@MountPath(value = "/syntacticannotation.html")
public class AnnotationPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 1L; 
    
    /** Store the session ID which is needed to get the connection to the client */
    private String sessionID; 
    
    /** store the client key which is needed to get the connection to the client */
    private IKey clientKey; 

    private @SpringBean MessageDispatcher dispatcher;
    
    /**
     * AnnotationPage Constructor
     */
    public AnnotationPage()
    {
        super();
        
        sessionID = new String();

        // Add WebSocketBehavior to the Annotation page
        add(new WebSocketBehavior()
        {
            private static final long serialVersionUID = 2L;

            /**
             * Handler to WebSocket message event triggered whenever a WebSocket message is received
             */
            @Override
            protected void onMessage(WebSocketRequestHandler handler, TextMessage message)
            {
                // For some unknown reason (maybe a tomcat or wicket bug?) the context classloader
                // seems to be set to the wrong loader
                // so we get no access all the classes from the webapp (only the ones from tomcat
                // itself!). Reset it to the loader of
                // the current class.
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                super.onMessage(handler, message);
                try {
                    // Handle message at application level not separately for each page
                    // Session ID and client key received during connection establishment are sent
                    // to identify the connection
                    dispatcher.handleMessage(handler, message, sessionID, clientKey);

                }
                catch (ClassNotFoundException e) {
                    // if the JCas could not be initialized)
                    System.out.println(e.getCause());
                    e.printStackTrace();
                }
                catch (UIMAException e) {
                    // if the JCas could not be initialized)
                    System.out.println(e.getCause());
                    e.printStackTrace();
                }
                catch (JsonParseException e) {
                    // if underlying input contains invalid content of type JsonParser supports
                    // (JSON for default case))
                    System.out.println(e.getCause());
                    e.printStackTrace();
                }
                catch (JsonProcessingException e) {
                    System.out.println(e.getCause());
                    e.printStackTrace();
                }
                catch (CASRuntimeException e) {
                    // When called out of sequence
                    System.out.println(e.getCause());
                    e.printStackTrace();
                }
                catch (CASAdminException e) {
                    System.out.println(e.getCause());
                    e.printStackTrace();
                }
                catch (UIMA_IllegalStateException e) {
                    System.out.println(e.getCause());
                    e.printStackTrace();
                }
                catch (IOException e) {
                    // if an I/O failure occurs
                    System.out.println(e.getCause());
                    e.printStackTrace();
                }
                catch (NoSuchElementException noelt) {
                    // if this queue is empty
                    System.out.println(noelt.getCause());
                    noelt.printStackTrace();
                }
                catch (UnsupportedOperationException unsupported) {
                    // if invalid index used remove(index)
                    System.out.println(unsupported.getCause());
                    unsupported.printStackTrace();
                }
                catch (Exception e) {
                    System.out.println(e.getCause());
                    e.printStackTrace();
                }
            }

            /**
             * Handler to WebSocket open event triggered whenever WebSocet conneciton is established
             */
            @Override
            protected void onConnect(ConnectedMessage message)
            {
                super.onConnect(message);
                // Store the session ID on connection establishment
                sessionID = message.getSessionId();
                // Store the client key on connection establishment
                clientKey = message.getKey();
                System.out.println("new client connected: "
                        + dispatcher.getConnectedClients().size() + "\n");
            }

            /**
             * Handler to WebSocket close event triggered whenever a WebSocket connection is closed
             */
            @Override
            protected void onClose(ClosedMessage message)
            {
                // Remove the client from the map of all connections, whenever a connection is
                // closed
                try {
                    dispatcher.removeClient(message.getSessionId(), message.getKey());
                    System.out.println("client disconnected: "
                            + (dispatcher.getConnectedClients().size() - 1) + "\n");
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }

            }
        });
    }
}
