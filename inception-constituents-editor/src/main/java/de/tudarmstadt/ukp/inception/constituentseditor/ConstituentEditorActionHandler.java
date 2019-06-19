/*
 * Thesis : Collaborative Web-based Tool for the Annotation of Syntactic Parse Trees
 */

package de.tudarmstadt.ukp.inception.constituentseditor;

import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.ACTIONS_LIST;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.ACTION_ADD_EDGE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.ACTION_ADD_NODE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.ACTION_DELETE_EDGE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.ACTION_DELETE_NODE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.ACTION_UPDATE_EDGE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.ACTION_UPDATE_NODE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.BINARY_FILE_EXT;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.LAST_UPDATE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.NOTIFICATION;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.RENDERING_MODE;
import static de.tudarmstadt.ukp.inception.constituentseditor.AnnotationConstants.TARGET_BINARY_FILE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectSingleAt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.openjson.JSONObject;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import de.tudarmstadt.ukp.dkpro.core.io.bincas.BinaryCasReader;
import de.tudarmstadt.ukp.dkpro.core.io.bincas.BinaryCasWriter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import de.tudarmstadt.ukp.dkpro.templates.synchronization.Version;

/**
 * 
 * @author Asha Joshi
 *
 */
@Component
public class ConstituentEditorActionHandler
{
    private DocumentService documentService;
    
    @Autowired
    public ConstituentEditorActionHandler(DocumentService aDocumentService)
    {
        documentService = aDocumentService;
    }

