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
package de.tudarmstadt.ukp.clarin.webanno.brat.util;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectAnnotationByAddress;
import static org.uimafit.util.JCasUtil.select;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.wicket.request.IRequestParameters;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.uimafit.factory.JCasFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorUIData;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A helper class for {@link BratAnnotator} and CurationEditor
 *
 * @author Seid Muhie Yimam
 *
 */
public class BratAnnotatorUtility
{

    public static Object getDocument(BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel)
        throws ClassNotFoundException, IOException, UIMAException
    {
        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);

        aUIData.setGetDocument(true);
        result = controller.getDocumentResponse(bratAnnotatorModel,
                aUIData.getAnnotationOffsetStart(), aUIData.getjCas(), aUIData.isGetDocument());
        aUIData.setGetDocument(false);

        return result;
    }

    public static Object createSpan(IRequestParameters aRequest, User aUser,
            BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel,
            MappingJacksonHttpMessageConverter jsonConverter)
        throws ClassNotFoundException, IOException, UIMAException, MultipleSentenceCoveredException
    {
        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);
        String offsets = aRequest.getParameterValue("offsets").toString();

        OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(offsets,
                OffsetsList.class);
        int start = offsetLists.get(0).getBegin();
        int end = offsetLists.get(0).getEnd();
        
        Sentence sentence = selectAnnotationByAddress(aUIData.getjCas(), Sentence.class,
                bratAnnotatorModel.getSentenceAddress());
        AnnotationFS originFs = selectAnnotationByAddress(aUIData.getjCas(), aUIData.getOrigin());
        AnnotationFS targetFs = selectAnnotationByAddress(aUIData.getjCas(), aUIData.getTarget());
        
        aUIData.setAnnotationOffsetStart(sentence.getBegin() + start);
        aUIData.setAnnotationOffsetEnd(sentence.getBegin() + end);
        aUIData.setType(aRequest.getParameterValue("type").toString());

        if (!BratAjaxCasUtil.offsetsInOneSentences(aUIData.getjCas(),
                aUIData.getAnnotationOffsetStart(), aUIData.getAnnotationOffsetEnd())) {
            throw new MultipleSentenceCoveredException(
                    "You selected a span across multiple sentences. Limit your span annotations to single sentences!");
        }


        result = controller.createSpanResponse(bratAnnotatorModel,
                aUIData.getAnnotationOffsetStart(), aUIData.getjCas(), aUIData.isGetDocument(),
                aUIData.getAnnotationOffsetEnd(), aUIData.getType(), originFs, targetFs);

        if (bratAnnotatorModel.isScrollPage()) {
            bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                    aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
        }
        return result;
    }

    public static Object createArc(IRequestParameters aRequest, User aUser,
            BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel)
        throws UIMAException, IOException
    {
        int origin = aRequest.getParameterValue("origin").toInt();
        int target = aRequest.getParameterValue("target").toInt();

        AnnotationFS originFs = selectAnnotationByAddress(aUIData.getjCas(), origin);
        AnnotationFS targetFs = selectAnnotationByAddress(aUIData.getjCas(), target);

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);
        aUIData.setOrigin(origin);
        aUIData.setTarget(target);
        aUIData.setType(aRequest.getParameterValue("type").toString());
        aUIData.setAnnotationOffsetStart(originFs.getBegin());


        result = controller.createArcResponse(bratAnnotatorModel,
                aUIData.getAnnotationOffsetStart(), aUIData.getjCas(), aUIData.isGetDocument(),
                aUIData.getType(), aUIData.getAnnotationOffsetEnd(), originFs, targetFs);
        if (bratAnnotatorModel.isScrollPage()) {
            bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                    aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
        }
        return result;
    }

    public static Object reverseArc(IRequestParameters aRequest, User aUser,
            BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel)
        throws UIMAException, IOException
    {
        Object result = null;
        
        int origin = aRequest.getParameterValue("origin").toInt();
        int target = aRequest.getParameterValue("target").toInt();
        
        AnnotationFS originFs = selectAnnotationByAddress(aUIData.getjCas(), origin);
        AnnotationFS targetFs = selectAnnotationByAddress(aUIData.getjCas(), target);
        
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);
        aUIData.setOrigin(origin);
        aUIData.setTarget(target);
        aUIData.setType(aRequest.getParameterValue("type").toString());
        aUIData.setAnnotationOffsetStart(originFs.getBegin());

        String annotationType = aUIData.getType().substring(0,
                aUIData.getType().indexOf(AnnotationTypeConstant.PREFIX) + 1);
        if (annotationType.equals(AnnotationTypeConstant.POS_PREFIX)) {
            result = controller.reverseArcResponse(bratAnnotatorModel, aUIData.getjCas(),
                    aUIData.getAnnotationOffsetStart(), originFs, targetFs, aUIData.getType(),
                    aUIData.isGetDocument());
            if (bratAnnotatorModel.isScrollPage()) {
                bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                        aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                        aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                        bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
            }
        }
        return result;
    }

    public static Object deleteSpan(IRequestParameters aRequest, User aUser,
            BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel,
            MappingJacksonHttpMessageConverter jsonConverter)
        throws JsonParseException, JsonMappingException, UIMAException, IOException
    {
        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);

        String offsets = aRequest.getParameterValue("offsets").toString();
        int id = aRequest.getParameterValue("id").toInt();
        OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(offsets,
                OffsetsList.class);
        int start = offsetLists.get(0).getBegin();
        int end = offsetLists.get(0).getEnd();
        
        Sentence sentence = selectAnnotationByAddress(aUIData.getjCas(), Sentence.class,
                bratAnnotatorModel.getSentenceAddress());
        
        aUIData.setAnnotationOffsetStart(sentence.getBegin() + start);
        aUIData.setAnnotationOffsetEnd(sentence.getEnd() + end);
        aUIData.setType(aRequest.getParameterValue("type").toString());

        AnnotationFS idFs = selectAnnotationByAddress(aUIData.getjCas(), id);

        result = controller.deleteSpanResponse(bratAnnotatorModel, idFs,
                aUIData.getAnnotationOffsetStart(), aUIData.getjCas(), aUIData.isGetDocument(),
                aUIData.getType());
        
        if (bratAnnotatorModel.isScrollPage()) {
            bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                    aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
        }
        return result;
    }

    public static Object deleteArc(IRequestParameters aRequest, User aUser,
            BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel)
        throws UIMAException, IOException
    {
        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);

        int origin = aRequest.getParameterValue("origin").toInt();
        int target = aRequest.getParameterValue("target").toInt();

        AnnotationFS originFs = selectAnnotationByAddress(aUIData.getjCas(), origin);
        AnnotationFS targetFs = selectAnnotationByAddress(aUIData.getjCas(), target);

        aUIData.setOrigin(origin);
        aUIData.setTarget(target);
        aUIData.setType(aRequest.getParameterValue("type").toString());
        aUIData.setAnnotationOffsetStart(originFs.getBegin());

        result = controller.deleteArcResponse(bratAnnotatorModel, aUIData.getjCas(),
                aUIData.getAnnotationOffsetStart(), originFs, targetFs, aUIData.getType(),
                aUIData.isGetDocument());
        
        if (bratAnnotatorModel.isScrollPage()) {
            bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                    aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
        }
        return result;
    }

    public static boolean isDocumentFinished(RepositoryService aRepository,
            BratAnnotatorModel aBratAnnotatorModel)
    {
        // if annotationDocument is finished, disable editing
        boolean finished = false;
        try {
            if (aBratAnnotatorModel.getMode().equals(Mode.CURATION)) {
                if (aBratAnnotatorModel.getDocument().getState()
                        .equals(SourceDocumentState.CURATION_FINISHED)) {
                    finished = true;
                }
            }
            else if (aRepository
                    .getAnnotationDocument(aBratAnnotatorModel.getDocument(),
                            aBratAnnotatorModel.getUser()).getState()
                    .equals(AnnotationDocumentState.FINISHED)/*
                                                              * ||
                                                              * aBratAnnotatorModel.getDocument().
                                                              * getState()
                                                              * .equals(SourceDocumentState
                                                              * .CURATION_FINISHED) ||
                                                              * aBratAnnotatorModel
                                                              * .getDocument().getState()
                                                              * .equals(SourceDocumentState
                                                              * .CURATION_IN_PROGRESS)
                                                              */) {
                finished = true;
            }
        }
        catch (Exception e) {
            finished = false;
        }

        return finished;
    }

    public static void clearJcasAnnotations(JCas aJCas, SourceDocument aSourceDocument, User aUser,
            RepositoryService repository)
        throws IOException
    {
        List<Annotation> annotationsToRemove = new ArrayList<Annotation>();
        for (Annotation a : select(aJCas, Annotation.class)) {
            if (!(a instanceof Token || a instanceof Sentence || a instanceof DocumentMetaData)) {
                annotationsToRemove.add(a);
            }
        }
        for (Annotation annotation : annotationsToRemove) {
            aJCas.removeFsFromIndexes(annotation);
        }
        repository.createAnnotationDocumentContent(aJCas, aSourceDocument, aUser);
    }
    
    public static void upgradeCasAndSave( RepositoryService aRepository, SourceDocument aDocument
    		, Mode aMode)
    {
        String username = SecurityContextHolder.getContext().getAuthentication()
                .getName();

        User user = aRepository.getUser(username);
        if(aRepository.existsAnnotationDocument(aDocument, user)){
        AnnotationDocument annotationDocument = aRepository
                .getAnnotationDocument(aDocument, user);
        try {
            if (aMode.equals(Mode.ANNOTATION) || aMode.equals(Mode.CORRECTION)) {
                CAS cas = aRepository.getAnnotationDocumentContent(
                        annotationDocument).getCas();
                upgrade(cas);
                aRepository.createAnnotationDocumentContent(cas.getJCas(),
                        annotationDocument.getDocument(), user);
            }
            else {
                CAS cas = aRepository.getCurationDocumentContent(
                        aDocument).getCas();
                upgrade(cas);
                aRepository.createCurationDocumentContent(cas.getJCas(),
                        aDocument, user);
            }

        }
        catch (UIMAException e) {
            
        }
        catch (ClassNotFoundException e) {
            
        }
        catch (IOException e) {
           
        }
        catch (Exception e) {
            // no need to catch, it is acceptable that no curation document
            // exists to be upgraded while there are annotation documents
        }

        }
    }

    public static void upgrade(CAS aCas)
        throws UIMAException, IOException
    {
        // Prepare template for new CAS
        CAS newCas = JCasFactory.createJCas().getCas();
        CASCompleteSerializer serializer = Serialization.serializeCASComplete((CASImpl) newCas);

        // Save old type system
        TypeSystem oldTypeSystem = aCas.getTypeSystem();

        // Save old CAS contents
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        Serialization.serializeWithCompression(aCas, os2, oldTypeSystem);

        // Prepare CAS with new type system
        Serialization.deserializeCASComplete(serializer, (CASImpl) aCas);

        // Restore CAS data to new type system
        Serialization.deserializeCAS(aCas, new ByteArrayInputStream(os2.toByteArray()),
                oldTypeSystem, null);
    }

}
