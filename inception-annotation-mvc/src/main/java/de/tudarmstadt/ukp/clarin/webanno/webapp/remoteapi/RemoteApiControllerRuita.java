/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi;

import static de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil.isProjectAdmin;
import static de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil.isSuperAdmin;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSame;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAt;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.v2.model.RMessageLevel.ERROR;
import static java.util.Arrays.asList;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;
import javax.persistence.NoResultException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.AnnotationJSONObject;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.AnnotationLayerJSONObject;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.CreateAndUpdateOutputMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.CreateAnnotationJSONInfo;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.CreateOutputMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.DeleteOutputMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.DocumentJSONObject;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.FeatureInfo;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.FeatureRef;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.JSONOutput;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.Message;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.ProjectJSONObject;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.TagSetJSONObject;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.TokenJSONObject;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.UpdateAnnotationJSONInfo;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects.UpdateOutputMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.v2.exception.AccessForbiddenException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.v2.exception.ObjectNotFoundException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.v2.exception.RemoteApiException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.v2.model.RProject;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.v2.model.RResponse;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;

@SwaggerDefinition(info = @Info(title = "WebAnno Remote API ruita", version = "3"))
@RequestMapping(RemoteApiControllerRuita.API_BASE)
@Controller
public class RemoteApiControllerRuita
{
    public static final String API_BASE = "/api/ruita";

    private static final String PROJECTS = "projects";
    private static final String DOCUMENTS = "documents";
    private static final String ANNOTATIONS = "annotations";
    private static final String TAGSETS = "tagSets";
    private static final String LAYERS = "layers";
    private static final String TOKENS = "tokens";

    private static final String PARAM_PROJECT_ID = "projectId";
    private static final String PARAM_DOCUMENT_ID = "documentId";
    private static final String PARAM_TAGSET_ID = "tagSetId";
    private static final String PARAM_LAYER_ID = "layerId";
    private static final String PARAM_TOKEN_ID = "tokenId";

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private @Resource DocumentService documentService;
    private @Resource CurationDocumentService curationService;
    private @Resource ProjectService projectService;
    private @Resource ImportExportService importExportService;
    private @Resource AnnotationSchemaService annotationService;
    private @Resource UserDao userRepository;
    private @Resource FeatureSupportRegistry featureSupportRegistry;

