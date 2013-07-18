/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation;

import java.io.IOException;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.request.IRequestParameters;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationUserSegmentForAnnotationDocument;

/** A utility class for the curation AND Correction modules
 * @author Seid Muhie Yimam
 *
 */
public class BratCuratorUtility
{
    public final static String CURATION_USER = "CURATION_USER";
    public static void mergeSpan(IRequestParameters aRequest,
            CurationUserSegmentForAnnotationDocument aCurationUserSegment, JCas aJcas,
            RepositoryService repository, AnnotationService annotationService)
    {
        // add span for merge
        // get information of the span clicked\
        String username = aCurationUserSegment.getUsername();
        Project project = aCurationUserSegment.getBratAnnotatorModel().getProject();
        SourceDocument sourceDocument = aCurationUserSegment.getBratAnnotatorModel().getDocument();
        Integer address = aRequest.getParameterValue("id").toInteger();
        AnnotationSelection annotationSelection = aCurationUserSegment
                .getAnnotationSelectionByUsernameAndAddress().get(username).get(address);
        if (annotationSelection != null) {
            AnnotationDocument clickedAnnotationDocument = null;
            List<AnnotationDocument> annotationDocuments = repository.listAnnotationDocument(
                    project, sourceDocument);
            for (AnnotationDocument annotationDocument : annotationDocuments) {
                if (annotationDocument.getUser().equals(username)) {
                    clickedAnnotationDocument = annotationDocument;
                    break;
                }
            }
            try {
                createSpan(aRequest, aCurationUserSegment.getBratAnnotatorModel(), aJcas,
                        clickedAnnotationDocument, address, repository, annotationService);
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

    public static void mergeArc(IRequestParameters aRequest,
            CurationUserSegmentForAnnotationDocument aCurationUserSegment, JCas aJcas,
            RepositoryService repository, AnnotationService annotationService)
    {
        // add span for merge
        // get information of the span clicked
        String username = aCurationUserSegment.getUsername();
        Project project = aCurationUserSegment.getBratAnnotatorModel().getProject();
        SourceDocument sourceDocument = aCurationUserSegment.getBratAnnotatorModel().getDocument();

        Integer addressOriginClicked = aRequest.getParameterValue("originSpanId").toInteger();
        Integer addressTargetClicked = aRequest.getParameterValue("targetSpanId").toInteger();
        String arcType = aRequest.getParameterValue("type").toString();
        AnnotationSelection annotationSelectionOrigin = aCurationUserSegment
                .getAnnotationSelectionByUsernameAndAddress().get(username)
                .get(addressOriginClicked);
        AnnotationSelection annotationSelectionTarget = aCurationUserSegment
                .getAnnotationSelectionByUsernameAndAddress().get(username)
                .get(addressTargetClicked);
        Integer addressOrigin = annotationSelectionOrigin.getAddressByUsername().get(CURATION_USER);
        Integer addressTarget = annotationSelectionTarget.getAddressByUsername().get(CURATION_USER);

        if (annotationSelectionOrigin != null && annotationSelectionTarget != null) {

            // TODO no coloring is done at all for arc annotation.
            // Do the same for arc colors (AGREE, USE,...
            AnnotationDocument clickedAnnotationDocument = null;
            List<AnnotationDocument> annotationDocuments = repository.listAnnotationDocument(
                    project, sourceDocument);
            for (AnnotationDocument annotationDocument : annotationDocuments) {
                if (annotationDocument.getUser().equals(username)) {
                    clickedAnnotationDocument = annotationDocument;
                    break;
                }
            }
            JCas clickedJCas = null;
            try {
                clickedJCas = repository.getAnnotationDocumentContent(clickedAnnotationDocument);
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
            AnnotationFS fsClicked = (AnnotationFS) clickedJCas.getLowLevelCas().ll_getFSForRef(
                    addressOriginClicked);
            arcType = BratAjaxCasUtil.getAnnotationType(fsClicked.getType()) + arcType;
            BratAjaxCasController controller = new BratAjaxCasController(repository,
                    annotationService);
            try {
                controller.addArcToCas(aCurationUserSegment.getBratAnnotatorModel(), arcType, 0, 0,
                        addressOrigin, addressTarget, aJcas);
                controller.createAnnotationDocumentContent(aCurationUserSegment
                        .getBratAnnotatorModel().getMode(), aCurationUserSegment
                        .getBratAnnotatorModel().getDocument(), aCurationUserSegment
                        .getBratAnnotatorModel().getUser(), aJcas);
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void createSpan(IRequestParameters aRequest,
            BratAnnotatorModel aBratAnnotatorModel, JCas aMergeJCas,
            AnnotationDocument aAnnotationDocument, int aAddress, RepositoryService repository,
            AnnotationService annotationService)
        throws IOException, UIMAException, ClassNotFoundException
    {

        String spanType = aRequest.getParameterValue("type").toString()
                .replace("_(" + AnnotationState.AGREE.name() + ")", "")
                .replace("_(" + AnnotationState.USE.name() + ")", "")
                .replace("_(" + AnnotationState.DISAGREE.name() + ")", "")
                .replace("_(" + AnnotationState.DO_NOT_USE.name() + ")", "")
                .replace("_(" + AnnotationState.NOT_SUPPORTED.name() + ")", "");

        JCas clickedJCas = repository.getAnnotationDocumentContent(aAnnotationDocument);
        AnnotationFS fsClicked = (AnnotationFS) clickedJCas.getLowLevelCas().ll_getFSForRef(
                aAddress);
        // TODO temporarily solution to remove the the prefix from curation sentence annotation
        // views
        spanType = BratAjaxCasUtil.getAnnotationType(fsClicked.getType()) + spanType;

        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);

        controller.addSpanToCas(aMergeJCas, fsClicked.getBegin(), fsClicked.getEnd(), spanType, 0,
                0);
        controller.createAnnotationDocumentContent(aBratAnnotatorModel.getMode(),
                aBratAnnotatorModel.getDocument(), aBratAnnotatorModel.getUser(), aMergeJCas);
    }
}
