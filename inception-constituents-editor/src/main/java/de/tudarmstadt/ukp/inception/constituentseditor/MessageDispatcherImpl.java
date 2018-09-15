/*
 * Thesis : Collaborative Web-based Tool for the Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 */

package de.tudarmstadt.ukp.inception.constituentseditor;

import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.CONFLICT;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.MAX_LATENCY_LAG;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.NOTIFICATION;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.REQUEST_ADD_EDGE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.REQUEST_ADD_NODE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.REQUEST_ADD_NODE_IN_BETWEEN;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.REQUEST_CREATE_NEW;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.REQUEST_DELETE_EDGE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.REQUEST_DELETE_NODE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.REQUEST_EDIT_EDGE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.REQUEST_EDIT_NODE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.REQUEST_GET_FILES;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.REQUEST_INIT;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.UPDATE_POS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.protocol.ws.api.IWebSocketConnection;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.TextMessage;
import org.apache.wicket.protocol.ws.api.registry.IKey;
import org.apache.wicket.protocol.ws.api.registry.IWebSocketConnectionRegistry;
import org.apache.wicket.protocol.ws.api.registry.SimpleWebSocketConnectionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * This class act as a intermediate between the client requests and the server processing
 * 
 * @author Asha Joshi
 */
@Component
public class MessageDispatcherImpl implements MessageDispatcher
{
    /** Queue to store requests received concurrently */
    private Queue<String> requestQueue;
    /** Wait state ( handling concurrent requests ) */
    private boolean waitInProgress = false;
    /** Operations for carrying out the linguistic analysis */
    private ConstituentEditorActionHandler actionHandler;
    /** stores information related to conflicting requests */
    private JSONObject useralert;
    /** Maps artifact with connections to client working on it */
    private Map<String, List<IWebSocketConnection>> clientsRegistry;
    /** Maps lowest latency among the clients working on the same artifact */
    private Map<String, Float> leastLatency;

    /**
     * AnnotationPageController constructor
     */
    @Autowired
    public MessageDispatcherImpl(ConstituentEditorActionHandler aActionHandler)
    {
        actionHandler = aActionHandler;
        
        requestQueue = new ConcurrentLinkedQueue<String>();
        useralert = new JSONObject();
        clientsRegistry = new HashMap<String, List<IWebSocketConnection>>();
        leastLatency = new HashMap<String, Float>();
    }

    /**
     * Get all the connected clients from WebSocketConnectionRegistry
     * 
     * @return a collection with all connections
     */
    @Override
    public Collection<IWebSocketConnection> getConnectedClients()
    {
        IWebSocketConnectionRegistry registry = new SimpleWebSocketConnectionRegistry();
        return registry.getConnections(Application.get());
    }

    /**
     * Multicast a message
     * 
     * @param message
     *            message to be multicasted
     * @param filename
     *            the artifact ID to which message belongs
     */
    @Override
    public void sendToAllConnectedClients(String message, String filename) throws Exception
    {
        JSONObject item = new JSONObject();

        // Any editing requests rejected due to conflicting actions are communicated
        if (useralert.length() != 0) {
            item.put(CONFLICT, useralert);
        }

        if (StringUtils.isNotBlank(filename)) {
            // Get a list of connections to clients working on the given artifact
            List<IWebSocketConnection> allconnection = clientsRegistry.get(filename);

            if (allconnection != null) {
                for (IWebSocketConnection conn : allconnection) {
                    // communicate the message to selected clients
                    conn.sendMessage(message);
                    // communicate the conflict related information
                    if (item.length() != 0) {
                        conn.sendMessage(item.toString());
                    }
                }
            }
        }

        // empty the conflict alert message after communicating to clients
        if (useralert.length() != 0) {
            useralert = new JSONObject();
        }
    }

