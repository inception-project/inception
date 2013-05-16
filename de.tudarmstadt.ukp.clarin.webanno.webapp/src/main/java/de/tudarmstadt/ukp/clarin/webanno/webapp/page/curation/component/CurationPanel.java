/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCopier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorUIData;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.CasToBratJson;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationOption;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.BratCurationDocumentEditor;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.BratCurationVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationSegmentForSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 *
 * @author Andreas Straninger Main Panel for the curation page. It displays a box with the complete
 *         text on the left side and a box for a selected sentence on the right side.
 *
 */
public class CurationPanel
    extends Panel
{

    private final static Log LOG = LogFactory.getLog(CurationPanel.class);

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    public final static String CURATION_USER = "CURATION_USER";

    /**
     * Map for tracking curated spans. Key contains the address of the span, the value contains the
     * username from which the span has been selected
     */
    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<String, Map<Integer, AnnotationSelection>>();

    private ListView<CurationUserSegmentForAnnotationDocument> sentenceListView;

    private CurationSegmentForSourceDocument curationSegment;

    ListView<CurationSegmentForSourceDocument> textListView;

    /**
     * Class for combining an on click ajax call and a label
     */
    class AjaxLabel
        extends Label
    {

        private static final long serialVersionUID = -4528869530409522295L;
        private AbstractAjaxBehavior click;

        public AjaxLabel(String id, String label, AbstractAjaxBehavior click)
        {
            super(id, label);
            this.click = click;
        }

        @Override
        public void onComponentTag(ComponentTag tag)
        {
            // add onclick handler to the browser
            // if clicked in the browser, the function
            // click.response(AjaxRequestTarget target) is called on the server side
            tag.put("onclick", "wicketAjaxGet('" + click.getCallbackUrl() + "')");
        }

    }

    public CurationPanel(String id, final CurationContainer curationContainer)
    {
        super(id);

        // add container for updating ajax
        final WebMarkupContainer textOuterView = new WebMarkupContainer("textOuterView");
        textOuterView.setOutputMarkupId(true);
        add(textOuterView);

        final WebMarkupContainer sentenceOuterView = new WebMarkupContainer("sentenceOuterView");
        sentenceOuterView.setOutputMarkupId(true);
        add(sentenceOuterView);

        final BratAnnotatorModel bratAnnotatorModel = new BratAnnotatorModel();
        // This is a Curation Operation, add to the data model a CURATION Mode
        bratAnnotatorModel.setMode(Mode.CURATION);

        bratAnnotatorModel.setDocument(curationContainer.getSourceDocument());
        if (curationContainer.getSourceDocument() != null) {
            bratAnnotatorModel.setProject(curationContainer.getSourceDocument().getProject());
            curationSegment = curationContainer.getCurationSegmentByBegin().get(0);
            bratAnnotatorModel.setFirstSentenceAddress(curationSegment.getSentenceAddress().get(
                    CURATION_USER));
            bratAnnotatorModel.setLastSentenceAddress(curationSegment.getSentenceAddress().get(
                    CURATION_USER));
            bratAnnotatorModel.setSentenceAddress(curationSegment.getSentenceAddress().get(
                    CURATION_USER));
            bratAnnotatorModel.setAnnotationLayers(new HashSet<TagSet>(annotationService
                    .listTagSets(bratAnnotatorModel.getProject())));
        }
        User userLoggedIn = repository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());
        bratAnnotatorModel.setUser(userLoggedIn);
        final BratCurationDocumentEditor mergeVisualizer = new BratCurationDocumentEditor(
                "mergeView", new Model<BratAnnotatorModel>(bratAnnotatorModel))
        {

            private static final long serialVersionUID = 7279648231521710155L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                updateRightSide(aTarget, sentenceOuterView, curationContainer, this);
            }
        };
        mergeVisualizer.setOutputMarkupId(true);
        add(mergeVisualizer);

        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();
        // update list of brat embeddings
        sentenceListView = new ListView<CurationUserSegmentForAnnotationDocument>("sentenceListView", sentences)
        {
            @Override
            protected void populateItem(ListItem<CurationUserSegmentForAnnotationDocument> item2)
            {
                final CurationUserSegmentForAnnotationDocument curationUserSegment = item2.getModelObject();
                BratCurationVisualizer curationVisualizer = new BratCurationVisualizer("sentence",
                        new Model<CurationUserSegmentForAnnotationDocument>(curationUserSegment))
                {
                    /**
                     * Method is called, if user has clicked on a span or an arc in the sentence
                     * panel. The span or arc respectively is identified and copied to the merge
                     * cas.
                     */
                    @Override
                    protected void onSelectAnnotationForMerge(AjaxRequestTarget aTarget)
                    {
                        final IRequestParameters request = getRequest().getPostParameters();

                        SourceDocument sourceDocument = curationContainer.getSourceDocument();
                        Project project = curationContainer.getProject();
                        JCas mergeJCas = null;
                        try {
                            mergeJCas = repository.getCurationDocumentContent(sourceDocument);
                        }
                        catch (UIMAException e1) {
                         error(ExceptionUtils.getRootCause(e1));
                        }
                        catch (IOException e1) {
                            error(ExceptionUtils.getRootCause(e1));
                        }
                        catch (ClassNotFoundException e1) {
                            error(ExceptionUtils.getRootCause(e1));
                        }
                        StringValue action = request.getParameterValue("action");
                        AnnotationSelection annotationSelection = null;
                        Integer address = null;
                        String username = curationUserSegment.getUsername();
                        // check if clicked on a span
                        if (!action.isEmpty() && action.toString().equals("selectSpanForMerge")) {
                            // add span for merge
                            // get information of the span clicked
                            address = request.getParameterValue("id").toInteger();
                            annotationSelection = annotationSelectionByUsernameAndAddress.get(
                                    username).get(address);
                        }
                        // check if clicked on an arc
                        if (!action.isEmpty() && action.toString().equals("selectArcForMerge")) {
                            // add span for merge
                            // get information of the span clicked
                            Integer addressOriginClicked = request
                                    .getParameterValue("originSpanId").toInteger();
                            Integer addressTargetClicked = request
                                    .getParameterValue("targetSpanId").toInteger();
                            String arcType = request.getParameterValue("type").toString();
                            String typePrefix = BratAjaxCasUtil.getAnnotationType(arcType);
                            String typeValue = BratAjaxCasUtil.getType(arcType);
                            AnnotationSelection annotationSelectionOrigin = annotationSelectionByUsernameAndAddress
                                    .get(username).get(addressOriginClicked);
                            AnnotationSelection annotationSelectionTarget = annotationSelectionByUsernameAndAddress
                                    .get(username).get(addressTargetClicked);
                            Integer addressOrigin = annotationSelectionOrigin
                                    .getAddressByUsername().get(CURATION_USER);
                            Integer addressTarget = annotationSelectionTarget
                                    .getAddressByUsername().get(CURATION_USER);

                            if (annotationSelectionOrigin != null
                                    && annotationSelectionTarget != null) {
                                BratAnnotatorUIData uIData = new BratAnnotatorUIData();
                                uIData.setjCas(mergeJCas);
                                uIData.setGetDocument(false);
                                uIData.setType(arcType);
                                uIData.setOrigin(addressOrigin);
                                uIData.setTarget(addressTarget);
                                BratAjaxCasController controller = new BratAjaxCasController(
                                        jsonConverter, repository, annotationService);
                                try {
                                    controller.createArcWithoutResponse(bratAnnotatorModel, uIData);
                                }
                                catch (UIMAException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                /*
                                 * if (typePrefix.equals(AnnotationTypeConstant.POS_PREFIX)) {
                                 *
                                 * ArcAdapter.getDependencyAdapter().addToCas(typeValue, uIData,
                                 * bratAnnotatorModel, false);
                                 *
                                 * adapter = ArcAdapter.getDependencyAdapter(); } else if
                                 * (typePrefix.equals(AnnotationTypeConstant.COREFERENCE_PREFIX)) {
                                 * adapter = ChainAdapter.getCoreferenceChainAdapter(); }
                                 * AnnotationFS fsOrigin = (AnnotationFS)
                                 * mergeJCas.getLowLevelCas().ll_getFSForRef(address); AnnotationFS
                                 * fsTarget = (AnnotationFS)
                                 * mergeJCas.getLowLevelCas().ll_getFSForRef(address); Type
                                 * tokenType = CasUtil.getType(mergeJCas.getCas(),
                                 * Token.class.getName()); Feature feature =
                                 * type.getFeatureByBaseName(labelFeatureName);
                                 *
                                 * AnnotationFS newAnnotation =
                                 * mergeJCas.getCas().createAnnotation(type, fsOrigin.getBegin(),
                                 * fsTarget.getEnd()); newAnnotation.setStringValue(feature,
                                 * typeValue); // If origin and target spans are multiple tokens,
                                 * dependentFS.getBegin will be the // the begin position of the
                                 * first token and dependentFS.getEnd will be the End // position of
                                 * the last token. newAnnotation.setFeatureValue( fsOrigin,
                                 * CasUtil.selectCovered(mergeJCas.getCas(), tokenType,
                                 * fsOrigin.getBegin(), fsOrigin.getEnd()).get(0));
                                 * newAnnotation.setFeatureValue( fsTarget,
                                 * CasUtil.selectCovered(mergeJCas.getCas(), tokenType,
                                 * fsTarget.getBegin(), fsTarget.getEnd()).get(0));
                                 * mergeJCas.addFsToIndexes(newAnnotation);
                                 */
                            }
                        }
                        if (annotationSelection != null) {
                            AnnotationDocument clickedAnnotationDocument = null;
                            List<AnnotationDocument> annotationDocuments = repository
                                    .listAnnotationDocument(project, sourceDocument);
                            for (AnnotationDocument annotationDocument : annotationDocuments) {
                                if (annotationDocument.getUser().getUsername().equals(username)) {
                                    clickedAnnotationDocument = annotationDocument;
                                }
                            }

                            if (annotationSelection != null) {
                                // remove old spans and add new span to merge cas
                                try {

                                    // clean up: remove old annotation selections (if present)
                                    for (AnnotationSelection as : annotationSelection
                                            .getAnnotationOption().getAnnotationSelections()) {
                                        Integer addressRemove = as.getAddressByUsername().get(
                                                CURATION_USER);
                                        if (addressRemove != null) {
                                            FeatureStructure fsRemove = mergeJCas.getLowLevelCas()
                                                    .ll_getFSForRef(addressRemove);
                                            mergeJCas.removeFsFromIndexes(fsRemove);
                                        }
                                    }
                                    // copy clicked annotation selection
                                    if (clickedAnnotationDocument != null) {
                                        JCas clickedJCas = repository
                                                .getAnnotationDocumentContent(clickedAnnotationDocument);
                                        CasCopier copier = new CasCopier(clickedJCas.getCas(),
                                                mergeJCas.getCas());
                                        FeatureStructure fsClicked = clickedJCas.getLowLevelCas()
                                                .ll_getFSForRef(address);
                                        FeatureStructure fsCopy = copier.copyFs(fsClicked);
                                        mergeJCas.getCas().addFsToIndexes(fsCopy);
                                    }
                                    // persist jcas
                                    User userLoggedIn = repository.getUser(SecurityContextHolder
                                            .getContext().getAuthentication().getName());
                                    repository.createCurationDocumentContent(mergeJCas,
                                            sourceDocument, userLoggedIn);

                                }
                                catch (UIMAException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                catch (ClassNotFoundException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                            }

                        }

                        updateRightSide(aTarget, sentenceOuterView, curationContainer,
                                mergeVisualizer);
                        // aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                };
                curationVisualizer.setOutputMarkupId(true);
                item2.add(curationVisualizer);
            }
        };
        sentenceListView.setOutputMarkupId(true);

        sentenceOuterView.add(sentenceListView);

        // List view for the complete text on the left side. Each item is a sentence of the text
        textListView = new ListView<CurationSegmentForSourceDocument>("textListView",
                curationContainer.getCurationSegments())
        {
            @Override
            protected void populateItem(ListItem<CurationSegmentForSourceDocument> item)
            {
                final CurationSegmentForSourceDocument curationSegmentItem = item.getModelObject();

                // ajax call when clicking on a sentence on the left side
                final AbstractDefaultAjaxBehavior click = new AbstractDefaultAjaxBehavior()
                {

                    @Override
                    protected void respond(AjaxRequestTarget target)
                    {
                        curationSegment = curationSegmentItem;
                        updateRightSide(target, sentenceOuterView, curationContainer,
                                mergeVisualizer);
                        // target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }

                };

                // add subcomponents to the component
                item.add(click);
                String colorCode = curationSegmentItem.getSentenceState().getColorCode();
                if (colorCode != null) {
                    item.add(new SimpleAttributeModifier("style", "background-color:" + colorCode
                            + ";"));
                }
                Label currentSentence = new AjaxLabel("sentence", curationSegmentItem.getText(),
                        click);
                item.add(currentSentence);

                Label sentenceNumber = new AjaxLabel("sentenceNumber", curationSegmentItem
                        .getSentenceNumber().toString(), click);
                item.add(sentenceNumber);
            }

        };
        // add subcomponents to the component
        textListView.setOutputMarkupId(true);
        textOuterView.add(textListView);

    }

    protected void updateRightSide(AjaxRequestTarget target, MarkupContainer parent,
            CurationContainer curationContainer, BratCurationDocumentEditor mergeVisualizer)
    {
        SourceDocument sourceDocument = curationContainer.getSourceDocument();
        Project project = curationContainer.getProject();
        List<AnnotationDocument> annotationDocuments = repository.listAnnotationDocument(project,
                sourceDocument);
        Map<String, JCas> jCases = new HashMap<String, JCas>();
        JCas mergeJCas = null;
        try {
            mergeJCas = repository.getCurationDocumentContent(sourceDocument);
        }
        catch (UIMAException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        User userLoggedIn = repository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());

        // get cases from repository
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            String username = annotationDocument.getUser().getUsername();
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)
                    || username.equals(CURATION_USER)) {
                try {
                    JCas jCas = repository.getAnnotationDocumentContent(annotationDocument);
                    jCases.put(username, jCas);

                    // cleanup annotationSelections
                    annotationSelectionByUsernameAndAddress.put(username,
                            new HashMap<Integer, AnnotationSelection>());
                }
                catch (Exception e) {
                    LOG.error("Unable to load document [" + annotationDocument + "]", e);
                    error("Unable to load document [" + annotationDocument + "]: "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
        // add mergeJCas separately
        jCases.put(CURATION_USER, mergeJCas);

        // create cas for merge panel
        int numUsers = jCases.size();

        // get differing feature structures
        List<Type> entryTypes = new LinkedList<Type>();
        // entryTypes.add(CasUtil.getType(mergeJCas.getCas(), Token.class));
        // entryTypes.add(CasUtil.getType(mergeJCas.getCas(), Sentence.class));
        entryTypes.add(CasUtil.getType(mergeJCas.getCas(), POS.class));
        entryTypes.add(CasUtil.getType(mergeJCas.getCas(), CoreferenceLink.class));
        entryTypes.add(CasUtil.getType(mergeJCas.getCas(), Lemma.class));
        entryTypes.add(CasUtil.getType(mergeJCas.getCas(), NamedEntity.class));
        entryTypes.add(CasUtil.getType(mergeJCas.getCas(), Dependency.class));
        // TODO arc types
        List<AnnotationOption> annotationOptions = null;
        try {
            annotationOptions = CasDiff.doDiff(entryTypes, jCases, curationSegment.getBegin(),
                    curationSegment.getEnd());
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // fill lookup variable for annotation selections
        for (AnnotationOption annotationOption : annotationOptions) {
            for (AnnotationSelection annotationSelection : annotationOption
                    .getAnnotationSelections()) {
                for (String username : annotationSelection.getAddressByUsername().keySet()) {
                    if (!username.equals(CURATION_USER)) {
                        Integer address = annotationSelection.getAddressByUsername().get(username);
                        annotationSelectionByUsernameAndAddress.get(username).put(address,
                                annotationSelection);
                    }
                }
            }
        }

        List<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();

        BratAnnotatorModel bratAnnotatorModel = mergeVisualizer.getModelObject();
        bratAnnotatorModel.setDocument(sourceDocument);
        bratAnnotatorModel.setProject(sourceDocument.getProject());
        bratAnnotatorModel.setUser(userLoggedIn);
        bratAnnotatorModel.setFirstSentenceAddress(curationSegment.getSentenceAddress().get(
                CURATION_USER));
        bratAnnotatorModel.setLastSentenceAddress(curationSegment.getSentenceAddress().get(
                CURATION_USER));
        bratAnnotatorModel.setSentenceAddress(curationSegment.getSentenceAddress().get(
                CURATION_USER));
        bratAnnotatorModel.setAnnotationLayers(new HashSet<TagSet>(annotationService
                .listTagSets(bratAnnotatorModel.getProject())));

        boolean hasDiff = false;
        List<String> usernamesSorted = new LinkedList<String>(jCases.keySet());
        Collections.sort(usernamesSorted);
        for (String username : usernamesSorted) {
            if (!username.equals(CURATION_USER)) {
                Map<Integer, AnnotationSelection> annotationSelectionByAddress = new HashMap<Integer, AnnotationSelection>();
                for (AnnotationOption annotationOption : annotationOptions) {
                    for (AnnotationSelection annotationSelection : annotationOption
                            .getAnnotationSelections()) {
                        if (annotationSelection.getAddressByUsername().containsKey(username)) {
                            Integer address = annotationSelection.getAddressByUsername().get(
                                    username);
                            annotationSelectionByAddress.put(address, annotationSelection);
                        }
                    }
                }
                JCas jCas = jCases.get(username);

                GetDocumentResponse response = this.getDocumentResponse(jCas, username,
                        bratAnnotatorModel);

                CurationUserSegmentForAnnotationDocument curationUserSegment2 = new CurationUserSegmentForAnnotationDocument();
                curationUserSegment2.setCollectionData(getStringCollectionData(response, jCas,
                        annotationSelectionByAddress, username, numUsers));
                curationUserSegment2.setDocumentResponse(getStringDocumentResponse(response));
                curationUserSegment2.setUsername(username);

                sentences.add(curationUserSegment2);
            }
        }

        // update sentence list on the right side
        sentenceListView.setModelObject(sentences);

        /*
         * CurationUserSegment2 mergeUserSegment = new CurationUserSegment2(); GetDocumentResponse
         * response = getDocumentResponse(mergeJCas, CURATION_USER, bratAnnotatorModel);
         * //mergeUserSegment.setCollectionData(getStringCollectionData(response, mergeJCas,
         * addresses, username)); mergeUserSegment.setCollectionData("{}");
         * mergeUserSegment.setDocumentResponse(getStringDocumentResponse(response));
         */

        mergeVisualizer.reloadContent(target);

        // send response to the client
        parent.addOrReplace(sentenceListView);
        target.add(parent);

    }

    private GetDocumentResponse getDocumentResponse(JCas jCas, String username,
            BratAnnotatorModel aBratAnnotatorModel)
    {
        GetDocumentResponse response = new GetDocumentResponse();
        response.setText(jCas.getDocumentText());

        List<String> tagSetNames = new ArrayList<String>();
        tagSetNames.add(AnnotationTypeConstant.POS);
        tagSetNames.add(AnnotationTypeConstant.DEPENDENCY);
        tagSetNames.add(AnnotationTypeConstant.NAMEDENTITY);
        tagSetNames.add(AnnotationTypeConstant.COREFERENCE);
        tagSetNames.add(AnnotationTypeConstant.COREFRELTYPE);

        CasToBratJson casToBratJson = new CasToBratJson();

        casToBratJson.addTokenToResponse(jCas, response, aBratAnnotatorModel);
        casToBratJson.addSentenceToResponse(jCas, response, aBratAnnotatorModel);
        SpanAdapter.getPosAdapter().addToBrat(jCas, response, aBratAnnotatorModel);
        ChainAdapter.getCoreferenceLinkAdapter().addToBrat(jCas, response, aBratAnnotatorModel);

        SpanAdapter.getLemmaAdapter().addToBrat(jCas, response, aBratAnnotatorModel);
        SpanAdapter.getNamedEntityAdapter().addToBrat(jCas, response, aBratAnnotatorModel);
        // TODO does not work yet
        ArcAdapter.getDependencyAdapter().addToBrat(jCas, response, aBratAnnotatorModel);
        ChainAdapter.getCoreferenceChainAdapter().addToBrat(jCas, response, aBratAnnotatorModel);

        return response;
    }

    private String getStringDocumentResponse(GetDocumentResponse aResponse)
    {
        String docData = "{}";
        // Serialize BRAT object model to JSON
        try {
            StringWriter out = new StringWriter();
            JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                    .createJsonGenerator(out);
            jsonGenerator.writeObject(aResponse);
            docData = out.toString();
        }
        catch (IOException e) {
            error(ExceptionUtils.getRootCauseMessage(e));
        }

        return docData;
    }

    private String getStringCollectionData(GetDocumentResponse response, JCas jCas,
            Map<Integer, AnnotationSelection> annotationSelectionByAddress, String username,
            int numUsers)
    {
        Map<String, Map<String, Object>> entityTypes = new HashMap<String, Map<String, Object>>();

        for (Entity entity : response.getEntities()) {
            // check if either address of entity has no changes ...
            // ... or if entity has already been clicked on
            int address = entity.getId();
            AnnotationSelection annotationSelection = annotationSelectionByAddress.get(address);
            if (annotationSelection == null) {
                String label = entity.getType();
                String type = entity.getType() + "_(NOT_SUPPORTED)";
                entity.setType(type);
                entityTypes.put(type, getEntity(type, label, AnnotationState.NOT_SUPPORTED));
            }
            else if (annotationSelection.getAddressByUsername().size() == numUsers) {
                String label = entity.getType();
                String type = entity.getType() + "_(AGREE)";
                entity.setType(type);
                entityTypes.put(type, getEntity(type, label, AnnotationState.AGREE));
            }
            else if (annotationSelection.getAddressByUsername().containsKey(CURATION_USER)) {
                entityTypes.put(entity.getType(),
                        getEntity(entity.getType(), entity.getType(), AnnotationState.USE));
                String label = entity.getType();
                String type = entity.getType() + "_(USE)";
                entity.setType(type);
                entityTypes.put(type, getEntity(type, label, AnnotationState.USE));
            }
            else {
                boolean doNotUse = false;
                for (AnnotationSelection otherAnnotationSelection : annotationSelection
                        .getAnnotationOption().getAnnotationSelections()) {
                    if (otherAnnotationSelection.getAddressByUsername().containsKey(CURATION_USER)) {
                        doNotUse = true;
                        break;
                    }
                }
                if (doNotUse) {
                    entityTypes.put(
                            entity.getType(),
                            getEntity(entity.getType(), entity.getType(),
                                    AnnotationState.DO_NOT_USE));
                    String label = entity.getType();
                    String type = entity.getType() + "_(DO_NOT_USE)";
                    entity.setType(type);
                    entityTypes.put(type, getEntity(type, label, AnnotationState.DO_NOT_USE));
                }
                else {
                    entityTypes
                            .put(entity.getType(),
                                    getEntity(entity.getType(), entity.getType(),
                                            AnnotationState.DISAGREE));
                }
            }
        }

        Map<Object, Object> collection = new HashMap<Object, Object>();
        collection.put("entity_types", entityTypes.values());

        String collData = "{}";
        try {
            StringWriter out = new StringWriter();
            JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                    .createJsonGenerator(out);
            jsonGenerator.writeObject(collection);
            collData = out.toString();
        }
        catch (IOException e) {
            error(ExceptionUtils.getRootCauseMessage(e));
        }
        return collData;
    }

    private Map<String, Object> getEntity(String type, String label, AnnotationState annotationState)
    {
        Map<String, Object> entityType = new HashMap<String, Object>();
        entityType.put("type", type);
        entityType.put("labels", new String[] { type, label });
        String color = annotationState.getColorCode();
        entityType.put("bgColor", color);
        entityType.put("borderColor", "darken");
        return entityType;
    }

}
