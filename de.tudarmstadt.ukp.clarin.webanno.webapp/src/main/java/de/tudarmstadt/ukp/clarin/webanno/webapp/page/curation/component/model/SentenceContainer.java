/**
 *
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.CurationPanel;
/**
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public class SentenceContainer
    extends WebMarkupContainer
{
    private static final long serialVersionUID = 8736268179612831795L;
    private ListView<CurationUserSegmentForAnnotationDocument> sentenceListView;
    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    public final static String CURATION_USER = "CURATION_USER";

    /**
     * Data models for {@link BratAnnotator}
     */
    public void setModel(IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(LinkedList<CurationUserSegmentForAnnotationDocument> aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> getModel()
    {
        return (IModel<LinkedList<CurationUserSegmentForAnnotationDocument>>) getDefaultModel();
    }

    public LinkedList<CurationUserSegmentForAnnotationDocument> getModelObject()
    {
        return (LinkedList<CurationUserSegmentForAnnotationDocument>) getDefaultModelObject();
    }
    public SentenceContainer(String id,
            IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> aModel)
    {
        super(id, aModel);
        // update list of brat embeddings
        sentenceListView = new ListView<CurationUserSegmentForAnnotationDocument>(
                "sentenceListView", aModel)
        {
            private static final long serialVersionUID = -5389636445364196097L;

            @Override
            protected void populateItem(ListItem<CurationUserSegmentForAnnotationDocument> item2)
            {
                final CurationUserSegmentForAnnotationDocument curationUserSegment = item2
                        .getModelObject();
                BratCurationVisualizer curationVisualizer = new BratCurationVisualizer("sentence",
                        new Model<CurationUserSegmentForAnnotationDocument>(curationUserSegment))
                {
                    private static final long serialVersionUID = -1205541428144070566L;

                    /**
                     * Method is called, if user has clicked on a span or an arc in the sentence
                     * panel. The span or arc respectively is identified and copied to the merge
                     * cas.
                     */
                    @Override
                    protected void onSelectAnnotationForMerge(AjaxRequestTarget aTarget)
                    {
                        final IRequestParameters request = getRequest().getPostParameters();

                        SourceDocument sourceDocument = curationUserSegment.getBratAnnotatorModel()
                                .getDocument();
                        Project project = curationUserSegment.getBratAnnotatorModel().getProject();
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
                            annotationSelection = curationUserSegment
                                    .getAnnotationSelectionByUsernameAndAddress().get(username)
                                    .get(address);
                            if (annotationSelection != null) {
                                AnnotationDocument clickedAnnotationDocument = null;
                                List<AnnotationDocument> annotationDocuments = repository
                                        .listAnnotationDocument(project, sourceDocument);
                                for (AnnotationDocument annotationDocument : annotationDocuments) {
                                    if (annotationDocument.getUser().equals(username)) {
                                        clickedAnnotationDocument = annotationDocument;
                                        break;
                                    }
                                }
                                try {
                                    CurationPanel.createSpan(request,
                                            curationUserSegment.getBratAnnotatorModel(), mergeJCas,
                                            clickedAnnotationDocument, address, repository,
                                            annotationService);
                                }
                                catch (UIMAException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                catch (ClassNotFoundException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }

                        }
                        // check if clicked on an arc
                        else if (!action.isEmpty() && action.toString().equals("selectArcForMerge")) {
                            // add span for merge
                            // get information of the span clicked
                            Integer addressOriginClicked = request
                                    .getParameterValue("originSpanId").toInteger();
                            Integer addressTargetClicked = request
                                    .getParameterValue("targetSpanId").toInteger();
                            String arcType = request.getParameterValue("type").toString();
                            AnnotationSelection annotationSelectionOrigin = curationUserSegment
                                    .getAnnotationSelectionByUsernameAndAddress().get(username)
                                    .get(addressOriginClicked);
                            AnnotationSelection annotationSelectionTarget = curationUserSegment
                                    .getAnnotationSelectionByUsernameAndAddress().get(username)
                                    .get(addressTargetClicked);
                            Integer addressOrigin = annotationSelectionOrigin
                                    .getAddressByUsername().get(CURATION_USER);
                            Integer addressTarget = annotationSelectionTarget
                                    .getAddressByUsername().get(CURATION_USER);

                            if (annotationSelectionOrigin != null
                                    && annotationSelectionTarget != null) {

                                // TODO no coloring is done at all for arc annotation.
                                // Do the same for arc colors (AGREE, USE,...
                                AnnotationDocument clickedAnnotationDocument = null;
                                List<AnnotationDocument> annotationDocuments = repository
                                        .listAnnotationDocument(project, sourceDocument);
                                for (AnnotationDocument annotationDocument : annotationDocuments) {
                                    if (annotationDocument.getUser().equals(username)) {
                                        clickedAnnotationDocument = annotationDocument;
                                        break;
                                    }
                                }
                                JCas clickedJCas = null;
                                try {
                                    clickedJCas = repository
                                            .getAnnotationDocumentContent(clickedAnnotationDocument);
                                }
                                catch (UIMAException e1) {
                                    // TODO Auto-generated catch block
                                    e1.printStackTrace();
                                }
                                catch (ClassNotFoundException e1) {
                                    // TODO Auto-generated catch block
                                    e1.printStackTrace();
                                }
                                catch (IOException e1) {
                                    // TODO Auto-generated catch block
                                    e1.printStackTrace();
                                }
                                AnnotationFS fsClicked = (AnnotationFS) clickedJCas
                                        .getLowLevelCas().ll_getFSForRef(addressOriginClicked);
                                arcType = BratAjaxCasUtil.getAnnotationType(fsClicked.getType())
                                        + arcType;
                                BratAjaxCasController controller = new BratAjaxCasController(
                                        repository, annotationService);
                                try {
                                    controller.addArcToCas(
                                            curationUserSegment.getBratAnnotatorModel(), arcType,
                                            0, 0, addressOrigin, addressTarget, mergeJCas);
                                    controller.createAnnotationDocumentContent(curationUserSegment
                                            .getBratAnnotatorModel().getMode(), curationUserSegment
                                            .getBratAnnotatorModel().getDocument(),
                                            curationUserSegment.getBratAnnotatorModel().getUser(),
                                            mergeJCas);
                                }
                                catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                        onChange(aTarget);
                    }
                };
                curationVisualizer.setOutputMarkupId(true);
                item2.add(curationVisualizer);
            }
        };
        sentenceListView.setOutputMarkupId(true);
        add(sentenceListView);
    }

    protected void onChange(AjaxRequestTarget aTarget)
    {
        // Overriden in curationPanel
    }
}