    /**
     * This method maintains the least response time and checks for slow connections.
     * 
     * @param filename
     *            the artifact ID
     * @param sessionID
     *            session ID
     * @param clientKey
     *            client key
     * @param latency
     *            latency value
     * @return false if connection is slow otherwise return true
     */
    private boolean checkConnectionSpeed(String filename, String sessionID, IKey clientKey,
            String latency)
    {
        if (Float.parseFloat(latency) >= 0) {
            // If no client previously were working on this document
            if (!leastLatency.containsKey(filename)) {
                // Map the received latency to the document
                leastLatency.put(filename, Float.parseFloat(latency));
                return true;
            }
            else {
                // If there are already clients working on same document
                if (leastLatency.get(filename) > Float.parseFloat(latency)) {
                    // check if received latency is the less than minimum latency
                    // set the received latency as minimum latency
                    leastLatency.put(filename, Float.parseFloat(latency));
                    return true;
                }
                else if ((leastLatency.get(filename) < Float.parseFloat(latency))
                        && (leastLatency.get(filename) + MAX_LATENCY_LAG < Float
                                .parseFloat(latency))) {
                    // If received latency is slower than maximum accepted latency then return false
                    // to indicate user
                    return false;
                }
                else {
                    return true;
                }
            }
        }
        else {
            return true;
        }
    }

    /**
     * Add client connection to clientsRegistry
     * 
     * @param filename
     *            artifact ID
     * @param sessionID
     *            session ID
     * @param clientKey
     *            client key
     */
    private void addClient(String filename, String sessionID, IKey clientKey)
    {

        IWebSocketConnectionRegistry registry = new SimpleWebSocketConnectionRegistry();

        // Get the connection to client using session ID and client key
        IWebSocketConnection wsConnection = registry.getConnection(Application.get(), sessionID,
                clientKey);

        // If artifact not added to clientsRegistry
        if (!clientsRegistry.containsKey(filename)) {
            List<IWebSocketConnection> allconnection = new ArrayList<IWebSocketConnection>();
            allconnection.add(wsConnection);
            // Add an entry mapping the artifact ID with given client connection
            clientsRegistry.put(filename, allconnection);
        }

        Iterator<Entry<String, List<IWebSocketConnection>>> iter = clientsRegistry.entrySet()
                .iterator();
        // Iterate over entries in clientsRegistry and update them
        while (iter.hasNext()) {
            Map.Entry<String, List<IWebSocketConnection>> entry = iter.next();
            if (entry.getKey().equals(filename)) {
                List<IWebSocketConnection> allconnection = entry.getValue();
                // If given connection is currently not mapped with the artifact ID, add it to
                // connections list
                if (!allconnection.contains(wsConnection)) {
                    allconnection.add(wsConnection);
                }
                clientsRegistry.replace(entry.getKey(), allconnection);
            }
            else {
                List<IWebSocketConnection> allconnection = entry.getValue();
                // remove given connection from lists mapped to other artifacts
                if (allconnection.contains(wsConnection)) {
                    allconnection.remove(wsConnection);
                }

                // In case if list is empty then remove the entry from clientsRegistry and the
                // latency value saved for that artifact ID
                if (allconnection.isEmpty()) {
                    iter.remove();
                    if (leastLatency.containsKey(entry.getKey())) {
                        leastLatency.remove(entry.getKey());
                    }
                }
                else {
                    clientsRegistry.replace(entry.getKey(), allconnection);
                }
            }
        }
    }

    /**
     * This method remove a connection from the clientsRegistry corresponding which disconnected
     * 
     * @param sessionID
     *            session ID
     * @param clientKey
     *            client key
     */
    @Override
    public void removeClient(String sessionID, IKey clientKey) throws Exception
    {
        IWebSocketConnectionRegistry registry = new SimpleWebSocketConnectionRegistry();

        // get the connection using session ID and client key
        IWebSocketConnection wsConnection = registry.getConnection(Application.get(), sessionID,
                clientKey);

        Iterator<Entry<String, List<IWebSocketConnection>>> iter = clientsRegistry.entrySet()
                .iterator();
        // Iterate over the map clientsRegistry and remove the connection
        while (iter.hasNext()) {
            Map.Entry<String, List<IWebSocketConnection>> entry = iter.next();
            List<IWebSocketConnection> allconnection = entry.getValue();
            if (allconnection.contains(wsConnection)) {
                allconnection.remove(wsConnection);
            }

            // In case if list is empty then remove the entry from clientsRegistry and the latency
            // value saved for that artifact ID
            if (allconnection.isEmpty()) {
                iter.remove();
                if (leastLatency.containsKey(entry.getKey())) {
                    leastLatency.remove(entry.getKey());
                }
            }
            else {
                clientsRegistry.replace(entry.getKey(), allconnection);
            }
        }
    }

