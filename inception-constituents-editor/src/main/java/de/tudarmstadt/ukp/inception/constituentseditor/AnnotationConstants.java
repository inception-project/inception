/*
 * Thesis : Collaborative Web-based Tool for the Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 */

package de.tudarmstadt.ukp.inception.constituentseditor;

/**
 * This class contains all the constant values used in the application.
 * 
 * @author Asha Joshi
 */
public class AnnotationConstants
{
    // Set of possible request types

    /** Request type to start annotation */
    public static final String REQUEST_INIT = "init";
    /** Request type to create new corpus */
    public static final String REQUEST_CREATE_NEW = "createNew";
    /** Request type to send mouse pointer location */
    public static final String UPDATE_POS = "pos";
    /** Request type to get the list of annotation files from filesystem */
    public static final String REQUEST_GET_FILES = "getAnnotationFiles";
    /** Request type to add a new node */
    public static final String REQUEST_ADD_NODE = "addNode";
    /** Request type to edit an existing node */
    public static final String REQUEST_EDIT_NODE = "editNode";
    /** Request type to delete an existing node */
    public static final String REQUEST_DELETE_NODE = "deleteNode";
    /** Request type to add an edge between two node */
    public static final String REQUEST_ADD_EDGE = "addEdge";
    /** Request type to delete an edge between two nodes */
    public static final String REQUEST_DELETE_EDGE = "deleteEdge";
    /** Request type to edit an existing edge */
    public static final String REQUEST_EDIT_EDGE = "editEdge";
    /** Request type to add a new node between two nodes */
    public static final String REQUEST_ADD_NODE_IN_BETWEEN = "addNodeBW";
    /** JSON object name for rendering mode / communication protocol */
    public static final String RENDERING_MODE = "mode";

    /**
     * Network with more than 100ms difference in response time with respect to the fastest
     * connection is considered slow
     */
    public static final int MAX_LATENCY_LAG = 100;

    // Notifications and alerts
    /** JSON object name for indicating the last updated element of the tree at frontend. */
    public static final String LAST_UPDATE = "lastUpdate";
    /** JSON object name for displaying information as notification to user */
    public static final String NOTIFICATION = "notification";
    /** JSON object name for sending information related to conflicts */
    public static final String CONFLICT = "conflict";

    // Storage related
    /** Location of filesystem */
    public static final String TARGET_BINARY_FILE = "/Users/bluefire/git/joshi-sourcecode/src/data/";
    /** the extension of files */
    public static final String BINARY_FILE_EXT = ".bin";

    // Applicable only for Iterative protocol
    // Some JSON objects names are preceded with numbers to define the order of rendering at client

    /** JSON object name for set of actions */
    public static final String ACTIONS_LIST = "actions";
    /** JSON object name for single action */
    public static final String ACTION = "action";
    /** JSON object name for delete edge action */
    public static final String ACTION_DELETE_EDGE = "1_deleteEdge";
    /** JSON object name for delete node action */
    public static final String ACTION_DELETE_NODE = "2_deleteNode";
    /** JSON object name for add node action */
    public static final String ACTION_ADD_NODE = "3_addNode";
    /** JSON object name for add edge action */
    public static final String ACTION_ADD_EDGE = "4_addEdge";
    /** JSON object name for update node action */
    public static final String ACTION_UPDATE_NODE = "updateNode";
    /** JSON object name for update edge action */
    public static final String ACTION_UPDATE_EDGE = "updateEdge";
}