    /**
     * API function that returns information about all the layer definitions of a project
     * (annotation schema).
     * 
     * @param aProjectId
     *            the project where the layers are defined.
     * @return ResponseEntity that contains http status code and information about the layers in the
     *         given project as JSON.
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @ApiOperation(value = "Get the layernames and features of a project as json")
    @RequestMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/"
            + LAYERS, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collection<AnnotationLayerJSONObject>> getLayers(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        ArrayList<AnnotationLayerJSONObject> outputList = new ArrayList<>();

        Project project = getProject(aProjectId);

        ArrayList<AnnotationLayer> layerls = (ArrayList<AnnotationLayer>) annotationService
                .listAnnotationLayer(project);
        for (AnnotationLayer layer : layerls) {
            // Skip Token because we want to treat it as a separate Object (not an Annotation)
            if ("Token".equals(layer.getUiName())) {
                continue;
            }
            AnnotationLayerJSONObject aljo = getAljoOfAnnotationLayer(layer);
            outputList.add(aljo);
        }

        return new ResponseEntity<Collection<AnnotationLayerJSONObject>>(outputList, HttpStatus.OK);
    }

    /**
     * API function that returns information about a specific layer definition of a project.
     * 
     * @param aProjectId
     *            the project where the layer is defined.
     * @param aLayerId
     *            layer about which information is provided.
     * @return ResponseEntity that contains http status code and information about the specific
     *         layer as JSON.
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @ApiOperation(value = "Get a layername and its features specified by an id")
    @RequestMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + LAYERS + "/{"
            + PARAM_LAYER_ID
            + "}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnnotationLayerJSONObject> getSingleLayer(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_LAYER_ID) long aLayerId)
        throws Exception
    {

        AnnotationLayer layer = annotationService.getLayer(aLayerId);
        AnnotationLayerJSONObject aljo = getAljoOfAnnotationLayer(layer);

        return new ResponseEntity<AnnotationLayerJSONObject>(aljo, HttpStatus.OK);
    }

    /**
     * API function that returns all tokens of a document. If only one of the optional parameters is
     * set, they are ignored.
     * 
     * @param aProjectId
     *            the project that contains the document.
     * @param aDocumentId
     *            the document that contains the text/tokens.
     * @param from
     *            start index of sublist from all tokens.
     * @param to
     *            end index of sublist from all tokens.
     * @return ResponseEntity that contains http status code and information about all the tokens of
     *         the document in a JSON Array.
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @ApiOperation(value = "Get the tokens of a document. Number can be specified")
    @RequestMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
            + PARAM_DOCUMENT_ID + "}/"
            + TOKENS, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collection<TokenJSONObject>> getTokens(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @RequestParam("from") Optional<Integer> from, @RequestParam("to") Optional<Integer> to)
        throws Exception
    {
        Project project = getProject(aProjectId);
        SourceDocument doc = getDocument(project, aDocumentId);

        // Get AnnotationDocument
        AnnotationDocument annoDoc = getAnnotation(doc, getCurrentUser(), true);

        // JCas jcas = documentService.readAnnotationCas(annoDoc, 'r');
        JCas jcas = documentService.readAnnotationCas(annoDoc);

        // Get list with Tokens from JCas
        Collection<Token> tokenColl = JCasUtil.select(jcas, Token.class);
        ArrayList<TokenJSONObject> tokenJSONList = new ArrayList<TokenJSONObject>();
        for (Token token : tokenColl) {
            TokenJSONObject tjo = new TokenJSONObject();
            tjo.setTokenId(token.getAddress());
            tjo.setCoveredText(token.getCoveredText());
            tjo.setBegin(token.getBegin());
            tjo.setEnd(token.getEnd());
            tokenJSONList.add(tjo);
        }
        if (from.isPresent() && to.isPresent()) {
            int toIndex;
            if (to.get() >= tokenJSONList.size()) {
                toIndex = tokenJSONList.size();
            }
            else {
                toIndex = to.get();
            }
            tokenJSONList = new ArrayList<TokenJSONObject>(
                    tokenJSONList.subList(from.get(), toIndex));

        }

        return new ResponseEntity<Collection<TokenJSONObject>>(tokenJSONList, HttpStatus.OK);

    }

    /**
     * API function that creates a new annotation.If the updateInformation is set properly inside of
     * the createInfo(see class definitions) the featureValues are also set after the creation.
     * Multiple featureValues can be set at once.
     * 
     * @param aProjectId
     *            the project that contains the document which is to be annotated.
     * @param aDocumentId
     *            document which is to be annotated.
     * @param createInfo
     *            information that is needed to create the annotations(set the FeatureValue). See
     *            class definition.
     * @return ResponseEntity that contains http status code and information whether the process was
     *         successful.
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @Transactional
    @ApiOperation(value = "Create a new Annotation")
    @RequestMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
            + PARAM_DOCUMENT_ID + "}/"
            + ANNOTATIONS, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<? extends Message> createAnnotation(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @RequestBody CreateAnnotationJSONInfo createInfo)
        throws Exception
    {
        Project project = getProject(aProjectId);
        SourceDocument doc = getDocument(project, aDocumentId);

        int newAnnotationId = -1;
        // Check which type of annotation we have
        AnnotationLayer layer = annotationService.getLayer(createInfo.getLayerId());
        String layerUiName = layer.getUiName();

        // If it is chain or relation begin and end are Ids
        boolean treatLimitsAsIds = false;
        if ("chain".equals(layer.getType()) || "relation".equals(layer.getType())) {
            treatLimitsAsIds = true;
        }

        // Get AnnotationDocument
        AnnotationDocument annoDoc = getAnnotation(doc, getCurrentUser(), true);

        // TODO: Upgrade CAS if there is a new Layer
        //JCas jcas = documentService.readAnnotationCas(annoDoc, 'w');
        JCas jcas = documentService.readAnnotationCas(annoDoc);

        try (AutoCloseable AutoJcas = (AutoCloseable) jcas) {

            int begin = createInfo.getBegin();
            int end = createInfo.getEnd();

            if (documentService.isAnnotationFinished(doc, getCurrentUser())) {
                throw new AnnotationException("This document is already closed. Please ask your "
                        + "project manager to re-open it via the Monitoring page");
            }

            if (!layer.isAllowStacking() && isLayerExisting(jcas, layer, project, begin, end)) {
                LOG.warn("Layer not stackable.");
                return new ResponseEntity<Message>(new Message("Layer not stackable."),
                        HttpStatus.BAD_REQUEST);
            }

            if (!AnchoringMode.TOKENS.equals(layer.getAnchoringMode())
                    && JCasUtil.selectAt(jcas, Token.class, begin, end).size() > 1) {
                LOG.warn("Layer does not allow multiple token annotations.");
                return new ResponseEntity<Message>(
                        new Message("Layer does not allow multiple token annotations."),
                        HttpStatus.BAD_REQUEST);

            }

            if (!layer.isCrossSentence()) {
                // If begin and end are the acctuall begin and end of the annotation
                if (!treatLimitsAsIds) {
                    if (!WebAnnoCasUtil.isSameSentence(jcas, begin, end)) {
                        LOG.warn("Layer does not allow cross sentence annotations.");
                        return new ResponseEntity<Message>(
                                new Message("Layer does not allow cross sentence annotations."),
                                HttpStatus.BAD_REQUEST);
                    }
                }
                // If begin and end are Ids of other annotations e.g dependency
                else {
                    if (!areAnnotationsSameSentence(jcas, begin, end)) {
                        LOG.warn("Layer does not allow cross sentence annotations.");
                        return new ResponseEntity<Message>(
                                new Message("Layer does not allow cross sentence annotations."),
                                HttpStatus.BAD_REQUEST);
                    }
                }
            }

            TypeAdapter adapter = annotationService.getAdapter(layer);

            // Load the feature editors with the remembered values (if any)

            // In case the schema has been changed but the cas has not been upgraded yet
            // we get an exception here. So we try to work around that by catching the exception,
            // upgrading the cas and than try again to create the annotation
            try {
                newAnnotationId = createNewAnnotation(adapter, doc, jcas, layer, begin, end);
            }
            catch (IllegalArgumentException e) {
                annotationService.upgradeCas(jcas.getCas(), annoDoc);
                newAnnotationId = createNewAnnotation(adapter, doc, jcas, layer, begin, end);
            }
            // Check if the annotation was created with the given parameters
            AnnotationFS newAnno = WebAnnoCasUtil.selectByAddr(jcas, newAnnotationId);
            String outputMessageAddition = "";
            // Inform the user if the annotation's begin and end has been changed due to
            // locking the annotation to Token offsets
            if (newAnno.getBegin() != begin || newAnno.getEnd() != end) {
                outputMessageAddition = " Note: Begin and End of the annotation have been"
                        + "changed. This can happen if the layer is locked to a token"
                        + " offset. New Values: begin:" + newAnno.getBegin() + ",end:"
                        + newAnno.getEnd();
            }

            // Set FeatureValue if requested
            if (createInfo.getUpdateInfoList() != null) {
                // List with information about every single update/put
                ArrayList<HashMap<String, Object>> updateOutputList = new ArrayList<>();

                ArrayList<UpdateAnnotationJSONInfo<Object>> updateInfoList = createInfo
                        .getUpdateInfoList();

                for (UpdateAnnotationJSONInfo<Object> updateInfo : updateInfoList) {
                    // Set the annotationId that was just created
                    updateInfo.setAnnotationId(newAnnotationId);
                    // HashMap for a single update response
                    HashMap<String, Object> resValues = new HashMap<>();
                    resValues.put("updatedFeatureId", updateInfo.getFeatureId());
                    // invoke update function
                    try {
                        // Save most important Values of Response in Hashmap
                        ResponseEntity<? extends Message> resEnt = updateAnnotation(aProjectId,
                                aDocumentId, updateInfo);
                        resValues.put("updateStatusCode", resEnt.getStatusCode().value());
                        resValues.put("updateMessage", resEnt.getBody());
                    }
                    catch (Exception e) {
                        resValues.put("updateStatusCode",
                                (HttpStatus.INTERNAL_SERVER_ERROR).value());
                        resValues.put("updateMessage", new Message(e.getMessage()));
                        throw e;
                    }
                    // Add updateResponse to List
                    updateOutputList.add(resValues);
                }

                return new ResponseEntity<Message>(
                        new CreateAndUpdateOutputMessage(
                                "Create successful." + outputMessageAddition, newAnnotationId,
                                createInfo.getLayerId(), layerUiName, updateOutputList),
                        HttpStatus.OK);

            }

            return new ResponseEntity<Message>(
                    new CreateOutputMessage("Create successful." + outputMessageAddition,
                            newAnnotationId, createInfo.getLayerId(), layerUiName),
                    HttpStatus.OK);
        }
    }

    /**
     * Returns true if two given annotations are in the same sentence. This means that the range
     * between begin of the first and the end of the second one is within one sentence.
     * 
     * @param aJCas
     *            the jas
     * @param id1
     *            id of first annotation
     * @param id2
     *            id of second annotation
     * @return whether the annotations are in the same sentence
     */
    private boolean areAnnotationsSameSentence(JCas aJCas, int id1, int id2)
    {
        AnnotationFS anno1 = WebAnnoCasUtil.selectByAddr(aJCas, id1);
        AnnotationFS anno2 = WebAnnoCasUtil.selectByAddr(aJCas, id2);

        if (!WebAnnoCasUtil.isSameSentence(aJCas, anno1.getBegin(), anno2.getEnd())) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if there already exists the same layer as the given one within this begin and
     * end
     * 
     * @param jCas
     *            the jcas
     * @param layer
     *            the layer to check
     * @param project
     *            the project
     * @param begin
     *            begin of the range
     * @param end
     *            end of the range
     * @return whether this layer already exists that this position in the text
     */
    private boolean isLayerExisting(JCas jCas, AnnotationLayer layer, Project project, int begin,
            int end)
    {
        List<Annotation> list = JCasUtil.selectAt(jCas, Annotation.class, begin, end);

        for (Annotation anno : list) {
            if (annotationService.getLayer(project, anno).getName().equals(layer.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * API function that updates an existing annotation, meaning that a specified featureValue of
     * the annotation is set.The feature is specified by id in the requestBody.(See class
     * definition)
     * 
     * @param aProjectId
     *            the project that contains the document which is to be annotated.
     * @param aDocumentId
     *            document which is to be annotated.
     * @param updateInfo
     *            information that is needed to set the featureValue.(See Class definition)
     * @return ResponseEntity that contains http status code and information whether the update
     *         process was successful.
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @Transactional
    @ApiOperation(value = "Update an annotation by setting a specified feature")
    @RequestMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
            + PARAM_DOCUMENT_ID + "}/"
            + ANNOTATIONS, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, 
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<? extends Message> updateAnnotation(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @RequestBody UpdateAnnotationJSONInfo<Object> updateInfo)
        throws Exception
    {
        Project project = getProject(aProjectId);
        SourceDocument doc = getDocument(project, aDocumentId);
        Message outputMessage = new Message();
        int annotationId = updateInfo.getAnnotationId();
        int featureId = updateInfo.getFeatureId();
        Object featureValueObject = updateInfo.getValue();

        // Get AnnotationDocument
        AnnotationDocument annoDoc = getAnnotation(doc, getCurrentUser(), false);
        // JCas jcas = documentService.readAnnotationCas(annoDoc, 'w');
        JCas jcas = documentService.readAnnotationCas(annoDoc);

        try (AutoCloseable AutoJcas = (AutoCloseable) jcas) {
            AnnotationFS annotation;
            annotation = WebAnnoCasUtil.selectByAddr(jcas, annotationId);

            AnnotationLayer layer = annotationService.getLayer(project, annotation);
            String typeName = layer.getType();

            if (layer.isReadonly()) {
                outputMessage.setText("Layer of the specified Annotation is Readonly.");
                return new ResponseEntity<Message>(outputMessage, HttpStatus.BAD_REQUEST);
            }

            AnnotationFeature annoFeat = annotationService.getFeature(featureId);
            boolean existsFeature = annotationService.existsFeature(annoFeat.getName(), layer);

            TypeAdapter adapter = annotationService.getAdapter(layer);
            // Set the featureValue according to the type of the feature
            MultiValueMode mvMode = annoFeat.getMultiValueMode();
            Object featValue;
            if (MultiValueMode.ARRAY.equals(mvMode)) {
                ArrayList<HashMap<String, String>> roleLabelInfoList = 
                        ((ArrayList<HashMap<String, String>>) featureValueObject);
                // Extend the old feature-list
                ArrayList<LinkWithRoleModel> lwrmList = adapter.getFeatureValue(annoFeat,
                        annotation);
                for (HashMap<String, String> hm : roleLabelInfoList) {
                    LinkWithRoleModel lwrm = new LinkWithRoleModel();
                    lwrm.label = hm.get("label");
                    lwrm.role = hm.get("role");
                    lwrm.targetAddr = Integer.parseInt(hm.get("targetId"));
                    lwrmList.add(lwrm);
                }
                featValue = lwrmList;

            }
            else {
                featValue = (String) featureValueObject;
            }

            if (existsFeature) {
                // Create a dummy annotator state
                AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
                state.setDocument(doc, asList(doc));
                state.setUser(getCurrentUser());
                
                // By default, if the feature with this featureId is already set it's just
                // overwritten

                // This differentiation is not really needed here at the moment. But it may
                // be useful in the future when featureValues become more complex. Also it won't
                // cause any performance issues so i leave it here.
                if ("span".equals(typeName)) {
                    ((SpanAdapter) adapter).setFeatureValue(state, jcas, annotationId, annoFeat,
                            featValue);
                }
                else if ("relation".equals(typeName)) {
                    ((ArcAdapter) adapter).setFeatureValue(state, jcas, annotationId, annoFeat,
                            featValue);
                }
                else if ("chain".equals(typeName)) {
                    ((ChainAdapter) adapter).setFeatureValue(state, jcas, annotationId, annoFeat,
                            featValue);
                }
            }
            else {
                outputMessage.setText(
                        "Feature with Id " + featureId + " does not exist for this annotation");
                return new ResponseEntity<Message>(outputMessage, HttpStatus.BAD_REQUEST);
            }

        }

        return new ResponseEntity<UpdateOutputMessage<Object>>(
                new UpdateOutputMessage<Object>("Annotation updated", featureValueObject),
                HttpStatus.OK);

    }

    /**
     * API functions that deletes a specified annotation. The annotation in specified in the
     * requestBody. (See class definition) This also deletes annotations that are dependent from the
     * specified annotation.
     * 
     * @param aProjectId
     *            the project that contains the document which is to be annotated.
     * @param aDocumentId
     *            document which is to be annotated.
     * @param aAnnotationId
     *            annotation to be deleted.
     * @return ResponseEntity and information about whether the delete process was successful and
     *         which attached annotations have been deleted in cascade.
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @Transactional
    @ApiOperation(value = "Delete an annotation specified by its Id")
    @RequestMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
            + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS
            + "/{annotationId}", method = RequestMethod.DELETE, produces = 
            MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Message> deleteAnnotation(@PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @PathVariable("annotationId") long aAnnotationId)
        throws Exception
    {
        // Prepare output Message
        Message message = new Message();
        Project project = getProject(aProjectId);
        SourceDocument doc = getDocument(project, aDocumentId);

        // Safe all the addresses of Annotations that where deleted because of the request
        ArrayList<Integer> forcedDeletionsList = new ArrayList<>();

        // Get AnnotationDocument
        AnnotationDocument annoDoc = getAnnotation(doc, getCurrentUser(), false);

        //JCas jcas = documentService.readAnnotationCas(annoDoc, 'w');
        JCas jcas = documentService.readAnnotationCas(annoDoc);

        try (AutoCloseable AutoJcas = (AutoCloseable) jcas) {
            AnnotationFS fs = WebAnnoCasUtil.selectByAddr(jcas,
                    Integer.parseInt(Long.toString(aAnnotationId)));
            AnnotationLayer layer = annotationService.getLayer(project, fs);
            TypeAdapter adapter = annotationService.getAdapter(layer);

            if (layer.isReadonly()) {
                LOG.warn("ERROR: Cannot delete an annotation on a read-only layer.");
                message.setText("Cannot delete an annotation on a read-only layer.");
                return new ResponseEntity<Message>(message, HttpStatus.BAD_REQUEST);
            }

            // == DELETE ATTACHED RELATIONS ==
            // If the deleted FS is a span, we must delete all relations that
            // point to it directly or indirectly via the attachFeature.
            //
            // NOTE: It is important that this happens before UNATTACH SPANS since the attach
            // feature
            // is no longer set after UNATTACH SPANS!
            if (adapter instanceof SpanAdapter) {
                // Create a dummy annotator state
                AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
                state.setDocument(doc, asList(doc));
                state.setUser(getCurrentUser());
                
                for (AnnotationFS attachedFs : getAttachedRels(fs, layer)) {
                    AnnotationLayer attachedFSLayer = annotationService.getLayer(project,
                            attachedFs);
                    TypeAdapter adapterForAttachedFS = annotationService
                            .getAdapter(attachedFSLayer);
                    adapterForAttachedFS.delete(state, jcas, new VID(attachedFs));

                    int deletedAddr = WebAnnoCasUtil.getAddr(attachedFs);

                    forcedDeletionsList.add(deletedAddr);
                    LOG.info("INFO: The attached annotation for relation type [" + annotationService
                            .getLayer(attachedFs.getType().getName(), project).getUiName()
                            + "] is deleted");
                }
            }
            // == UNATTACH SPANS ==
            // If the deleted FS is a span that is attached to another span, the
            // attachFeature in the other span must be set to null. Typical example: POS is deleted,
            // so
            // the pos feature of Token must be set to null. This is a quick case, because we only
            // need
            // to look at span annotations that have the same offsets as the FS to be deleted.
            if (adapter instanceof SpanAdapter && layer.getAttachType() != null
                    && layer.getAttachFeature() != null) {
                Type spanType = CasUtil.getType(jcas.getCas(), layer.getAttachType().getName());
                Feature attachFeature = spanType
                        .getFeatureByBaseName(layer.getAttachFeature().getName());
                for (AnnotationFS attachedFs : getAttachedSpans(fs, layer)) {
                    attachedFs.setFeatureValue(attachFeature, null);
                    LOG.info("Unattached [" + attachFeature.getShortName() + "] on annotation ["
                            + getAddr(attachedFs) + "]");
                }
            }

            // == CLEAN UP LINK FEATURES ==
            // If the deleted FS is a span that is the target of a link feature, we must unset that
            // link and delete the slot if it is a multi-valued link. Here, we have to scan all
            // annotations from layers that have link features that could point to the FS
            // to be deleted: the link feature must be the type of the FS or it must be generic.
            if (adapter instanceof SpanAdapter) {
                for (AnnotationFeature linkFeature : annotationService
                        .listAttachedLinkFeatures(layer)) {
                    Type linkType = CasUtil.getType(jcas.getCas(),
                            linkFeature.getLayer().getName());

                    for (AnnotationFS linkFS : CasUtil.select(jcas.getCas(), linkType)) {
                        List<LinkWithRoleModel> links = adapter.getFeatureValue(linkFeature,
                                linkFS);
                        Iterator<LinkWithRoleModel> i = links.iterator();
                        boolean modified = false;
                        while (i.hasNext()) {
                            LinkWithRoleModel link = i.next();
                            if (link.targetAddr == getAddr(fs)) {
                                i.remove();
                                LOG.info("Cleared slot [" + link.role + "] in feature ["
                                        + linkFeature.getName() + "] on annotation ["
                                        + getAddr(linkFS) + "]");
                                modified = true;
                            }
                        }
                        if (modified) {
                            WebAnnoCasUtil.setFeature(linkFS, linkFeature, links);
                        }
                    }
                }
            }

            // If the deleted FS is a relation, we don't have to do anything. Nothing can point to a
            // relation.
            if (adapter instanceof ArcAdapter) {
                // Do nothing ;)
            }

            // Create a dummy annotator state
            AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
            state.setDocument(doc, asList(doc));
            state.setUser(getCurrentUser());
            
            // Actually delete annotation
            adapter.delete(state, jcas, new VID(fs));

            message = new DeleteOutputMessage("Annotation was successfully deleted",
                    forcedDeletionsList, layer.getId(), layer.getUiName(), aAnnotationId);

            // Store CAS again
            // documentService.writeAnnotationCas(jcas, doc, getCurrentUser(), true);

            return new ResponseEntity<Message>(message, HttpStatus.OK);
        }

    }

    /**
     * API function that return information about a specified token of a document.
     * 
     * @param aProjectId
     *            the project that contains the document.
     * @param aDocumentId
     *            the document that contains the tokens.
     * @param aTokenId
     *            the specified token.
     * @return ResponseEntity and information about the specified token
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @ApiOperation(value = "Get a single Token by Id")
    @RequestMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
            + PARAM_DOCUMENT_ID + "}/" + TOKENS + "/{" + PARAM_TOKEN_ID
            + "}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenJSONObject> getToken(@PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @PathVariable("tokenId") long aTokenId)
        throws Exception
    {
        Project project = getProject(aProjectId);
        SourceDocument doc = getDocument(project, aDocumentId);

        // Get AnnotationDocument
        AnnotationDocument annoDoc = getAnnotation(doc, getCurrentUser(), true);

        // JCas jcas = documentService.readAnnotationCas(annoDoc, 'r');
        JCas jcas = documentService.readAnnotationCas(annoDoc);

        // Get list with Tokens from JCas
        Collection<Token> tokenColl = JCasUtil.select(jcas, Token.class);
        TokenJSONObject tjo = new TokenJSONObject();
        for (Token token : tokenColl) {
            if (token.getAddress() == aTokenId) {
                tjo.setTokenId(token.getAddress());
                tjo.setCoveredText(token.getCoveredText());
                tjo.setBegin(token.getBegin());
                tjo.setEnd(token.getEnd());
                break;
            }
        }

        return new ResponseEntity<TokenJSONObject>(tjo, HttpStatus.OK);

    }

    /**
     * API function that returns information about annotations of a given document. The number of
     * returned annotations can be varied as well as the textRange in which annotations are
     * searched. Also an annotation type (layer) can be specified. Parameters the need two
     * values(from,to) are ignored if only one is passed. Per default all parameters are set in a
     * way that they do not reduce the number of returned annotations.
     * 
     * @param aProjectId
     *            the project that contains the annotated document
     * @param aDocumentId
     *            the annotated document
     * @param annotationFrom
     *            start index of sublist from all annotations.
     * @param annotationTo
     *            start index of sublist from all annotations.
     * @param textIndexFrom
     *            start index of text range where annotations are searched in.
     * @param textIndexTo
     *            end index of text range where annotations are searched in.
     * @param layerFilter
     *            specified layer to filter for.
     * @return ResponseEntity that contains http status code and information about all requested
     *         annotations.
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @ApiOperation(value = "Get the annotations of a document as json.This method will return all annotations")
    @RequestMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
            + PARAM_DOCUMENT_ID + "}/"
            + ANNOTATIONS, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collection<AnnotationJSONObject>> documentAnnotations(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @RequestParam("annotationFrom") Optional<Integer> annotationFrom,
            @RequestParam("annotationTo") Optional<Integer> annotationTo,
            @RequestParam("textIndexFrom") Optional<Integer> textIndexFrom,
            @RequestParam("textIndexTo") Optional<Integer> textIndexTo,
            @RequestParam("layer") Optional<String> layerFilter)
        throws Exception
    {
        // Get required files/data (also ensures it exists and that the current user can access it
        Project project = getProject(aProjectId);
        SourceDocument doc = getDocument(project, aDocumentId);

        // Get AnnotationDocument
        AnnotationDocument annoDoc = getAnnotation(doc, getCurrentUser(), true);

        // JCas jcas = documentService.readAnnotationCas(annoDoc, 'r');
        JCas jcas = documentService.readAnnotationCas(annoDoc);

        // Get Collection with all Annotations from JCas
        Collection<Annotation> annotationColl = JCasUtil.select(jcas, Annotation.class);

        ArrayList<AnnotationJSONObject> outputList = new ArrayList<AnnotationJSONObject>();

        // Iterate through annotations to generate a corresponding AnnotationJSONObject for each one
        for (Annotation a : annotationColl) {
            String layerName = a.getType().getShortName();
            // We don't want to treat this types as annotations in our json
            if (!("Sentence".equals(layerName) || "DocumentMetaData".equals(layerName)
                    || "Token".equals(layerName))) {

                String layerUiN = annotationService.getLayer(project, a).getUiName().toLowerCase();
                // If the annotation is not the layer we want or not in the text range, leave it out
                if (!((layerFilter.isPresent()
                        && !(layerUiN.equals(layerFilter.get().toLowerCase())))
                        || (textIndexFrom.isPresent() && textIndexTo.isPresent()
                                && (a.getBegin() < textIndexFrom.get()
                                        || a.getBegin() >= textIndexTo.get())))) {
                    AnnotationJSONObject ajo = getJSONObjFromAnnotation(a, project);
                    if (ajo != null) {
                        outputList.add(ajo);
                    }
                }
            }
        }
        // Filter number of annotations
        if (annotationFrom.isPresent() && annotationTo.isPresent()) {
            // If a number of annotations is specified filter the list
            int toIndex;
            if (annotationTo.get() >= outputList.size()) {
                toIndex = outputList.size();
            }
            else {
                toIndex = annotationTo.get();
            }
            outputList = new ArrayList<AnnotationJSONObject>(
                    outputList.subList(annotationFrom.get(), toIndex));

        }

        return new ResponseEntity<Collection<AnnotationJSONObject>>(outputList, HttpStatus.OK);
    }

    /**
     * API function that returns information about all tagSets defined in a given project.
     * 
     * @param aProjectId
     *            the project where the tagSets are defined.
     * @return ResponseEntity that contains http status code and information about all tagSets in a
     *         JSON Array
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @ApiOperation(value = "Get all TagSets defined in a project")
    @RequestMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/"
            + TAGSETS, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collection<TagSetJSONObject>> getTagSets(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        Project project = getProject(aProjectId);
        ArrayList<TagSet> tagSetList = (ArrayList<TagSet>) annotationService.listTagSets(project);
        ArrayList<TagSetJSONObject> tsjoList = new ArrayList<>();
        for (TagSet ts : tagSetList) {
            TagSetJSONObject tsjo = new TagSetJSONObject();
            tsjo.setId(ts.getId());
            tsjo.setName(ts.getName());
            tsjo.setDescription(ts.getDescription());
            ArrayList<String> tagNames = new ArrayList<>();
            List<Tag> tagList = annotationService.listTags(ts);
            for (Tag t : tagList) {
                tagNames.add(t.getName());
            }
            tsjo.setTagNames(tagNames);
            tsjoList.add(tsjo);
        }

        return new ResponseEntity<Collection<TagSetJSONObject>>(tsjoList, HttpStatus.OK);

    }

    /**
     * API function that returns information about a specific tagSet.The returned tag list of the
     * tagSet can be filtered by request parameters.
     * 
     * @param aProjectId
     *            the project where the tagSets is defined.
     * @param aTagSetId
     *            id to specify the tagSet
     * @param number
     *            number of tags in the taglist
     * @param currentInput
     *            startString of the tags in the taglist
     * @return ResponsEntity that contains http status code and information about the requested
     *         tagSet(Tag list may be not complete due to filter/parameter options)
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @ApiOperation(value = "Get TagSet by Id")
    @RequestMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + TAGSETS + "/{"
            + PARAM_TAGSET_ID
            + "}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TagSetJSONObject> getTagSet(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_TAGSET_ID) long aTagSetId,
            @RequestParam("number") Optional<Integer> number,
            @RequestParam("currentInput") Optional<String> currentInput)
        throws Exception
    {
        TagSet tagSet = annotationService.getTagSet(aTagSetId);
        TagSetJSONObject tsjo = new TagSetJSONObject();

        tsjo.setId(tagSet.getId());
        tsjo.setName(tagSet.getName());
        tsjo.setDescription(tagSet.getDescription());
        ArrayList<String> tagNames = new ArrayList<>();
        List<Tag> tagList = annotationService.listTags(tagSet);
        // Set this default to -1 so it doesn't effect the loop if it is not set by the user
        int maxNumberOfTagSets = -1;
        if (number.isPresent()) {
            maxNumberOfTagSets = number.get();
        }
        // In case we need to filter
        if (currentInput.isPresent()) {
            // In case we need to filter just set the tagnames that match the pattern
            String pattern = currentInput.get();
            for (Tag t : tagList) {
                if (maxNumberOfTagSets == 0) {
                    break;
                }
                else if (((t.getName()).toLowerCase()).startsWith(pattern.toLowerCase())) {
                    tagNames.add(t.getName());
                    maxNumberOfTagSets--;
                }

            }
        }

        else {
            // Normal case with no filtering, just return the number that is specified
            for (Tag t : tagList) {
                if (maxNumberOfTagSets == 0) {
                    break;
                }
                tagNames.add(t.getName());
                maxNumberOfTagSets--;
            }
        }
        tsjo.setTagNames(tagNames);

        return new ResponseEntity<TagSetJSONObject>(tsjo, HttpStatus.OK);

    }

    /**
     * Method that create an AnnotationJSONObject from a given annotation
     * 
     * @param a
     *            the annotation to make an AnnotationJSONObject
     * @param project
     *            the project that contains the annotation
     * @return AnnotationJSONObject for the given annotation
     */
    private AnnotationJSONObject getJSONObjFromAnnotation(Annotation a, Project project)
    {
        // Create and fill the object that will be used to represent the annotation in json
        AnnotationJSONObject ajo = new AnnotationJSONObject();

        // Set attributes token/annotation
        ajo.setBegin(a.getBegin());
        ajo.setEnd(a.getEnd());
        ajo.setAnnotationId(a.getAddress());
        List<Token> tokenls = JCasUtil.selectCovered(Token.class, a);
        ArrayList<Long> tjoList = new ArrayList<>();
        for (Token t : tokenls) {
            tjoList.add((long) t.getAddress());
        }
        ajo.setCoveredTokens(tjoList);
        ArrayList<FeatureRef<Object>> features;
        features = getAnnotationFeatures(a, project);

        ajo.setLayerId(annotationService.getLayer(project, a).getId());
        ajo.setFeatures(features);
        return ajo;

    }

    /**
     * Method that returns a list of FeatureRefs that are used in AnnotationJSONObjects to reference
     * a feature
     * 
     * @param annotation
     *            the annotation with the features
     * @param project
     *            the project that contains the annotation
     * @return List of FeatureReferences (FeatureRefs)
     */
    private ArrayList<FeatureRef<Object>> getAnnotationFeatures(Annotation annotation,
            Project project)
    {

        ArrayList<FeatureRef<Object>> returnList = new ArrayList<>();
        AnnotationLayer layer = annotationService.getLayer(project, annotation);

        TypeAdapter aAdapter = annotationService.getAdapter(layer);

        ArrayList<Feature> feats = (ArrayList<Feature>) annotation.getType().getFeatures();

        for (Feature aFeat : feats) {

            FeatureRef<Object> featureRef = new FeatureRef<>();
            boolean exists = annotationService.existsFeature(aFeat.getShortName(), layer);
            if (exists) {
                AnnotationFeature aFeature = annotationService.getFeature(aFeat.getShortName(),
                        layer);
                long featId = aFeature.getId();
                featureRef.setId(featId);

                // Handle the value
                Object valueObj = aAdapter.getFeatureValue(aFeature, annotation);

                // The getFeatureValue method returns either an ArrayList if the Feature is
                // a multi-value-Feature or a Simple Type (String,Integer,Float,Boolean) if it is
                // a single-value-Feature

                if (MultiValueMode.ARRAY.equals(aFeature.getMultiValueMode())) {
                    // handle ArrayList
                    ArrayList<HashMap<String, String>> infoList = new ArrayList<>();
                    for (LinkWithRoleModel link : (ArrayList<LinkWithRoleModel>) valueObj) {
                        HashMap<String, String> r_l_info = new HashMap<>();
                        r_l_info.put("role", link.role);
                        r_l_info.put("label", link.label);
                        r_l_info.put("targetId", Integer.toString((link.targetAddr)));

                        infoList.add(r_l_info);
                    }
                    featureRef.setValue(infoList);
                    featureRef.setMulti(true);
                }
                else {
                    // Maybe we can make a difference between String, Integer, etc..
                    if (valueObj != null) {
                        featureRef.setMulti(false);
                        featureRef.setValue(valueObj.toString());

                    }
                }
                returnList.add(featureRef);
            }

        }
        return returnList;
    }

    /**
     * Method that creates an AnnotationLayerJSONObject from a given AnnoationLayer
     * 
     * @param aLayer
     *            the AnnotationLayer to make an AnnotationLayerJSONObject
     * @return the AnnoationLayerJSONObject for the given layer
     */
    public AnnotationLayerJSONObject getAljoOfAnnotationLayer(AnnotationLayer aLayer)
    {
        AnnotationLayer layer = aLayer;
        AnnotationLayerJSONObject aljo = new AnnotationLayerJSONObject();
        // Set values for aljo
        aljo.setLayerId(layer.getId());
        aljo.setUiName(layer.getUiName());
        aljo.setName(layer.getName());
        aljo.setType(layer.getType());
        aljo.setDescription(layer.getDescription());
        aljo.setReadonly(layer.isReadonly());
        aljo.setCrossSentence(layer.isCrossSentence());
        aljo.setAllowStacking(layer.isAllowStacking());
        aljo.setMultipleTokens(AnchoringMode.TOKENS.equals(layer.getAnchoringMode()));
        aljo.setPartialTokenCovering(AnchoringMode.CHARACTERS.equals(layer.getAnchoringMode()));

        ArrayList<FeatureInfo> featureInfos = new ArrayList<>();
        ArrayList<AnnotationFeature> featls = (ArrayList<AnnotationFeature>) annotationService
                .listAnnotationFeature(layer);
        for (AnnotationFeature feat : featls) {
            FeatureInfo featureInfo = new FeatureInfo();
            featureInfo.setId(feat.getId());
            featureInfo.setUiName(feat.getUiName());
            featureInfo.setName(feat.getName());
            featureInfo.setType(feat.getType());
            featureInfo.setRequired(feat.isRequired());
            // Check if multi or single value
            if (MultiValueMode.ARRAY.equals((feat.getMultiValueMode()))) {
                featureInfo.setMulti(true);
            }
            else {
                featureInfo.setMulti(false);
            }
            featureInfo.setDescription(feat.getDescription());
            // check if there is a tagset to avoid NullPointerException
            if (feat.getTagset() != null) {
                featureInfo.setTagSetId(feat.getTagset().getId());
            }
            featureInfos.add(featureInfo);
        }
        aljo.setFeatures(featureInfos);
        return aljo;
    }

    /**
     * Method that distinguishes between the different types (span,Chain,Arc) and creates an
     * annotation in the cas
     * 
     * @param aAdapter
     *            adapter to create the annotation
     * @param aJCas
     *            JCas where the annotation is created
     * @param layer
     *            layer of the annotation
     * @param begin
     *            begin of the annotation
     * @param end
     *            end of the annotation
     * @return id of the new annotation
     * @throws AnnotationException
     *             throw an exception if the a span annotation is not in the same sentence
     * @throws IOException
     *             Signals that an I/O exception of some sort has occurred.
     * @throws ObjectNotFoundException 
     */
    private int createNewAnnotation(TypeAdapter aAdapter, SourceDocument aDoc, JCas aJCas,
            AnnotationLayer layer, int begin, int end)
        throws AnnotationException, IOException, ObjectNotFoundException
    {
        if ("relation".equals(layer.getType())) {
            if (aAdapter instanceof SpanAdapter) {
                LOG.error("ERROR: Layer [" + aAdapter.getLayer().getUiName()
                        + "] does not support arc annotation.");
                return -1;
            }
            else if (aAdapter instanceof ArcAdapter) {
                return createNewRelationAnnotation((ArcAdapter) aAdapter, aJCas, begin, end);
            }
            else if (aAdapter instanceof ChainAdapter) {
                return createNewChainLinkAnnotation((ChainAdapter) aAdapter, aJCas, begin, end);
            }
            else {
                throw new IllegalStateException("I don't know how to use ["
                        + aAdapter.getClass().getSimpleName() + "] in this situation.");
            }
        }
        else {
            if (aAdapter instanceof SpanAdapter) {
                return createNewSpanAnnotation(aDoc, (SpanAdapter) aAdapter, aJCas, begin, end);
            }
            else if (aAdapter instanceof ChainAdapter) {
                return ((ChainAdapter) aAdapter).addSpan(aJCas, begin, end);
            }
            else {
                throw new IllegalStateException("I don't know how to use ["
                        + aAdapter.getClass().getSimpleName() + "] in this situation.");
            }
        }
    }

    /**
     * Method to create a new relation annotation
     * 
     * @param aAdapter
     *            adapter to create the annotation
     * @param aJCas
     *            JCas where the annotation is created
     * @param begin
     *            begin of the annotation
     * @param end
     *            end of the annotation
     * @return id of the new annotation
     * @throws AnnotationException
     */
    private int createNewRelationAnnotation(ArcAdapter aAdapter, JCas aJCas, int begin, int end)
        throws AnnotationException
    {
        AnnotationFS originFs = WebAnnoCasUtil.selectByAddr(aJCas, begin);
        AnnotationFS targetFs = WebAnnoCasUtil.selectByAddr(aJCas, end);
        int endIndex = aJCas.getDocumentText().length();
        // Creating a relation
        AnnotationFS newAnnotationFS = aAdapter.add(originFs, targetFs, aJCas, 0, endIndex - 1);
        int newId = WebAnnoCasUtil.getAddr(newAnnotationFS);
        return newId;
    }

    /**
     * Method to create a new span annotation
     * @param aDoc 
     * 
     * @param aAdapter
     *            adapter to create the annotation
     * @param aJCas
     *            JCas where the annotation is created
     * @param begin
     *            begin of the annotation
     * @param end
     *            end of the annotation
     * @return id of the new annotation
     * @throws AnnotationException
     * @throws ObjectNotFoundException 
     */
    private int createNewSpanAnnotation(SourceDocument aDoc, SpanAdapter aAdapter, JCas aJCas,
            int begin, int end)
        throws IOException, AnnotationException, ObjectNotFoundException
    {
        LOG.info("createNewSpanAnnotation()");

        if (!aAdapter.getLayer().getAnchoringMode().isZeroSpanAllowed() && begin == end) {
            throw new AnnotationException(
                    "Cannot create zero-width annotation on layers that lock to token boundaries.");
        }

        // Create a dummy annotator state
        AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setDocument(aDoc, asList(aDoc));
        state.setUser(getCurrentUser());
        
        return aAdapter.add(state, aJCas, begin, end);
    }

    /**
     * Method to create a new chain link annotation.
     * 
     * @param aAdapter
     *            adapter to create the annotation.
     * @param aJCas
     *            JCas where the annotation is created.
     * @param begin
     *            begin of the annotation.
     * @param end
     *            end of the annotation.
     * @return id of the new annotation.
     * @throws AnnotationException
     */
    private int createNewChainLinkAnnotation(ChainAdapter aAdapter, JCas aJCas, int begin, int end)
    {
        LOG.info("createNewChainLinkAnnotation()");

        AnnotationFS originFs = WebAnnoCasUtil.selectByAddr(aJCas, begin);
        AnnotationFS targetFs = WebAnnoCasUtil.selectByAddr(aJCas, end);

        // Creating a new chain link
        return aAdapter.addArc(aJCas, originFs, targetFs);
    }

    /**
     * Method that returns all attached relation annotations.
     * 
     * @param aFs
     *            annotation the output annotations are attached to.
     * @param aLayer
     *            layer of the annotation.
     * @return Set of attached relation annotations.
     */
    public Set<AnnotationFS> getAttachedRels(AnnotationFS aFs, AnnotationLayer aLayer)
    {
        CAS cas = aFs.getCAS();
        Set<AnnotationFS> toBeDeleted = new HashSet<>();
        for (AnnotationLayer relationLayer : annotationService.listAttachedRelationLayers(aLayer)) {
            ArcAdapter relationAdapter = (ArcAdapter) annotationService.getAdapter(relationLayer);
            Type relationType = CasUtil.getType(cas, relationLayer.getName());
            Feature sourceFeature = relationType
                    .getFeatureByBaseName(relationAdapter.getSourceFeatureName());
            Feature targetFeature = relationType
                    .getFeatureByBaseName(relationAdapter.getTargetFeatureName());

            // This code is already prepared for the day that relations can go between
            // different layers and may have different attach features for the source and
            // target layers.
            Feature relationSourceAttachFeature = null;
            Feature relationTargetAttachFeature = null;
            if (relationAdapter.getAttachFeatureName() != null) {
                relationSourceAttachFeature = sourceFeature.getRange()
                        .getFeatureByBaseName(relationAdapter.getAttachFeatureName());
                relationTargetAttachFeature = targetFeature.getRange()
                        .getFeatureByBaseName(relationAdapter.getAttachFeatureName());
            }

            for (AnnotationFS relationFS : CasUtil.select(cas, relationType)) {
                // Here we get the annotations that the relation is pointing to in the UI
                FeatureStructure sourceFS;
                if (relationSourceAttachFeature != null) {
                    sourceFS = relationFS.getFeatureValue(sourceFeature)
                            .getFeatureValue(relationSourceAttachFeature);
                }
                else {
                    sourceFS = relationFS.getFeatureValue(sourceFeature);
                }

                FeatureStructure targetFS;
                if (relationTargetAttachFeature != null) {
                    targetFS = relationFS.getFeatureValue(targetFeature)
                            .getFeatureValue(relationTargetAttachFeature);
                }
                else {
                    targetFS = relationFS.getFeatureValue(targetFeature);
                }

                if (isSame(sourceFS, aFs) || isSame(targetFS, aFs)) {
                    toBeDeleted.add(relationFS);
                    LOG.info("Deleted relation [" + getAddr(relationFS) + "] from layer ["
                            + relationLayer.getName() + "]");
                }
            }
        }

        return toBeDeleted;
    }

    /**
     * Method that returns all attached span annotations.
     * 
     * @param aFs
     *            annotation the output annotations are attached to.
     * @param aLayer
     *            layer of the annotation.
     * @return Set of attached span annotations.
     */
    private Set<AnnotationFS> getAttachedSpans(AnnotationFS aFs, AnnotationLayer aLayer)
    {
        CAS cas = aFs.getCAS();
        Set<AnnotationFS> attachedSpans = new HashSet<>();
        TypeAdapter adapter = annotationService.getAdapter(aLayer);
        if (adapter instanceof SpanAdapter && aLayer.getAttachType() != null) {
            Type spanType = CasUtil.getType(cas, aLayer.getAttachType().getName());
            Feature attachFeature = spanType
                    .getFeatureByBaseName(aLayer.getAttachFeature().getName());

            for (AnnotationFS attachedFs : selectAt(cas, spanType, aFs.getBegin(), aFs.getEnd())) {
                if (isSame(attachedFs.getFeatureValue(attachFeature), aFs)) {
                    attachedSpans.add(attachedFs);
                }
            }
        }
        return attachedSpans;
    }

    @ExceptionHandler(value = RemoteApiException.class)
    public ResponseEntity<RResponse<Void>> handleException(RemoteApiException aException)
        throws IOException
    {
        LOG.error(aException.getMessage(), aException);
        return ResponseEntity.status(aException.getStatus()).contentType(APPLICATION_JSON_UTF8)
                .body(new RResponse<>(ERROR, aException.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<RResponse<Void>> handleException(Exception aException) throws IOException
    {
        LOG.error(aException.getMessage(), aException);
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).contentType(APPLICATION_JSON_UTF8)
                .body(new RResponse<>(ERROR, "Internal server error: " + aException.getMessage()));
    }

    /**
     * Get the currently logged in user
     * 
     * @return the user
     * @throws ObjectNotFoundException
     *             if the user does not exist in the repository
     */
    private User getCurrentUser() throws ObjectNotFoundException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return getUser(username);
    }

    /**
     * Get a user by id
     * 
     * @param aUserId
     *            id of the user
     * @return the user
     * @throws ObjectNotFoundException
     *             if the user does not exist in the repository
     */
    private User getUser(String aUserId) throws ObjectNotFoundException
    {
        User user = userRepository.get(aUserId);
        if (user == null) {
            throw new ObjectNotFoundException("User [" + aUserId + "] not found.");
        }
        return user;
    }

    /**
     * Get a project by id
     * 
     * @param aProjectId
     *            id of the project
     * @return the project
     * @throws ObjectNotFoundException
     *             if the project does not exist in the repository
     * @throws AccessForbiddenException
     *             if the currently logged in user has not the necessary permissions
     */
    private Project getProject(long aProjectId)
        throws ObjectNotFoundException, AccessForbiddenException
    {
        // Get current user - this will throw an exception if the current user does not exit
        User user = getCurrentUser();

        // Get project
        Project project;
        try {
            project = projectService.getProject(aProjectId);
        }
        catch (NoResultException e) {
            throw new ObjectNotFoundException("Project [" + aProjectId + "] not found.");
        }

        // Check for the access
        assertPermission(
                "User [" + user.getUsername() + "] is not allowed to access project [" + aProjectId
                        + "]",
                isProjectAdmin(project, projectService, user)
                        || isSuperAdmin(projectService, user));

        return project;
    }

    /**
     * Get a SourceDocument by id
     * 
     * @param aProject
     *            the project that contains the document
     * @param aDocumentId
     *            id of the document
     * @return the SourceDocument
     * @throws ObjectNotFoundException
     *             if the document does not exist in the repository
     */
    private SourceDocument getDocument(Project aProject, long aDocumentId)
        throws ObjectNotFoundException
    {
        try {
            return documentService.getSourceDocument(aProject.getId(), aDocumentId);
        }
        catch (NoResultException e) {
            throw new ObjectNotFoundException("Document [" + aDocumentId + "] in project ["
                    + aProject.getId() + "] not found.");
        }
    }

    /**
     * Get an AnnotationDocument by id
     * 
     * @param aDocument
     *            the SourceDocument of the AnnotationDocument
     * @param aUser
     *            the user
     * @param aCreateIfMissing
     *            whether the AnnotationDocument should be created if missing
     * @return the AnnotationDocument
     * @throws ObjectNotFoundException
     *             if no annotation document exists for the given source/user.
     */
    private AnnotationDocument getAnnotation(SourceDocument aDocument, User aUser,
            boolean aCreateIfMissing)
        throws ObjectNotFoundException
    {
        try {
            if (aCreateIfMissing) {
                return documentService.createOrGetAnnotationDocument(aDocument, aUser);
            }
            else {
                return documentService.getAnnotationDocument(aDocument, aUser);
            }
        }
        catch (NoResultException e) {
            throw new ObjectNotFoundException(
                    "Annotation for user [" + aUser + "] on document [" + aDocument.getId()
                            + "] in project [" + aDocument.getProject().getId() + "] not found.");
        }
    }

    private void assertPermission(String aMessage, boolean aHasAccess)
        throws AccessForbiddenException
    {
        if (!aHasAccess) {
            throw new AccessForbiddenException(aMessage);
        }
    }

    /**
     * API function that returns information about all projects accessible by the authenticated user
     * 
     * @return ResponseEntity that contains http status code and lists information about the
     *         projects
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @ApiOperation(value = "List the projects accessible by the authenticated user")
    @RequestMapping(value = ("/"
            + PROJECTS), method = RequestMethod.GET, produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ProjectJSONObject<List<RProject>>> projectList() throws Exception
    {
        // Get current user - this will throw an exception if the current user does not exit
        User user = getCurrentUser();

        // Get projects with permission
        List<Project> accessibleProjects = projectService.listAccessibleProjects(user);

        // Collect all the projects
        List<RProject> projectList = new ArrayList<>();
        for (Project project : accessibleProjects) {
            projectList.add(new RProject(project));
        }
        return ResponseEntity.ok(new ProjectJSONObject<>(projectList));
    }

    /**
     * API function that returns information about a specific project
     * 
     * @param aProjectId
     *            id to specify a project
     * @return ResponseEntity that contains http status code and information about a specific
     *         project
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @ApiOperation(value = "Get information about a project")
    @RequestMapping(value = ("/" + PROJECTS + "/{" + PARAM_PROJECT_ID
            + "}"), method = RequestMethod.GET, produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Project> projectRead(@PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        return new ResponseEntity<Project>(project, HttpStatus.OK);
    }

    /**
     * API function that returns information about all documents in a given project
     * 
     * @param aProjectId
     *            id to specify a project
     * @return ResponseEntity that contains http status code and information about the documents in
     *         the specified project
     * @throws Exception
     *             any errors caused inside of the function.
     */
    @ApiOperation(value = "List documents in a project")
    @RequestMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/"
            + DOCUMENTS, method = RequestMethod.GET, produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ProjectJSONObject<List<? extends JSONOutput>>> documentList(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        List<SourceDocument> documents = documentService.listSourceDocuments(project);

        List<DocumentJSONObject> documentList = new ArrayList<>();
        for (SourceDocument document : documents) {
            AnnotationDocument annoDoc = getAnnotation(document, getCurrentUser(), true);
            // JCas jcas = documentService.readAnnotationCas(annoDoc, 'r');
            JCas jcas = documentService.readAnnotationCas(annoDoc);

            documentList.add(new DocumentJSONObject(document, jcas));
        }

        return ResponseEntity.ok(new ProjectJSONObject<>(documentList));
    }

    public static SourceDocumentState parseSourceDocumentState(String aState)
    {
        switch (aState) {
        case "NEW":
            return SourceDocumentState.NEW;
        case "ANNOTATION-IN-PROGRESS":
            return SourceDocumentState.ANNOTATION_IN_PROGRESS;
        case "CURATION-COMPLETE":
            return SourceDocumentState.CURATION_FINISHED;
        case "CURATION-IN-PROGRESS":
            return SourceDocumentState.CURATION_IN_PROGRESS;
        default:
            throw new IllegalArgumentException("Unknown source document state [" + aState + "]");
        }
    }

    public static String sourceDocumentStateToString(SourceDocumentState aState)
    {
        switch (aState) {
        case NEW:
            return "NEW";
        case ANNOTATION_IN_PROGRESS:
            return "ANNOTATION-IN-PROGRESS";
        case CURATION_FINISHED:
            return "CURATION-COMPLETE";
        case CURATION_IN_PROGRESS:
            return "CURATION-IN-PROGRESS";
        default:
            throw new IllegalArgumentException("Unknown source document state [" + aState + "]");
        }
    }

    public static AnnotationDocumentState parseAnnotationDocumentState(String aState)
    {
        switch (aState) {
        case "NEW":
            return AnnotationDocumentState.NEW;
        case "COMPLETE":
            return AnnotationDocumentState.FINISHED;
        case "LOCKED":
            return AnnotationDocumentState.IGNORE;
        case "IN-PROGRESS":
            return AnnotationDocumentState.IN_PROGRESS;
        default:
            throw new IllegalArgumentException(
                    "Unknown annotation document state [" + aState + "]");
        }
    }

    public static String annotationDocumentStateToString(AnnotationDocumentState aState)
    {
        switch (aState) {
        case NEW:
            return "NEW";
        case FINISHED:
            return "COMPLETE";
        case IGNORE:
            return "LOCKED";
        case IN_PROGRESS:
            return "IN-PROGRESS";
        default:
            throw new IllegalArgumentException(
                    "Unknown annotation document state [" + aState + "]");
        }
    }
}