    /**
     * This method checks for slow connection, synchronization of client state and processes the
     * request
     * 
     * @param handler
     *            the handler to the client requesting
     * @param msg
     *            the message received
     */
    @Override
    public void handleMessage(WebSocketRequestHandler handler, TextMessage msg, String sessionID,
            IKey clientKey)
        throws Exception
    {
        JSONObject actualObj = new JSONObject(msg.getText());

        Long messagetimestamp = System.currentTimeMillis(); // get server system timestamp

        // Add connection to the map clientsRegistry, if not yet added (for "init" message)
        if (actualObj.get("action").toString().equals(REQUEST_INIT)) {
            addClient(actualObj.get("file").toString(), sessionID, clientKey);
        }

        // Process miscellaneous requests immediately
        if (actualObj.get("action").toString().equals(REQUEST_GET_FILES)
                || actualObj.get("action").toString().equals(REQUEST_INIT)
                || actualObj.get("action").toString().equals(UPDATE_POS)
                || actualObj.get("action").toString().equals(REQUEST_CREATE_NEW)) {
            executeMiscRequest(handler, msg.getText());
        }
        else {
            // Editing requests

            // check is connection is slow
            if (actualObj.has("latency")
                    && StringUtils.isNotBlank(actualObj.get("latency").toString())) {
                if (!checkConnectionSpeed(actualObj.get("file").toString(), sessionID, clientKey,
                        actualObj.get("latency").toString())) {
                    JSONObject item = new JSONObject();
                    item.put(NOTIFICATION, "Your connection is slow");
                    handler.push(item.toString());
                }
            }

            String selectedFile = actualObj.get("file").toString();
            // get the file name and sentence ID
            String[] splitFileDetails = selectedFile.split("#");
            if (splitFileDetails.length == 2) {
                String selectedFileName = splitFileDetails[0];
                int sentenceAddress = Integer.parseInt(splitFileDetails[1]);
                // version check before processing the request further
                if (actionHandler.verifyVersion(selectedFileName,
                        Integer.parseInt(actualObj.get("version").toString()), sentenceAddress)) {
                    // Add receive timestamp to the request after verification of client data state
                    actualObj.put("timestamp", messagetimestamp);
                    // Add the request to the queue
                    requestQueue.add(actualObj.toString());
                    // If wait not in progress
                    if (!waitInProgress) {
                        Application application = Application.get();
                        // wait for some time before processing requests
                        waitInProgress = true;
                        schedulewait(application);
                    }
                }
                else {
                    JSONObject item = new JSONObject();
                    item.put(NOTIFICATION,
                            "Annotation data not synchronized with server. Performing synchronization..");
                    handler.push(item.toString());
                    // send complete analysis data for synchronization
                    String data = actionHandler.render(selectedFile, "");
                    if (StringUtils.isNotBlank(data)) {
                        handler.push(data);
                    }
                }
            }
        }
    }

    /**
     * Initiate request processing after wait finished and conflicts are resolved
     */
    @Override
    public void waitFinished(Application application) throws Exception
    {
        if (!Application.exists()) {
            ThreadContext.setApplication(application);
        }

        // If only one request received
        if (requestQueue.size() == 1) {
            // execute request
            executeRequest(requestQueue.poll());
        }
        else if (requestQueue.size() > 1) {
            // If multiple requests received in similar times, initiate conflict detection and
            // resolution
            // prioritize requests
            priortizeQueue();

            // Execute all requests from the queue
            while (requestQueue.peek() != null) {
                executeRequest(requestQueue.poll());
            }
        }
        waitInProgress = false;
    }