    /**
     * Link the child and parent node.
     * 
     * @param selectedFile
     *            file to modify annotation data
     * @param renderingmode
     *            rendering mode, 0 - baseline, 1 - iterative
     * @param parentNode
     *            the address of parent node
     * @param childNode
     *            the address of child node
     * @return the updated annotation data in JSON format for client
     */
    public String addEdge(String selectedFile, int renderingmode, String parentNode,
            String childNode, int clientversion)
        throws IOException, UIMAException
    {
        String selectedFileName;
        int sentenceAddress;
        // get the file name and sentence ID
        String[] splitFileDetails = selectedFile.split("#");

        if (splitFileDetails.length == 2) {
            selectedFileName = splitFileDetails[0];
            sentenceAddress = Integer.parseInt(splitFileDetails[1]);

            // Create a JCas for modifying annotation data
            JCas document = CasCreationUtils.createCas((TypeSystemDescription) null, null, null)
                    .getJCas();

            // Reading annotation and load to JCas
            loadFileToCas(selectedFileName, document);

            // Get the parent node using its address
            Annotation parent = getNodeByAddress(document, Integer.parseInt(parentNode));

            // Get the child node using its address
            Annotation child = getNodeByAddress(document, Integer.parseInt(childNode));

            // For storing list of new children nodes
            List<Annotation> new_child_node = new ArrayList<>();

            if (parent instanceof Constituent) {
                // get current children of parent node
                Collection<Annotation> children = select(((Constituent) parent).getChildren(),
                        Annotation.class);

                // add all children including the new child to list
                new_child_node.addAll(children);
                new_child_node.add(child);

                parent.setBegin(parent.getBegin() > child.getBegin() ? child.getBegin()
                        : parent.getBegin());
                parent.setEnd(parent.getEnd() < child.getEnd() ? child.getEnd() : parent.getEnd());
                // set the new children list to parent node
                ((Constituent) parent)
                        .setChildren(FSCollectionFactory.createFSArray(document, new_child_node));

                // set the parent of child to selected parent node
                if (child instanceof Token) {
                    ((Token) child).setParent(parent);
                }
                else if (child instanceof Constituent) {
                    ((Constituent) child).setParent(parent);
                }

                new_child_node.clear();

                Sentence selectedSentence = getNodeByAddress(document, sentenceAddress);

                // Only one root element exist in current syntactic analysis
                ROOT root = selectSingleAt(document, ROOT.class, selectedSentence.getBegin(),
                        selectedSentence.getEnd());

                if (root != null) {
                    // Get the children of root
                    Collection<Annotation> root_children = select(root.getChildren(),
                            Annotation.class);
                    // remove the selected child node from root children
                    root_children.forEach((root_child) -> {
                        if (root_child.getAddress() != Integer.parseInt(childNode)) {
                            new_child_node.add(root_child);
                        }
                    });
                    // update root children
                    root.setChildren(FSCollectionFactory.createFSArray(document, new_child_node));
                }
            }

            // update the version
            int version = updateVersion(document, sentenceAddress);

            // Save the updated annotation data
            saveAnnotationdata(document, selectedFileName);

            // return the analysis data in the JSON form

            // For incremental protocol, track updates
            if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
                String syntacticFun = "none";
                if (child instanceof Constituent) {
                    syntacticFun = isBlank(((Constituent) child).getSyntacticFunction())
                            ? "none"
                            : ((Constituent) child).getSyntacticFunction();
                }
                else if (child instanceof Token) {
                    syntacticFun = isBlank(((Token) child).getSyntacticFunction())
                            ? "none"
                            : ((Token) child).getSyntacticFunction();
                }
                
                VEdge tempedge = new VEdge();
                tempedge.setTo(parent.getAddress());
                tempedge.setFrom(child.getAddress());
                tempedge.setId("e_" + tempedge.getTo() + "_" + tempedge.getFrom());
                tempedge.setLabel(syntacticFun);
                
                ObjectMapper mapper = new ObjectMapper();
                
                // List of actions performed for incremental mode
                JSONObject response = new JSONObject();
                JSONObject actions = new JSONObject();
                actions.put(ACTION_ADD_EDGE,
                        mapper.writeValueAsString(tempedge));
                response.put(RENDERING_MODE, renderingmode);
                response.put(ACTIONS_LIST, actions);
                response.put(LAST_UPDATE,
                        "e_" + tempedge.getTo() + "_" + tempedge.getFrom());
                response.put("selectedfile", selectedFile);
                response.put("version", version);
                
                // return set of actions
                return response.toString();
            }
            // Baseline mode return complete annotation data
            else {
                // return set complete annotation data
                return render(selectedFile,
                        "e_" + parent.getAddress() + "_" + child.getAddress());
            }
        }
        else {
            return "";
        }

    }

    /**
     * editEdge function Change the syntactic function of a node
     * 
     * @param selectedFile
     *            the file selected
     * @param renderingmode
     *            the rendering mode
     * @param edge_id
     *            the edge ID selected
     * @param edgelabel
     *            the new syntactic function
     * @return the updated annotation data in JSON format for client
     */
    public String editEdge(String selectedFile, int renderingmode, String edge_id, String edgelabel,
            int clientversion)
        throws Exception
    {
        // List of actions in incremental mode
        JSONObject response = new JSONObject();
        JSONObject actions = new JSONObject();

        String selectedFileName;
        int sentenceAddress;
        // get the file name and sentence ID
        String[] splitFileDetails = selectedFile.split("#");

        if (splitFileDetails.length == 2) {
            selectedFileName = splitFileDetails[0];
            sentenceAddress = Integer.parseInt(splitFileDetails[1]);

            // JCas document = JCasFactory.createJCas();
            JCas document = CasCreationUtils.createCas((TypeSystemDescription) null, null, null)
                    .getJCas();

            // Reading annotation
            loadFileToCas(selectedFileName, document);

            String[] splitedgeID = edge_id.split("_");

            // get the Parent node ID from Edge ID
            Annotation child_node = getNodeByAddress(document, Integer.parseInt(splitedgeID[2]));

            // set the new syntactic function or edge label
            if (child_node instanceof Token) {
                ((Token) child_node).setSyntacticFunction(edgelabel);
            }
            else if (child_node instanceof Constituent) {
                ((Constituent) child_node).setSyntacticFunction(edgelabel);
            }
            // update the version
            int version = updateVersion(document, sentenceAddress);

            // update annotation data
            saveAnnotationdata(document, selectedFileName);

            // return results in JSON format

            // For incremental protocol, track updates
            if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
                JSONObject item = new JSONObject();
                item.put("id", edge_id);
                item.put("text", isBlank(edgelabel) ? "none" : edgelabel);
                actions.put(ACTION_UPDATE_EDGE, item.toString());
                response.put(RENDERING_MODE, renderingmode);
                response.put(ACTIONS_LIST, actions);
                response.put(LAST_UPDATE, edge_id);
                response.put("selectedfile", selectedFile);
                response.put("version", version);
                // return set of actions
                return response.toString();
            }
            // Rendering mode Baseline
            else {
                // return set complete annotation data
                return render(selectedFile, edge_id);
            }
        }
        else {
            return "";
        }
    }

    /**
     * editNode function Change the type of a node
     * 
     * @param selectedFile
     *            the annotation file selected
     * @param renderingmode
     *            the rendering mode selected
     * @param node_id
     *            the ID of node to be updated
     * @param newtype
     *            the new type
     * @return the updated annotated data
     */
    public String editNode(String selectedFile, int renderingmode, String node_id, String newtype,
            int clientversion)
        throws Exception
    {

        // List of actions in incremental mode
        JSONObject response = new JSONObject();
        JSONObject actions = new JSONObject();

        String selectedFileName;
        int sentenceAddress;
        // get the file name and sentence ID
        String[] splitFileDetails = selectedFile.split("#");

        if (splitFileDetails.length == 2) {
            selectedFileName = splitFileDetails[0];
            sentenceAddress = Integer.parseInt(splitFileDetails[1]);
            JCas document = CasCreationUtils.createCas((TypeSystemDescription) null, null, null)
                    .getJCas();

            // Reading annotation
            loadFileToCas(selectedFileName, document);

            // get the node using address
            Annotation selectednode = getNodeByAddress(document, Integer.parseInt(node_id));

            // set the new type to the node
            if (selectednode instanceof Token) {
                // If node is a Token set POS value
                ((Token) selectednode).getPos().setPosValue(newtype);
            }
            else if (selectednode instanceof Constituent) {
                // If node is a Constituent set Type
                ((Constituent) selectednode).setConstituentType(newtype);
            }

            // update annotation data
            int version = updateVersion(document, sentenceAddress);

            // Save the annotation data
            saveAnnotationdata(document, selectedFileName);

            // return the updated analysis data in JSON form

            // For incremental protocol, track updates
            if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
                JSONObject item = new JSONObject();
                item.put("id", Integer.parseInt(node_id));
                item.put("text", newtype);
                actions.put(ACTION_UPDATE_NODE, item.toString());
                response.put(RENDERING_MODE, renderingmode);
                response.put(ACTIONS_LIST, actions);
                response.put(LAST_UPDATE, node_id);
                response.put("selectedfile", selectedFile);
                response.put("version", version);
                // return set of actions
                return response.toString();
            }
            // Rendering mode Baseline
            else {
                // return set complete annotation data
                return render(selectedFile, node_id);
            }

        }
        else {
            return "";
        }
    }

    /**
     * deleteEdge function Removes the link between parent and child node
     * 
     * @param selectedFile
     *            the selected file name
     * @param renderingmode
     *            the selected rendering mode
     * @param edge_id
     *            the edge to be removed
     * @return the updated annotated data
     */
    public String deleteEdge(String selectedFile, int renderingmode, String edge_id,
            int clientversion)
        throws Exception
    {
        // List of actions in incremental mode
        JSONObject response = new JSONObject();
        JSONObject actions = new JSONObject();

        String selectedFileName;
        int sentenceAddress;
        // get the file name and sentence ID
        String[] splitFileDetails = selectedFile.split("#");

        if (splitFileDetails.length == 2) {
            selectedFileName = splitFileDetails[0];
            sentenceAddress = Integer.parseInt(splitFileDetails[1]);

            // Create Cas
            JCas document = CasCreationUtils.createCas((TypeSystemDescription) null, null, null)
                    .getJCas();

            // Reading annotation data from selected file
            loadFileToCas(selectedFileName, document);

            // get parent child node IDs from Edge ID
            String[] splitedgeID = edge_id.split("_");

            // get parent node from address
            Annotation parent = getNodeByAddress(document, Integer.parseInt(splitedgeID[1]));
            // get child node from address
            Annotation child = getNodeByAddress(document, Integer.parseInt(splitedgeID[2]));

            // get the Sentence annotations
            Sentence selectedSentence = getNodeByAddress(document, sentenceAddress);

            // Only one root element exist in current syntactic analysis
            ROOT root = selectSingleAt(document, ROOT.class, selectedSentence.getBegin(),
                    selectedSentence.getEnd());

            // Remove previous parent of child node and set root as new parent
            if (root != null) {
                if (child instanceof Token) {
                    ((Token) child).setParent(root);
                }
                else {
                    ((Constituent) child).setParent(root);
                }
            }

            // get the children of parent node
            if (parent != null) {
                Collection<Annotation> parent_children;
                if (parent instanceof ROOT) {
                    parent_children = select(((ROOT) parent).getChildren(), Annotation.class);
                }
                else {
                    parent_children = select(((Constituent) parent).getChildren(),
                            Annotation.class);
                }

                // remove the child node from children of parent node
                List<Annotation> newchildren = new ArrayList<>();

                // update the array of chilren and begin, end pos
                int beginpos = 0, endpos = 0, first = 0;
                Iterator<Annotation> parent_children_iterator = parent_children.iterator();
                while (parent_children_iterator.hasNext()) {
                    Annotation p_child = (Annotation) parent_children_iterator.next();
                    if (p_child.getAddress() != child.getAddress()) {
                        if (first == 0) {
                            beginpos = p_child.getBegin();
                            endpos = p_child.getEnd();
                            first++;
                        }
                        else {
                            beginpos = p_child.getBegin() < beginpos ? p_child.getBegin()
                                    : beginpos;
                            endpos = p_child.getEnd() > endpos ? p_child.getEnd() : endpos;
                        }
                        newchildren.add(p_child);
                    }
                }

                // update the children of parent node
                if (parent instanceof ROOT) {
                    ((ROOT) parent)
                            .setChildren(FSCollectionFactory.createFSArray(document, newchildren));
                }
                else {
                    // update begin and end position values
                    parent.setBegin(beginpos);
                    parent.setEnd(endpos);
                    ((Constituent) parent)
                            .setChildren(FSCollectionFactory.createFSArray(document, newchildren));
                }

                newchildren.clear();

                // update the children of root node
                // add the child node to children of root node
                if (root != null) {
                    Collection<Annotation> root_children = select(root.getChildren(),
                            Annotation.class);
                    newchildren.addAll(root_children);
                    newchildren.add(child);
                    root.setChildren(FSCollectionFactory.createFSArray(document, newchildren));
                }
            }

            // update annotation data
            int version = updateVersion(document, sentenceAddress);

            // write the cache to the file
            saveAnnotationdata(document, selectedFileName);

            // return the updated analysis data in JSON form

            // For incremental protocol, track updates
            if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
                actions.put(ACTION_DELETE_EDGE,
                        "e_" + parent.getAddress() + "_" + child.getAddress());
                response.put(RENDERING_MODE, renderingmode);
                response.put(ACTIONS_LIST, actions);
                response.put(LAST_UPDATE, parent.getAddress());
                response.put("selectedfile", selectedFile);
                response.put("version", version);
                // return set of actions
                return response.toString();
            }
            // Rendering mode Baseline
            else {
                // return set complete annotation data
                return render(selectedFile, Integer.toString(parent.getAddress()));
            }
        }
        else {
            return "";
        }
    }

    /**
     * 
     * deleteNode function Removes the selected node from annotation data
     * 
     * @param selectedFileName
     *            the selected annotation file
     * @param renderingmode
     *            the selected rendering mode
     * @param node_id
     *            the ID of node to be deleted
     * @return the updated annotation data in JSON form
     * @throws Exception
     */
    public String deleteNode(String selectedFile, int renderingmode, String node_id,
            int clientversion)
        throws Exception
    {

        // List of actions in incremental mode
        JSONObject response = new JSONObject();
        JSONObject actions = new JSONObject();

        String selectedFileName;
        int sentenceAddress;
        // get the file name and sentence ID
        String[] splitFileDetails = selectedFile.split("#");

        if (splitFileDetails.length == 2) {
            selectedFileName = splitFileDetails[0];
            sentenceAddress = Integer.parseInt(splitFileDetails[1]);

            // Create Cas
            JCas document = CasCreationUtils.createCas((TypeSystemDescription) null, null, null)
                    .getJCas();

            // Reading annotation data from selected file
            loadFileToCas(selectedFileName, document);

            // get sentence
            Sentence selectedSentence = getNodeByAddress(document, sentenceAddress);

            // get root
            ROOT root = selectSingleAt(document, ROOT.class, selectedSentence.getBegin(),
                    selectedSentence.getEnd());

            // get the node from its address
            Annotation node = getNodeByAddress(document, Integer.parseInt(node_id));

            // delete node
            // node is a Token
            if (node instanceof Token) {
                deletePOSNode(node, document, Integer.parseInt(node_id), actions, renderingmode,
                        root);
            }
            // node is a Constituent
            else if (node instanceof Constituent) {
                deleteContituentNode(node, document, Integer.parseInt(node_id), actions,
                        renderingmode);
            }
            // update annotation version
            int version = updateVersion(document, sentenceAddress);

            // write the cache to the file
            saveAnnotationdata(document, selectedFileName);

            // For incremental protocol, track updates
            if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
                response.put(RENDERING_MODE, renderingmode);
                response.put(ACTIONS_LIST, actions);
                response.put("selectedfile", selectedFile);
                response.put("version", version);
                // return set of actions
                return response.toString();
            }
            else {
                // return set complete annotation data
                return render(selectedFile, "");
            }
        }
        else {
            return "";
        }
    }

    /**
     * deleteContituentNode Removes the selected node from annotation data
     * 
     * @param node
     *            the node to be removed
     * @param document
     *            the cas with annotation data
     * @param node_id
     *            the ID of node to be removed
     * @param actions
     *            the set of actions performed
     * @param renderingmode
     *            the selected rendering mode
     */
    private void deleteContituentNode(Annotation node, JCas document, int node_id,
            JSONObject actions, int renderingmode)
    {
        // get the parent of node
        Annotation parent = ((Constituent) node).getParent();

        // List of edges added and deleted
        List<String> deletedEdges = new ArrayList<String>();
        List<VEdge> addedEdges = new ArrayList<VEdge>();

        // get the children of node
        List<Annotation> newchildren = new ArrayList<>();
        Collection<Annotation> node_children = select(((Constituent) node).getChildren(),
                Annotation.class);

        // get the children of node's parent node
        Collection<Annotation> parent_children;
        if (parent instanceof ROOT) {
            parent_children = select(((ROOT) parent).getChildren(), Annotation.class);
        }
        else {
            parent_children = select(((Constituent) parent).getChildren(), Annotation.class);
        }

        // set the parent of node children to node parent
        node_children.forEach((child) -> {
            String syntacticfun = "none";
            if (child instanceof Token) {
                ((Token) child).setParent(parent);
                syntacticfun = isBlank(((Token) child).getSyntacticFunction()) ? "none"
                        : ((Token) child).getSyntacticFunction();
            }
            else {
                ((Constituent) child).setParent(parent);
                syntacticfun = isBlank(((Constituent) child).getSyntacticFunction())
                        ? "none"
                        : ((Constituent) child).getSyntacticFunction();
            }
            // do not show links to the ROOT node
            if (parent instanceof ROOT == false) {
                // add the new edges to list
                VEdge tempedge = new VEdge();
                tempedge.setTo(parent.getAddress());
                tempedge.setFrom(child.getAddress());
                tempedge.setId("e_" + parent.getAddress() + "_" + child.getAddress());
                tempedge.setLabel(syntacticfun);
                addedEdges.add(tempedge);
            }
            // add the edges deleted to list
            deletedEdges.add("e_" + node.getAddress() + "_" + child.getAddress());
        });

        // add all the node's children to Node's parent children
        newchildren.addAll(node_children);
        parent_children.forEach((child) -> {
            // remove the node itself from node's parent children list
            if (child.getAddress() != node_id) {
                newchildren.add(child);
            }
        });

        // set new list of children to parent node
        if (parent instanceof ROOT) {
            ((ROOT) parent).setChildren(FSCollectionFactory.createFSArray(document, newchildren));
        }
        else {
            ((Constituent) parent)
                    .setChildren(FSCollectionFactory.createFSArray(document, newchildren));
            deletedEdges.add("e_" + parent.getAddress() + "_" + node_id);
        }

        // For incremental protocol, track updates
        if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
            actions.put(ACTION_DELETE_EDGE, deletedEdges);
            actions.put(ACTION_DELETE_NODE, node_id);
            actions.put(ACTION_ADD_EDGE, addedEdges);
        }
        node.removeFromIndexes();
    }

    /**
     * deletePOSNode delete the Token and corresponding POS node
     * 
     * @param node
     *            the node to be deleted
     * @param document
     *            the cas with annotation data
     * @param node_id
     *            the ID of node to be deleted
     * @param actions
     *            the set of actions performed - applicable for Incremental rendering
     * @param renderingmode
     *            the selected rendering mode
     * @throws Exception
     */
    private void deletePOSNode(Annotation node, JCas document, int node_id, JSONObject actions,
            int renderingmode, ROOT root)
        throws Exception
    {

        // get the parent of selected Token
        Annotation parent = ((Token) node).getParent();

        // maintain list of nodes and edges deleted
        List<Integer> deletedNodes = new ArrayList<Integer>();
        List<String> deletedEdges = new ArrayList<String>();

        // remove the selected Token and POS selected
        int posaddress = ((Token) node).getPos().getAddress();
        Annotation posnode = getNodeByAddress(document, posaddress);
        posnode.removeFromIndexes();
        ((Token) node).setPos(null);
        // If Token previously had a constituent parent node, set parent to root
        if (root != null) {
            if ((((Token) node).getParent() == null)
                    || (((Token) node).getParent().getAddress() != root.getAddress())) {
                ((Token) node).setParent(root);
            }
        }
        // Remove parent nodes recursively with only one child node
        if (parent != null) {
            removenodeRecursively(document, parent, node_id, deletedNodes, deletedEdges,
                    renderingmode, root);
        }

        // Rendering mode Iterative - track actions performed
        if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
            deletedNodes.add(node.getAddress());
            actions.put(ACTION_DELETE_EDGE, deletedEdges);
            actions.put(ACTION_DELETE_NODE, deletedNodes);
        }

        if (root != null) {
            List<Annotation> child_node = new ArrayList<>();
            child_node.addAll(select(((ROOT) root).getChildren(), Annotation.class));
            child_node.add(node);
            ((ROOT) root).setChildren(FSCollectionFactory.createFSArray(document, child_node));
        }
    }

    /**
     * removenodeRecursively function removes recursively parent nodes with single child node which
     * is deleted
     * 
     * @param document
     *            the cas with annotation data
     * @param parent
     *            the parent node
     * @param node_id
     *            the id of child node
     * @param deletedNodes
     *            list of deleted nodes
     * @param deletedEdges
     *            list of deleted edges
     * @param renderingmode
     *            the delected rendering mode
     * @throws Exception
     */
    private void removenodeRecursively(JCas document, Annotation parent, int node_id,
            List<Integer> deletedNodes, List<String> deletedEdges, int renderingmode, ROOT root)
        throws Exception
    {
        // get the children of parent node
        Collection<Annotation> children = select(((Constituent) parent).getChildren(),
                Annotation.class);

        // If only one child exist and parent is not ROOT node
        if (children.size() == 1 && parent instanceof ROOT == false) {
            // remove the parent node and call recursively removenodeRecursively
            // on parent's parent node
            removenodeRecursively(document, ((Constituent) parent).getParent(), parent.getAddress(),
                    deletedNodes, deletedEdges, renderingmode, root);

            // For incremental protocol, track updates
            if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
                deletedNodes.add(parent.getAddress());
                deletedEdges.add("e_" + parent.getAddress() + "_" + node_id);
            }

            // remove parent node
            parent.removeFromIndexes();
        }
        // Case when number of children are more than one or parent node is ROOT
        else {
            // remove the child node from list of children and update parent
            // new child list
            int beginpos = 0, endpos = 0, first = 0;
            List<Annotation> child_node = new ArrayList<>();
            Iterator<Annotation> children_iterator = children.iterator();
            while (children_iterator.hasNext()) {
                Annotation child = (Annotation) children_iterator.next();
                if (child.getAddress() != node_id) {
                    if (first == 0) {
                        beginpos = child.getBegin();
                        endpos = child.getEnd();
                        first++;
                    }
                    else {
                        beginpos = child.getBegin() < beginpos ? child.getBegin() : beginpos;
                        endpos = child.getEnd() > endpos ? child.getEnd() : endpos;
                    }
                    child_node.add(child);
                }
            }

            if (getNodeByAddress(document, node_id) instanceof Token == false) {
                if (parent instanceof ROOT) {
                    ((ROOT) parent)
                            .setChildren(FSCollectionFactory.createFSArray(document, child_node));
                }
                else {
                    parent.setBegin(beginpos);
                    parent.setEnd(endpos);
                    ((Constituent) parent)
                            .setChildren(FSCollectionFactory.createFSArray(document, child_node));

                    // For incremental protocol, track updates
                    if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
                        deletedEdges.add("e_" + parent.getAddress() + "_" + node_id);
                    }
                }
            }
            else {
                // Only remove links of Token with Constituent parent and set
                // Root as parent
                if (parent instanceof ROOT == false) {
                    parent.setBegin(beginpos);
                    parent.setEnd(endpos);
                    ((Constituent) parent)
                            .setChildren(FSCollectionFactory.createFSArray(document, child_node));
                    // For incremental protocol, track updates
                    if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
                        deletedEdges.add("e_" + parent.getAddress() + "_" + node_id);
                    }

                    child_node.clear();
                    // get the root node
                    if (root != null && root instanceof ROOT) {
                        Collection<Annotation> root_children = select(
                                ((Constituent) root).getChildren(), Annotation.class);
                        child_node.addAll(root_children);
                        child_node.add(getNodeByAddress(document, node_id));
                        root.setChildren(FSCollectionFactory.createFSArray(document, child_node));
                    }

                }
            }
        }
    }

    /**
     * addNodeInBetween function adds the new node in between the edge child and parent node to
     * annotation data
     * 
     * @param selectedFileName
     *            the selected file name
     * @param renderingmode
     *            the selected rendering mode
     * @param edge_id
     *            the ID of edge
     * @param newlabel
     *            the type of new node
     * @return the updated annotation data in JSON form
     * @throws Exception
     */
    public String addNodeInBetween(String selectedFile, int renderingmode, String edge_id,
            String newlabel, int clientversion)
        throws Exception
    {

        // List of actions in incremental mode
        JSONObject response = new JSONObject();
        JSONObject actions = new JSONObject();

        // get the file name and sentence ID
        String selectedFileName;
        int sentenceAddress;
        String[] splitFileDetails = selectedFile.split("#");

        if (splitFileDetails.length == 2) {
            selectedFileName = splitFileDetails[0];
            sentenceAddress = Integer.parseInt(splitFileDetails[1]);

            // Create Cas
            JCas document = CasCreationUtils.createCas((TypeSystemDescription) null, null, null)
                    .getJCas();

            // Reading annotation data from selected file
            loadFileToCas(selectedFileName, document);

            // Check node type with id
            String[] splitnodeID = edge_id.split("_");

            int newnodeaddress = 0;
            // text has start and end offset in ID separated with underscore
            if (splitnodeID.length > 1) {
                // Case of POS node
                newnodeaddress = createNodeInBW(document, edge_id, newlabel, actions,
                        renderingmode);
            }

            // update annotation version
            int version = updateVersion(document, sentenceAddress);

            // write the cache to the file
            saveAnnotationdata(document, selectedFileName);

            // For incremental protocol, track updates
            if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
                response.put(RENDERING_MODE, renderingmode);
                response.put(ACTIONS_LIST, actions);
                response.put(LAST_UPDATE, newnodeaddress);
                response.put("selectedfile", selectedFile);
                response.put("version", version);
                // return set of actions
                return response.toString();
            }
            else {
                // return set complete annotation data
                return render(selectedFile, Integer.toString(newnodeaddress));
            }
        }
        else {
            return "";
        }
    }

    /**
     * createNodeInBW function Add a new Constituent node in between the given child and parent node
     * to the annotation data
     * 
     * @param root
     *            the root node
     * @param document
     *            the cas with annotation data
     * @param edge_id
     *            the ID of edge
     * @param nodetype
     *            the type of new node
     * @param actions
     *            the set of actions performed - applicable for Iterative rendering
     * @param renderingmode
     *            the selected rendering mode
     * @throws Exception
     */
    private int createNodeInBW(JCas document, String edge_id, String nodetype, JSONObject actions,
            int renderingmode)
        throws Exception
    {
        String[] node_ids = edge_id.split("_");
        Annotation selectedchildnode = getNodeByAddress(document, Integer.parseInt(node_ids[2]));
        Annotation selectedparentnode = getNodeByAddress(document, Integer.parseInt(node_ids[1]));

        // create a parent node
        Constituent newNode = new Constituent(document);
        newNode.setConstituentType(nodetype); // label of parent
        newNode.setParent(selectedparentnode);
        newNode.setBegin(selectedchildnode.getBegin());
        newNode.setEnd(selectedchildnode.getEnd());
        newNode.addToIndexes();

        // set the new type
        if (selectedchildnode instanceof Token) {
            ((Token) selectedchildnode).setParent(newNode);
        }
        else if (selectedchildnode instanceof Constituent) {
            ((Constituent) selectedchildnode).setParent(newNode);
        }

        // link the new constituent node with child node
        List<Annotation> child_node = new ArrayList<>();
        child_node.add(selectedchildnode);
        newNode.setChildren(FSCollectionFactory.createFSArray(document, child_node));
        newNode.setSyntacticFunction("none"); // edge default syntactic
                                              // function

        child_node.clear();

        // add the new constituent node to children of root
        Collection<Annotation> parent_children = null;
        parent_children = select(((Constituent) selectedparentnode).getChildren(),
                Annotation.class);

        int beginpos = 0, endpos = 0, first = 0;
        Iterator<Annotation> parent_children_iterator = parent_children.iterator();
        while (parent_children_iterator.hasNext()) {
            Annotation child = (Annotation) parent_children_iterator.next();
            if (child.getAddress() != selectedchildnode.getAddress()) {
                if (first == 0) {
                    beginpos = child.getBegin();
                    endpos = child.getEnd();
                    first++;
                }
                else {
                    beginpos = child.getBegin() < beginpos ? child.getBegin() : beginpos;
                    endpos = child.getEnd() > endpos ? child.getEnd() : endpos;
                }
                child_node.add(child);
            }
        }

        child_node.add(newNode);

        // update the begin and end pos of parent annotation
        beginpos = newNode.getBegin() < beginpos ? newNode.getBegin() : beginpos;
        endpos = newNode.getEnd() > endpos ? newNode.getEnd() : endpos;
        selectedparentnode.setBegin(beginpos);
        selectedparentnode.setBegin(endpos);

        ((Constituent) selectedparentnode)
                .setChildren(FSCollectionFactory.createFSArray(document, child_node));

        // For incremental protocol, track updates
        if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
            List<VEdge> newedges = new ArrayList<VEdge>();

            VEdge tempedge = new VEdge();
            tempedge.setTo(newNode.getAddress());
            tempedge.setFrom(selectedchildnode.getAddress());
            tempedge.setId("e_" + tempedge.getTo() + "_" + tempedge.getFrom());
            String syntacticfun = "none";
            if (selectedchildnode instanceof Constituent) {
                syntacticfun = isBlank(((Constituent) selectedchildnode).getSyntacticFunction()) ? "none"
                                : ((Constituent) selectedchildnode).getSyntacticFunction();
            }
            else if (selectedchildnode instanceof Token) {
                syntacticfun = isBlank(((Token) selectedchildnode).getSyntacticFunction()) ? "none"
                                : ((Token) selectedchildnode).getSyntacticFunction();
            }
            tempedge.setLabel(syntacticfun);
            newedges.add(tempedge);

            tempedge = new VEdge();
            tempedge.setTo(selectedparentnode.getAddress());
            tempedge.setFrom(newNode.getAddress());
            tempedge.setId("e_" + tempedge.getTo() + "_" + tempedge.getFrom());
            tempedge.setLabel(newNode.getSyntacticFunction());
            newedges.add(tempedge);

            VNode newnode = new VNode();
            newnode.setId(newNode.getAddress());
            newnode.setText(newNode.getConstituentType());
            newnode.setOffset(new int[2]);

            ObjectMapper mapper = new ObjectMapper();
            actions.put(ACTION_DELETE_EDGE, edge_id);
            actions.put(ACTION_ADD_NODE, mapper.writeValueAsString(newnode));
            actions.put(ACTION_ADD_EDGE, newedges);
        }
        return newNode.getAddress();
    }

    /**
     * addNode function adds the new node to annotation data
     * 
     * @param selectedFile
     *            the selected file name
     * @param renderingmode
     *            the selected rendering mode
     * @param child_id
     *            the ID of node child node
     * @param nodetype
     *            the type of new node
     * @return the updated annotation data in JSON form
     */
    public String addNode(String selectedFile, int renderingmode, String child_id, String nodetype,
            int clientversion)
        throws Exception
    {

        // List of actions in incremental mode
        JSONObject response = new JSONObject();
        JSONObject actions = new JSONObject();

        String selectedFileName;
        int sentenceAddress;
        // get the file name and sentence ID
        String[] splitFileDetails = selectedFile.split("#");

        if (splitFileDetails.length == 2) {
            selectedFileName = splitFileDetails[0];
            sentenceAddress = Integer.parseInt(splitFileDetails[1]);

            // Create Cas
            JCas document = CasCreationUtils.createCas((TypeSystemDescription) null, null, null)
                    .getJCas();

            // Reading annotation data from selected file
            loadFileToCas(selectedFileName, document);

            Sentence selectedSentence = getNodeByAddress(document, sentenceAddress);

            // Only one root element exist in current syntactic analysis
            ROOT root = selectSingleAt(document, ROOT.class, selectedSentence.getBegin(),
                    selectedSentence.getEnd());

            // Check node type with id
            String[] splitnodeID = child_id.split("_");
            int newnodeaddress = 0;
            // text has start and end offset in ID seperated with underscore
            if (splitnodeID.length > 1) {
                // Case of POS node
                newnodeaddress = createPOSNode(document, splitnodeID, nodetype, actions,
                        renderingmode);
            }
            // nodes has only there address as ID
            else {
                // Case of Constituent node
                newnodeaddress = createConstituentNode(root, document, Integer.parseInt(child_id),
                        nodetype, actions, renderingmode);
            }

            // update the version
            int version = updateVersion(document, sentenceAddress);

            // write the cache to the file
            saveAnnotationdata(document, selectedFileName);

            // For incremental protocol, track updates
            if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
                response.put(RENDERING_MODE, renderingmode);
                response.put(ACTIONS_LIST, actions);
                response.put(LAST_UPDATE, newnodeaddress);
                response.put("selectedfile", selectedFile);
                response.put("version", version);
                // return set of actions
                return response.toString();
            }
            else {
                // return set complete annotation data
                return render(selectedFile, Integer.toString(newnodeaddress));
            }
        }
        else {
            return "";
        }

    }

    /**
     * createConstituentNode function Add a new Constituent node to the annotation data
     * 
     * @param root
     *            the root node
     * @param document
     *            the cas with annotation data
     * @param child_id
     *            the ID of child node
     * @param nodetype
     *            the type of new node
     * @param actions
     *            the set of actions performed - applicable for Iterative rendering
     * @param renderingmode
     *            the selected rendering mode
     * @throws Exception
     */
    private int createConstituentNode(ROOT root, JCas document, int child_id, String nodetype,
            JSONObject actions, int renderingmode)
        throws Exception
    {

        // get the child node from address
        Annotation selectedchildnode = getNodeByAddress(document, child_id);
        String syntacticFun = "none";
        // create a parent node
        Constituent newParent = new Constituent(document);
        newParent.setConstituentType(nodetype); // label of parent
        newParent.setParent(root);
        newParent.setBegin(selectedchildnode.getBegin());
        newParent.setEnd(selectedchildnode.getEnd());
        newParent.addToIndexes();

        // set the new type
        if (selectedchildnode instanceof Token) {
            ((Token) selectedchildnode).setParent(newParent);
            syntacticFun = isBlank(((Token) selectedchildnode).getSyntacticFunction())
                    ? "none"
                    : ((Token) selectedchildnode).getSyntacticFunction();
        }
        else if (selectedchildnode instanceof Constituent) {
            ((Constituent) selectedchildnode).setParent(newParent);
            syntacticFun = isBlank(((Constituent) selectedchildnode).getSyntacticFunction())
                    ? "none"
                    : ((Constituent) selectedchildnode).getSyntacticFunction();
        }

        // link the new constituent node with child node
        List<Annotation> child_node = new ArrayList<>();
        child_node.add(selectedchildnode);
        newParent.setChildren(FSCollectionFactory.createFSArray(document, child_node));
        newParent.setSyntacticFunction("none"); // edge default syntactic
                                                // function

        child_node.clear();

        // add the new constituent node to children of root
        if (root != null) {
            Collection<Annotation> root_children = null;
            if (root.getChildren() != null) {
                root_children = select(root.getChildren(), Annotation.class);
                root_children.forEach((child) -> {
                    if (child.getAddress() != child_id) {
                        child_node.add(child);
                    }
                });
            }
            child_node.add(newParent);
            root.setChildren(FSCollectionFactory.createFSArray(document, child_node));
        }

        // For incremental protocol, track updates
        if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
            VEdge tempedge = new VEdge();
            tempedge.setTo(newParent.getAddress());
            tempedge.setFrom(child_id);
            tempedge.setId("e_" + tempedge.getTo() + "_" + tempedge.getFrom());
            tempedge.setLabel(syntacticFun);

            VNode newnode = new VNode();
            newnode.setId(newParent.getAddress());
            newnode.setText(newParent.getConstituentType());
            newnode.setOffset(new int[2]);

            ObjectMapper mapper = new ObjectMapper();
            actions.put(ACTION_ADD_NODE, mapper.writeValueAsString(newnode));
            actions.put(ACTION_ADD_EDGE, mapper.writeValueAsString(tempedge));
        }

        // return the address of newly created Constituent annotation
        return newParent.getAddress();
    }

    /**
     * createPOSNode function add a new Token and POS to annotation data
     * 
     * @param root
     *            the root node
     * @param document
     *            the cas with annotation data
     * @param splitnodeID
     *            the ID of child node
     * @param nodetype
     *            the type of new node
     * @param actions
     *            the set of actions performed - applicable for Iterative rendering
     * @param renderingmode
     *            the selected rendering mode
     * @throws Exception
     */
    private int createPOSNode(JCas document, String[] splitnodeID, String nodetype,
            JSONObject actions, int renderingmode)
        throws Exception
    {

        // Create a new node
        Token newtoken = getTokenwithOffset(document, Integer.parseInt(splitnodeID[1]),
                Integer.parseInt(splitnodeID[2]));

        if (newtoken != null) {
            POS newpos = new POS(document);
            newpos.setPosValue(nodetype);
            newpos.setBegin(newtoken.getBegin());
            newpos.setEnd(newtoken.getEnd());
            newpos.addToIndexes();
            newtoken.setPos(newpos);

            // For incremental protocol, track updates
            if (renderingmode == Mode.MODE_INCREMENTAL.ordinal()) {
                VNode newnode = new VNode();
                newnode.setId(newtoken.getAddress());
                newnode.setText(newpos.getPosValue());
                int[] tmpoffset = new int[] { newtoken.getBegin(), newtoken.getEnd() };
                newnode.setOffset(tmpoffset);
                ObjectMapper mapper = new ObjectMapper();
                actions.put(ACTION_ADD_NODE,
                        mapper.writeValueAsString(newnode));
            }
        }

        // return newly created POS's Token address
        return newtoken.getAddress();
    }

    /**
     * Automatic testing setup Get the artifact ID for test documents
     * 
     * @param filename
     * @return
     * @throws Exception
     */
    public String getAnnotationFileTesting(String filename) throws Exception
    {
        TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        JCas document = CasCreationUtils.createCas(tsd, null, null).getJCas();
        loadFileToCas(filename + BINARY_FILE_EXT, document);
        Collection<Sentence> sentences = select(document, Sentence.class);
        Sentence selectedSentence = sentences.iterator().next();
        String annotationfile = filename + BINARY_FILE_EXT + "#"
                + selectedSentence.getAddress();
        return annotationfile;
    }

    /**
     * In the method the version number is verified against version number received from client to
     * identify unsynchronized clients
     * 
     * @param selectedFileName
     *            artifact ID
     * @param version
     *            version number received from client
     * @param sentenceAddress
     *            sentence address
     * @return Boolean false if mismatch else true
     * @throws Exception
     */
    public Boolean verifyVersion(String selectedFileName, int version, int sentenceAddress)
        throws Exception
    {
        // Create a JCas for modifying annotation data
        JCas document = CasCreationUtils.createCas((TypeSystemDescription) null, null, null)
                .getJCas();

        // Reading annotation and load to JCas
        loadFileToCas(selectedFileName, document);

        // Get the version details corresponding to sentence address
        Collection<Version> synch_param = select(document, Version.class);
        Version versiondetails = null;
        Iterator<Version> synch_param_iterator = synch_param.iterator();
        while (synch_param_iterator.hasNext()) {
            Version iteratorVersion = synch_param_iterator.next();
            if (iteratorVersion.getSentenceAddress() == sentenceAddress) {
                versiondetails = iteratorVersion;
                break;
            }
        }

        // check if client requesting has same version as the document at the server
        if (versiondetails != null) {
            return versiondetails.getVersion() == version ? true : false;
        }
        else {
            addVersion(selectedFileName, document, sentenceAddress);
            return false;
        }
    }

    /**
     * The version details are updated in this method
     * 
     * @param document
     *            the JCas object
     * @param sentenceAddress
     *            the sentence annotation address
     * @return
     */
    private int updateVersion(JCas document, int sentenceAddress)
    {

        // Get the current date
        DateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        Date todaysdate = new Date();

        // Get the version corresponding to the sentence address
        Collection<Version> synch_param = select(document, Version.class);
        Version versiondetails = null;
        Iterator<Version> synch_param_iterator = synch_param.iterator();
        while (synch_param_iterator.hasNext()) {
            Version iteratorVersion = synch_param_iterator.next();
            if (iteratorVersion.getSentenceAddress() == sentenceAddress) {
                versiondetails = iteratorVersion;
                break;
            }
        }

        // increment version number if version date matches current date
        if (versiondetails.getVersiondate().equals(sdf.format(todaysdate))) {
            versiondetails.setVersion(versiondetails.getVersion() + 1);
        }
        else {
            // If version date doesn't match current date
            versiondetails.setVersion(1); // set version number to 1
            versiondetails.setVersiondate(sdf.format(todaysdate)); // set date to todays date
        }

        // return the version number
        return versiondetails.getVersion();
    }

    /**
     * Add version details to the annotation data
     * 
     * @param selectedFileName
     *            the file for storing annotation data
     * @param document
     *            the CAS
     * @return the version number
     */
    private void addVersion(String selectedFileName, JCas document, int sentenceAddress)
        throws AnalysisEngineProcessException, ResourceInitializationException
    {

        // get todays date
        DateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        Date todaysdate = new Date();

        // add version details to CAS
        Version versiondetails = new Version(document);

        // Set initial version number to 1
        versiondetails.setVersion(1);
        versiondetails.setVersiondate(sdf.format(todaysdate));
        versiondetails.setSentenceAddress(sentenceAddress);

        // add version details to CAS index
        versiondetails.addToIndexes();

        // write the CAS to the file
        saveAnnotationdata(document, selectedFileName);
    }

    /**
     *             Source: https://github.com/webanno/webanno/blob/
     *             1dc7ca2054f3efcf1f84d2629b987c20eaa062e0/webanno-api-dao/src/
     *             main/java/de/tudarmstadt/ukp/clarin/webanno/api/dao/
     *             RepositoryServiceDbData.java#L2052 (accessed on 08.01.2017)
     */
    private void upgradeCAS(String filename, CAS document) throws IOException, UIMAException
    {
        TypeSystemDescription allTypes = TypeSystemDescriptionFactory.createTypeSystemDescription();
        CAS newCas = JCasFactory.createJCas(allTypes).getCas();
        CASCompleteSerializer serializer = Serialization.serializeCASComplete((CASImpl) newCas);

        // Save old type system
        TypeSystem oldTypeSystem = document.getTypeSystem();

        // Save old CAS contents
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        Serialization.serializeWithCompression(document, os2, oldTypeSystem);

        // Prepare CAS with new type system
        Serialization.deserializeCASComplete(serializer, (CASImpl) document);

        // Restore CAS data to new type system
        Serialization.deserializeCAS(document, new ByteArrayInputStream(os2.toByteArray()),
                oldTypeSystem, null);

        // Make sure JCas is properly initialized too
        document.getJCas();

        // write the CAS with updated type system to file
        saveAnnotationdata(document.getJCas(), filename);
    }

    /**
     * getAnnotation function reads the complete annotation data and form JSON structure for the
     * visualization of syntactic tree at client
     * 
     * @param selectedFile
     *            the selected file name
     * @return the annotation data in the form of nodes and edges
     */
    public String render(String selectedFile, String lastupdate)
        throws IOException, UIMAException
    {
        // List of nodes - Tokens and Constituents
        List<VNode> nodes = new ArrayList<>();

        // List of edges - parent-child relation between nodes expect with ROOT
        List<VEdge> edges = new ArrayList<>();

        List<int[]> text_offsets = new ArrayList<>(); // Text offset of annotation text
        String documenttext; // Text to be annotated
        int versionnumber; // To store the updated version number
        String selectedFileName; // To store the artifact ID
        int sentenceAddress; // To store the sentence address
        String[] splitFileDetails = selectedFile.split("#"); // get the file name and sentence ID

        if (splitFileDetails.length == 2) {
            selectedFileName = splitFileDetails[0];
            sentenceAddress = Integer.parseInt(splitFileDetails[1]);

            TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();

            // Create Cas object
            JCas document = CasCreationUtils.createCas(tsd, null, null).getJCas();

            // Start: Check if verification information already exist in the CAS metadata
            // Get Version annotation before loading the analysis data from file
            Version syncParam_before = new Version(document);

            // load annotation data to CAS
            loadFileToCas(selectedFileName, document);

            // Get Version annotation after loading the analysis data from file
            Version syncParam_after = new Version(document);

            // If the annotation ID does not match, the typesystem of CAS is not updated
            if (syncParam_after.getTypeIndexID() != syncParam_before.getTypeIndexID()) {
                // update type system of cas
                upgradeCAS(selectedFileName, document.getCas());

                // Inform user to select file again with new sentence address
                JSONObject item = new JSONObject();
                item.put(NOTIFICATION, "Please select the file again");
                return item.toString();
            }
            else {
                // get the version details
                Collection<Version> synch_param = select(document, Version.class);
                // If no version added yet
                if (synch_param.size() == 0) {
                    // Add version to sentence being annotated
                    addVersion(selectedFileName, document, sentenceAddress);
                    versionnumber = 1;
                }
                else {
                    // If version already exists, update version number
                    Version versiondetails = null;
                    Iterator<Version> synch_param_iterator = synch_param.iterator();
                    while (synch_param_iterator.hasNext()) {
                        Version iteratorVersion = synch_param_iterator.next();
                        if (iteratorVersion.getSentenceAddress() == sentenceAddress) {
                            versiondetails = iteratorVersion;
                            break;
                        }
                    }
                    if (versiondetails != null) {
                        versionnumber = versiondetails.getVersion();
                    }
                    else {
                        addVersion(selectedFileName, document, sentenceAddress);
                        versionnumber = 1;
                    }
                }
                // End: Check if verification information already exist in the CAS metadata

                // Get the sentence annotation using given address
                Sentence selectedSentence = getNodeByAddress(document, sentenceAddress);

                // Get the complete document text
                documenttext = document.getDocumentText();

                // get all Token with begin and end offset within Sentence annotation being and end
                Collection<Token> tokens = select(document, Token.class);
                tokens.forEach((singletoken) -> {
                    if (singletoken.getBegin() >= selectedSentence.getBegin()
                            && singletoken.getEnd() <= selectedSentence.getEnd()) {
                        int[] tmpoffset = new int[] { singletoken.getBegin(),
                                singletoken.getEnd() };
                        text_offsets.add(tmpoffset);
                        traverseToken(singletoken, nodes, edges);
                    }
                });

                // Convert Java objects to JSON string
                ObjectMapper mapper = new ObjectMapper();
                String offsetsToJson = mapper.writeValueAsString(text_offsets);
                String nodesToJson = mapper.writeValueAsString(nodes);
                String edgesToJson = mapper.writeValueAsString(edges);

                JSONObject item = new JSONObject();
                item.put(RENDERING_MODE, Mode.MODE_FULL.ordinal());
                item.put("text", documenttext);
                item.put("offset", offsetsToJson);
                item.put("nodes", nodesToJson);
                item.put("edges", edgesToJson);
                item.put("version", versionnumber);

                if (isNotBlank(lastupdate)) {
                    item.put(LAST_UPDATE, lastupdate);
                }
                item.put("selectedfile", selectedFile);

                return item.toString();
            }
        }
        else {
            return "";
        }
    }

    /**
     * This method traverse through a node followed by its parent and parent's parent until ROOT
     * annotation is reached
     */
    private void traverseToken(Annotation aAnno, List<VNode> aVNodes, List<VEdge> aVEdges)
    {
        // If annotation is of type Token
        if (aAnno instanceof Token) {
            // Check if Token has a valid pos value
            if (((Token) aAnno).getPos() != null) {
                VNode tempnode = new VNode();
                tempnode.setId(aAnno.getAddress());
                int[] tmpoffset = new int[] { aAnno.getBegin(), aAnno.getEnd() };
                tempnode.setOffset(tmpoffset);
                tempnode.setText(((Token) aAnno).getPos().getPosValue());
                // add POS annotation to list of tree nodes
                aVNodes.add(tempnode);

                // If Token has a parent which is Constituent annotation
                if ((((Token) aAnno).getParent() instanceof Constituent)
                        && !(((Token) aAnno).getParent() instanceof ROOT)) {
                    VEdge tempedge = new VEdge();
                    tempedge.setTo(((Token) aAnno).getParent().getAddress());
                    tempedge.setFrom(aAnno.getAddress());
                    tempedge.setId("e_" + tempedge.getTo() + "_" + tempedge.getFrom());
                    tempedge.setLabel(
                            isBlank(((Token) aAnno).getSyntacticFunction()) ? "none"
                                    : ((Token) aAnno).getSyntacticFunction());
                    // add the relation with parent to list of tree edges
                    aVEdges.add(tempedge);
                    // invoke the same method to iterate over parent annotation
                    traverseToken(((Token) aAnno).getParent(), aVNodes, aVEdges);
                }
            }
        }
        else if ((aAnno instanceof Constituent) && !(aAnno instanceof ROOT)) {
            // If annotation is of type Constituent
            VNode tempnode = new VNode();
            tempnode.setId(aAnno.getAddress());
            tempnode.setOffset(new int[2]);
            tempnode.setText(((Constituent) aAnno).getConstituentType());
            // If annotation is not yet added to list of nodes
            if (!tempnode.alreadyExist(aVNodes)) {
                // add Constituent annotation to list of tree nodes
                aVNodes.add(tempnode);

                // If Constituent has a parent which is Constituent annotation
                if ((((Constituent) aAnno).getParent() instanceof Constituent)
                        && !(((Constituent) aAnno).getParent() instanceof ROOT)) {
                    VEdge tempedge = new VEdge();
                    tempedge.setTo(((Constituent) aAnno).getParent().getAddress());
                    tempedge.setFrom(aAnno.getAddress());
                    tempedge.setId("e_" + tempedge.getTo() + "_" + tempedge.getFrom());
                    tempedge.setLabel(
                            isBlank(((Constituent) aAnno).getSyntacticFunction())
                                    ? "none"
                                    : ((Constituent) aAnno).getSyntacticFunction());
                    // add the relation with parent to list of tree edges
                    aVEdges.add(tempedge);
                    // invoke the same method to iterate over parent annotation
                    traverseToken(((Constituent) aAnno).getParent(), aVNodes, aVEdges);
                }
            }
        }
        else {
            // do nothing
        }
    }

    /**
     * loadFileToCas function read the annotation data from binary file and load
     * it to cas
     * 
     * @param selectedFileName
     *            the selected file name
     * @param document
     *            the cas object
     */
    private void loadFileToCas(String selectedFileName, JCas document)
        throws ResourceInitializationException, CollectionException, IOException
    {
        // Reading from the file
        CollectionReader binReader = createReader(BinaryCasReader.class,
                BinaryCasReader.PARAM_SOURCE_LOCATION, TARGET_BINARY_FILE + selectedFileName);

        // read the annotation from file and set to CAS
        binReader.getNext(document.getCas());
        binReader.close();
    }
    
    /**
     * saveAnnotationdata function writes the annotation data from cas to binary file
     * 
     * @param document
     *            the CAS with annotation data
     * @param selectedFileName
     *            the selected file name
     */
    private void saveAnnotationdata(JCas document, String selectedFileName)
        throws ResourceInitializationException, AnalysisEngineProcessException
    {
        // Init binary writer
        AnalysisEngine binWriter = createEngine(BinaryCasWriter.class, 
                BinaryCasWriter.PARAM_FORMAT, "S+", 
                BinaryCasWriter.PARAM_SINGULAR_TARGET, true,
                BinaryCasWriter.PARAM_TARGET_LOCATION, TARGET_BINARY_FILE + selectedFileName,
                BinaryCasWriter.PARAM_OVERWRITE, true);
        // write the cache to the file
        binWriter.process(document);
        binWriter.collectionProcessComplete();
        binWriter.destroy();
    }

    /**
     * getNodeByAddress function returns the annotation type with the given address
     * 
     * @param document
     *            the cas with annotation data
     * @param node_id
     *            the address of the node
     */
    private static <T extends TOP> T getNodeByAddress(JCas document, int node_id)
    {
        return document.getLowLevelCas().ll_getFSForRef(node_id);
    }

    /**
     * getTokenwithOffset function returns the Token with given offset
     * 
     * @param aJCas
     *            the CAS with annotation data
     * @param aBegin
     *            the beginning offset
     * @param aEnd
     *            the end offset
     */
    private static Token getTokenwithOffset(JCas aJCas, int aBegin, int aEnd)
    {
        Token currentToken = selectSingleAt(aJCas, Token.class, aBegin, aEnd);
        return currentToken;
    }

    /**
     * Get the list of all Sentence annotation information inside a JCas object
     */
    private void getSentences(JCas document, Map<Integer, String> sentence_list)
    {
        Collection<Sentence> sentences = select(document, Sentence.class);
        sentences.forEach((singleSentence) -> {
            sentence_list.put(singleSentence.getAddress(), singleSentence.getCoveredText());
        });
    }

    /**
     * getFiles function gets the list of annotation files and the sentences
     */
    public String getFiles()
        throws ResourceInitializationException, CollectionException, IOException, CASException
    {
        TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        JCas document = CasCreationUtils.createCas(tsd, null, null).getJCas();
        Map<String, Map<Integer, String>> file_list = new HashMap<String, Map<Integer, String>>();

        // Get all files in the filesystem
        File[] files = new File(TARGET_BINARY_FILE).listFiles();
        if (files != null) {
            // Iterate over all the files and map each file with an array of all the sentence
            // related information
            for (File file : files) {
                if (file.isFile()) {
                    document.reset();
                    loadFileToCas(file.getName(), document);
                    Map<Integer, String> sentence_list = new HashMap<Integer, String>();
                    getSentences(document, sentence_list);
                    file_list.put(file.getName(), sentence_list);
                }
            }
        }

        // convert annotation data into JOSN format
        ObjectMapper mapper = new ObjectMapper();
        JSONObject item = new JSONObject();
        item.put("files", mapper.writeValueAsString(file_list));

        // return result in JSON form
        return item.toString();

    }

    /**
     * This method performs tokenization on the text sentence(s)
     */
    public String createUnAnnotatedCorpus(String filename, String filedata) throws Exception
    {
        // Create JCas object with given text
        JCas document = JCasFactory.createText(filedata, "en");
        DocumentMetaData dmd = DocumentMetaData.create(document);
        dmd.setDocumentId(filename);

        // Create tokenizer engine
        AnalysisEngine tokenizer = createEngine(StanfordSegmenter.class);
        // Create writer engine
        AnalysisEngine writer = createEngine(BinaryCasWriter.class,
                BinaryCasWriter.PARAM_TARGET_LOCATION, 
                    TARGET_BINARY_FILE + filename + BINARY_FILE_EXT,
                BinaryCasWriter.PARAM_FORMAT, "S+", 
                BinaryCasWriter.PARAM_SINGULAR_TARGET, true,
                BinaryCasWriter.PARAM_OVERWRITE, true);
        // Perform tokenization and write to a file
        SimplePipeline.runPipeline(document, tokenizer, writer);

        // add ROOT annotations corresponding to each Sentence annotation
        Collection<Sentence> sentence = select(document, Sentence.class);
        sentence.forEach((singleSentence) -> {
            ROOT root = new ROOT(document);
            root.setBegin(singleSentence.getBegin());
            root.setEnd(singleSentence.getEnd());
            root.addToIndexes();
        });
        // save the result to the file same file
        saveAnnotationdata(document, filename + BINARY_FILE_EXT);

        // Notify the user for successful request processing
        JSONObject item = new JSONObject();
        item.put(NOTIFICATION,
                "The corpus successfully added to a file. Please select to start annotation");
        return item.toString();
    }
    
    /**
     * This method performs tokenization and segmentation on the text sentence(s) to get a
     * pre-annotated result
     */
    public String createCorpus(String filename, String filedata) throws Exception
    {
        // Create JCas object with given text
        JCas document = JCasFactory.createText(filedata, "en");
        DocumentMetaData dmd = DocumentMetaData.create(document);
        dmd.setDocumentId(filename);

        // Create tokenizer engine
        AnalysisEngine tokenizer = createEngine(StanfordSegmenter.class);
        // Create parser engine
        AnalysisEngine parser = createEngine(StanfordParser.class, 
                StanfordParser.PARAM_READ_POS, false, 
                StanfordParser.PARAM_WRITE_POS, true);
        // Create writer engine
        AnalysisEngine writer = createEngine(BinaryCasWriter.class,
                BinaryCasWriter.PARAM_TARGET_LOCATION,
                TARGET_BINARY_FILE + filename + BINARY_FILE_EXT,
                BinaryCasWriter.PARAM_FORMAT, "S+", 
                BinaryCasWriter.PARAM_SINGULAR_TARGET, true,
                BinaryCasWriter.PARAM_OVERWRITE, true);
        // Run the three engine on JCas object using in a pipeline
        SimplePipeline.runPipeline(document, tokenizer, parser, writer);

        // Notify the user for successful request processing
        JSONObject item = new JSONObject();
        item.put(NOTIFICATION,
                "The corpus successfully added to a file. Please select to start annotation");
        return item.toString();
    }
}