    /**
     * Handling concurrency control: conflict detection and resolution
     */
    private void priortizeQueue() throws Exception
    {
        System.out.println("Queue after conflict resolution: " + requestQueue.toString());

        Multimap<String, String> conflictingaction = ArrayListMultimap.create();
        Collection<String> toDelete = new ArrayList<String>();

        // Add all the request in a map with the tree element being edited as the key and the
        // request as the value
        for (String request : requestQueue) {
            JSONObject actualObj = new JSONObject(request);
            switch (actualObj.get("action").toString()) {
            case REQUEST_ADD_NODE:
                conflictingaction.put(actualObj.get("childnode").toString(), request);
                break;
            case REQUEST_ADD_EDGE:
                conflictingaction.put(actualObj.get("childid").toString(), request);
                break;
            case REQUEST_ADD_NODE_IN_BETWEEN:
                String[] splitedgeID1 = actualObj.get("edgeid").toString().split("_");
                // get the Parent node ID from Edge ID
                conflictingaction.put(splitedgeID1[1], request);
                conflictingaction.put(splitedgeID1[2], request);
                break;
            case REQUEST_EDIT_NODE:
                conflictingaction.put(actualObj.get("nodeid").toString(), request);
                break;
            case REQUEST_EDIT_EDGE:
                String[] splitedgeID2 = actualObj.get("nodeid").toString().split("_");
                // get the Parent node ID from Edge ID
                conflictingaction.put(splitedgeID2[1], request);
                conflictingaction.put(splitedgeID2[2], request);
                break;
            case REQUEST_DELETE_NODE:
                conflictingaction.put(actualObj.get("nodeid").toString(), request);
                break;
            case REQUEST_DELETE_EDGE:
                // get the Parent and Child node ID from Edge ID
                String[] splitedgeID3 = actualObj.get("edgeid").toString().split("_");
                conflictingaction.put(splitedgeID3[1], request);
                conflictingaction.put(splitedgeID3[2], request);
                break;
            }
        }

        for (Entry<String, Collection<String>> entry : conflictingaction.asMap().entrySet()) {
            System.out.println("Address: " + entry.getKey() + " is used in "
                    + entry.getValue().size() + " request");
            // Check for conflicts for elements being edited in multiple requests
            if (entry.getValue().size() > 1) {
                Collection<String> filteredActions = entry.getValue();
                String previousReq = "";
                for (Iterator<String> iterator = filteredActions.iterator(); iterator.hasNext();) {
                    // resolve two requests at a time
                    if (StringUtils.isBlank(previousReq)) {
                        previousReq = iterator.next();
                    }
                    else {
                        JSONObject request1Obj = new JSONObject(previousReq);
                        JSONObject request2Obj = new JSONObject(iterator.next());
                        Long request1Time = Long.valueOf(request1Obj.get("timestamp").toString());
                        Long request2Time = Long.valueOf(request2Obj.get("timestamp").toString());
                        // If both requests are of same type then select the one with earlier time
                        // and reject the other
                        if (request1Obj.get("action").toString()
                                .equals(request2Obj.get("action").toString())) {
                            if (request1Time.compareTo(request2Time) == 0) {
                                // If both conflicting requests received at same time, accept one
                                // randomly
                                useralert.put(entry.getKey(), request2Obj.get("action").toString());
                                toDelete.add(request2Obj.toString());
                                previousReq = request1Obj.toString();
                                break;
                            }
                            else if (request1Time.compareTo(request2Time) == 1) {
                                useralert.put(entry.getKey(), request1Obj.get("action").toString());
                                toDelete.add(request1Obj.toString());
                                previousReq = request2Obj.toString();
                            }
                            else {
                                useralert.put(entry.getKey(), request2Obj.get("action").toString());
                                toDelete.add(request2Obj.toString());
                                previousReq = request1Obj.toString();
                            }
                        }
                        else if ((request1Obj.get("action").toString().equals(REQUEST_ADD_NODE)
                                || request1Obj.get("action").toString().equals(REQUEST_EDIT_NODE)
                                || request1Obj.get("action").toString().equals(REQUEST_ADD_EDGE)
                                || request1Obj.get("action").toString().equals(REQUEST_EDIT_EDGE)
                                || request1Obj.get("action").toString()
                                        .equals(REQUEST_ADD_NODE_IN_BETWEEN)
                                || request1Obj.get("action").toString()
                                        .equals(REQUEST_DELETE_EDGE))) {
                            if (request2Obj.get("action").toString().equals(REQUEST_DELETE_NODE)) {
                                // reject second request, as deleteNode request has lowest priority
                                useralert.put(entry.getKey(), request2Obj.get("action").toString());
                                toDelete.add(request2Obj.toString());
                                previousReq = request1Obj.toString();
                            }
                            else if ((request1Obj.get("action").toString().equals(REQUEST_ADD_NODE)
                                    && request2Obj.get("action").toString()
                                            .equals(REQUEST_ADD_EDGE))
                                    || (request1Obj.get("action").toString()
                                            .equals(REQUEST_ADD_EDGE)
                                            && request2Obj.get("action").toString()
                                                    .equals(REQUEST_ADD_NODE))
                                    || (request1Obj.get("action").toString()
                                            .equals(REQUEST_EDIT_EDGE)
                                            && request2Obj.get("action").toString()
                                                    .equals(REQUEST_ADD_NODE_IN_BETWEEN))
                                    || (request1Obj.get("action").toString()
                                            .equals(REQUEST_ADD_NODE_IN_BETWEEN)
                                            && request2Obj.get("action").toString()
                                                    .equals(REQUEST_EDIT_EDGE))) {
                                // Cases where requests have similar priorities, select one with
                                // earlier time to be selected
                                if (request1Time.compareTo(request2Time) == 0) {
                                    useralert.put(entry.getKey(),
                                            request2Obj.get("action").toString());
                                    if (!toDelete.contains(request2Obj.toString())) {
                                        toDelete.add(request2Obj.toString());
                                    }
                                    previousReq = request1Obj.toString();
                                    break;
                                }
                                else if (request1Time.compareTo(request2Time) == 1) {
                                    useralert.put(entry.getKey(),
                                            request1Obj.get("action").toString());
                                    if (!toDelete.contains(request1Obj.toString())) {
                                        toDelete.add(request1Obj.toString());
                                    }
                                    previousReq = request2Obj.toString();
                                }
                                else {
                                    useralert.put(entry.getKey(),
                                            request2Obj.get("action").toString());
                                    if (!toDelete.contains(request2Obj.toString())) {
                                        toDelete.add(request2Obj.toString());
                                    }
                                    previousReq = request1Obj.toString();
                                }
                            }
                            else if (request2Obj.get("action").toString()
                                    .equals(REQUEST_DELETE_EDGE)
                                    && (request1Obj.get("action").toString()
                                            .equals(REQUEST_EDIT_EDGE)
                                            || request1Obj.get("action").toString()
                                                    .equals(REQUEST_ADD_NODE_IN_BETWEEN)
                                            || request1Obj.get("action").toString()
                                                    .equals(REQUEST_EDIT_NODE))) {
                                // Reject deleteEdge request as it has lower priority than editEdge,
                                // addNodeBW and editNode
                                useralert.put(entry.getKey(), request2Obj.get("action").toString());
                                if (!toDelete.contains(request2Obj.toString())) {
                                    toDelete.add(request2Obj.toString());
                                }
                                previousReq = request1Obj.toString();
                            }
                            else if (request1Obj.get("action").toString()
                                    .equals(REQUEST_DELETE_EDGE)
                                    && (request2Obj.get("action").toString()
                                            .equals(REQUEST_EDIT_EDGE)
                                            || request2Obj.get("action").toString()
                                                    .equals(REQUEST_ADD_NODE_IN_BETWEEN)
                                            || request2Obj.get("action").toString()
                                                    .equals(REQUEST_EDIT_NODE))) {
                                // Reject deleteEdge request as it has lower priority than editEdge,
                                // addNodeBW and editNode
                                useralert.put(entry.getKey(), request1Obj.get("action").toString());
                                if (!toDelete.contains(request1Obj.toString())) {
                                    toDelete.add(request1Obj.toString());
                                }
                                previousReq = request1Obj.toString();
                            }
                        }
                        else if (request2Obj.get("action").toString().equals(REQUEST_DELETE_EDGE)
                                && (request1Obj.get("action").toString().equals(REQUEST_EDIT_EDGE)
                                        || request1Obj.get("action").toString()
                                                .equals(REQUEST_ADD_NODE_IN_BETWEEN)
                                        || request1Obj.get("action").toString()
                                                .equals(REQUEST_EDIT_NODE))) {
                            // Reject deleteEdge request as it has lower priority than editEdge,
                            // addNodeBW and editNode
                            useralert.put(entry.getKey(), request2Obj.get("action").toString());
                            if (!toDelete.contains(request2Obj.toString())) {
                                toDelete.add(request2Obj.toString());
                            }
                            previousReq = request1Obj.toString();
                        }
                        else if (request1Obj.get("action").toString().equals(REQUEST_DELETE_NODE)) {
                            // Reject deleteNode request as it has lowest priority than others
                            useralert.put(entry.getKey(), request1Obj.get("action").toString());
                            if (!toDelete.contains(request1Obj.toString())) {
                                toDelete.add(request1Obj.toString());
                            }
                            previousReq = request2Obj.toString();
                        }
                        else if (request1Obj.get("action").toString().equals(REQUEST_DELETE_EDGE)) {
                            // Reject deleteEdge request as it has lowest priority than others
                            // (after deleNode)
                            useralert.put(entry.getKey(), request1Obj.get("action").toString());
                            if (!toDelete.contains(request1Obj.toString())) {
                                toDelete.add(request1Obj.toString());
                            }
                            previousReq = request2Obj.toString();
                        }
                    }
                }
            }
        }
        System.out.println("rejected request" + toDelete.toString());
        toDelete.forEach((del) -> {
            requestQueue.remove(del);

        });
        System.out.println("Queue after conflict resolution: " + requestQueue.toString());
    }

    /**
     * Execute requests not related to editing
     * 
     * @param handler
     *            holds the connection to the client requesting
     * @param msg
     *            request message
     */
    private void executeMiscRequest(WebSocketRequestHandler handler, String msg) throws Exception
    {
        // form object-value pairs of the message received
        JSONObject actualObj = new JSONObject(msg); 
        // get the request type
        String action = actualObj.get("action").toString();
        // store the filename and the sentence ID (artifact)
        String file_name = ""; 
        // store data after processing the request
        String data = ""; 

        switch (action) {
        // Get list of annotation files
        case REQUEST_GET_FILES:
            // get the list of files from filesystem
            data = actionHandler.getFiles();
            // send the response
            handler.push(data);
            break;
        // Start annotation "init" request
        case REQUEST_INIT:
            file_name = actualObj.get("file").toString();
            // get complete analysis data for the given artifact
            data = actionHandler.render(file_name, "");
            if (StringUtils.isNotBlank(data)) {
                // send the response
                handler.push(data);
            }
            break;
        // Create new corpus for annotation
        case REQUEST_CREATE_NEW:
            file_name = actualObj.get("file").toString();
            String corpusdata = actualObj.get("text").toString();
            // create a file with given sentence for annotation
            data = actionHandler.createCorpus(file_name, corpusdata);
            // send the response
            handler.push(data);
            break;
        // Mouse poiner location update
        case UPDATE_POS:
            // Multicast mouse pointer location
            sendToAllConnectedClients(msg, actualObj.get("selectedfile").toString());
            break;
        }
    }

    /**
     * Execute annotation editing requests
     * 
     * @param msg
     *            request received from client
     * @throws Exception
     */
    private void executeRequest(String msg) throws Exception
    {
        JSONObject actualObj = new JSONObject(msg); // form object-value pairs of the message
                                                    // received
        String annotationdata = ""; // store the analysis data after processing
        String action = ""; // store the editing request type
        action = actualObj.get("action").toString(); // get the request type
        String file_name = ""; // store the artifact ID ( filename + sentence ID)
        String selectednodeid = ""; // store the element ID which is being edited
        String user = actualObj.get("user").toString(); // store the user name
        int clientversion = Integer.parseInt(actualObj.get("version").toString()); // get the
                                                                                   // version at the
                                                                                   // client
        int renderingmode = Mode.MODE_FULL.ordinal(); // get the selected communication protocol
        if (actualObj.get("mode") != null) {
            renderingmode = Integer.parseInt(actualObj.get("mode").toString()) == Mode.MODE_FULL
                    .ordinal() ? Mode.MODE_FULL.ordinal() : Mode.MODE_INCREMENTAL.ordinal();
        }

        switch (action) {
        // Request - Add a new node
        case REQUEST_ADD_NODE:
            if (StringUtils.isNotBlank(actualObj.get("childnode").toString())) {
                selectednodeid = actualObj.get("childnode").toString();
                String newlabel = actualObj.get("label").toString();
                file_name = actualObj.get("file").toString();
                // request addition of a new node and multicast message
                annotationdata = actionHandler.addNode(file_name, renderingmode, selectednodeid,
                        newlabel, clientversion);
                if (StringUtils.isNotBlank(annotationdata)) {
                    multicastMSG(annotationdata, msg, file_name, user);
                }
            }
            break;
        // Request edit existing node
        case REQUEST_EDIT_NODE:
            if (StringUtils.isNotBlank(actualObj.get("nodeid").toString())) {
                selectednodeid = actualObj.get("nodeid").toString();
                String editedlabel = actualObj.get("newlabel").toString();
                file_name = actualObj.get("file").toString();
                // request editing of an edge and multicast message
                annotationdata = actionHandler.editNode(file_name, renderingmode, selectednodeid,
                        editedlabel, clientversion);
                if (StringUtils.isNotBlank(annotationdata)) {
                    multicastMSG(annotationdata, msg, file_name, user);
                }
            }
            break;
        // Request delete existing node
        case REQUEST_DELETE_NODE:
            if (StringUtils.isNotBlank(actualObj.get("nodeid").toString())) {
                selectednodeid = actualObj.get("nodeid").toString();
                file_name = actualObj.get("file").toString();
                // request deletion of a new node and multicast message
                annotationdata = actionHandler.deleteNode(file_name, renderingmode, selectednodeid,
                        clientversion);
                if (StringUtils.isNotBlank(annotationdata)) {
                    multicastMSG(annotationdata, msg, file_name, user);
                }
            }
            break;
        // Request add edge between two nodes
        case REQUEST_ADD_EDGE:
            if (StringUtils.isNotBlank(actualObj.get("parentid").toString())
                    && StringUtils.isNotBlank(actualObj.get("childid").toString())) {
                selectednodeid = actualObj.get("parentid").toString();
                String childnode = actualObj.get("childid").toString();
                file_name = actualObj.get("file").toString();
                // request addition of an edge and multicast message
                annotationdata = actionHandler.addEdge(file_name, renderingmode, selectednodeid,
                        childnode, clientversion);
                if (StringUtils.isNotBlank(annotationdata)) {
                    multicastMSG(annotationdata, msg, file_name, user);
                }
            }
            break;
        // Request editing existing edge
        case REQUEST_EDIT_EDGE:
            if (StringUtils.isNotBlank(actualObj.get("nodeid").toString())) {
                selectednodeid = actualObj.get("nodeid").toString();
                String newEdgelabel = actualObj.get("edgelabel").toString();
                file_name = actualObj.get("file").toString();
                // request editing of an edge and multicast message
                annotationdata = actionHandler.editEdge(file_name, renderingmode, selectednodeid,
                        newEdgelabel, clientversion);
                multicastMSG(annotationdata, msg, file_name, user);
            }
            break;
        // Request Delete existing edge
        case REQUEST_DELETE_EDGE:
            if (StringUtils.isNotBlank(actualObj.get("edgeid").toString())) {
                selectednodeid = actualObj.get("edgeid").toString();
                file_name = actualObj.get("file").toString();
                // request deletion of an edge and multicast message
                annotationdata = actionHandler.deleteEdge(file_name, renderingmode, selectednodeid,
                        clientversion);
                multicastMSG(annotationdata, msg, file_name, user);
            }
            break;
        // Request Add node in between
        case REQUEST_ADD_NODE_IN_BETWEEN:
            if (StringUtils.isNotBlank(actualObj.get("edgeid").toString())) {
                selectednodeid = actualObj.get("edgeid").toString();
                file_name = actualObj.get("file").toString();
                String newNodeabel = actualObj.get("nodelabel").toString();
                // request addition of a new node between selected parent and child node and
                // multicast message
                annotationdata = actionHandler.addNodeInBetween(file_name, renderingmode,
                        selectednodeid, newNodeabel, clientversion);
                multicastMSG(annotationdata, msg, file_name, user);
            }
            break;
        }
    }

    /**
     * This method calculates and add the processing time and forwards message for multicasting
     * 
     * @param tosend
     *            the response message to be sent
     * @param received
     *            the request message received
     * @param file_name
     *            the artifact ID
     */
    private void multicastMSG(String tosend, String received, String file_name, String user)
        throws Exception
    {
        // form object-value pairs of message to be sent
        JSONObject receivedOBJ = new JSONObject(received);
        
        // calculate the processing time by taking the current time and subtracting the time at
        // which message received
        long processingtime = System.currentTimeMillis() - receivedOBJ.getLong("timestamp");

        // form object-value pairs of message received
        JSONObject tosendOBJ = new JSONObject(tosend);
        // add the client's sent timestamp to the response
        tosendOBJ.put("sentTimestamp", receivedOBJ.getLong("sendUnixTime"));
        // add the processing time to the response message
        tosendOBJ.put("processingtime", processingtime);
        // add the user name to the response message
        tosendOBJ.put("user", user);
        // send message to all clients working on given artifact
        sendToAllConnectedClients(tosendOBJ.toString(), file_name); 
    }

    /**
     * Add a wait of 500ms for handling concurrent requests from multiple clients
     */
    @Override
    public void schedulewait(Application application)
    {
        Timer timer = new Timer();
        timer.schedule(new TimerTask()
        {

            @Override
            public void run()
            {
                try {
                    waitFinished(application);
                }
                catch (Exception e) {
                    System.out.println(e.getCause());
                    e.printStackTrace();
                }
            }
        }, 1000);
    }
}
