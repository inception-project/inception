/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.experimental.api;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static java.lang.Math.toIntExact;
import static org.apache.uima.fit.util.CasUtil.selectFS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.MDC;
import org.springframework.asm.Type;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.experimental.api.message.AnnotationMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.ClientMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.DocumentMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.ViewportMessage;
import de.tudarmstadt.ukp.inception.experimental.api.websocket.AnnotationProcessAPI;
import de.tudarmstadt.ukp.inception.revieweditor.AnnotationListItem;

@Component
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true")
public class AnnotationSystemAPIImpl
    implements AnnotationSystemAPI
{
    private final String OFFSET_TYPE_CHAR = "char";
    private final String OFFSET_TYPE_WORD = "word";
    private final String OFFSET_TYPE_SENTENCE = "sentence";

    private final AnnotationSchemaService annotationService;
    private final ProjectService projectService;
    private final DocumentService documentService;
    private final UserDao userDao;
    private final RepositoryProperties repositoryProperties;
    private final AnnotationProcessAPI annotationProcessAPI;

    public AnnotationSystemAPIImpl(ProjectService aProjectService, DocumentService aDocumentService,
            UserDao aUserDao, RepositoryProperties aRepositoryProperties,
            AnnotationProcessAPI aAnnotationProcessAPI,
            AnnotationSchemaService annotationSchemaService)
    {

        projectService = aProjectService;
        documentService = aDocumentService;
        userDao = aUserDao;
        repositoryProperties = aRepositoryProperties;
        annotationProcessAPI = aAnnotationProcessAPI;
        this.annotationService = annotationSchemaService;

    }

    @Override
    public void handleDocument(ClientMessage aClientMessage)
    {
        try {
            CAS cas;

            DocumentMessage message = new DocumentMessage();

            if (aClientMessage.getDocument() == 0L) {
                // TODO receive random new document
                cas = getCasForDocument(aClientMessage.getUsername(), aClientMessage.getProject(),
                        41714);
                message.setId(41714);
            }
            else {
                cas = getCasForDocument(aClientMessage.getUsername(), aClientMessage.getProject(),
                        aClientMessage.getDocument());

                message.setId(
                        toIntExact(documentService.getSourceDocument(aClientMessage.getProject(),
                                aClientMessage.getDocument()).getId()));
            }

            message.setViewportText(getViewportText(aClientMessage, cas));

            message.setAnnotations(getAnnotations(cas, aClientMessage.getProject()));

            annotationProcessAPI.handleSendDocumentRequest(message, aClientMessage.getUsername());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void handleViewport(ClientMessage aClientMessage) throws IOException
    {
        CAS cas = getCasForDocument(aClientMessage.getUsername(), aClientMessage.getProject(),
                aClientMessage.getDocument());

        ViewportMessage message = new ViewportMessage();

        message.setViewportText(getViewportText(aClientMessage, cas));

        message.setAnnotations(getAnnotations(cas, aClientMessage.getProject()));

        annotationProcessAPI.handleSendViewportRequest(message, aClientMessage.getUsername());

    }

    @Override
    public void handleSelectAnnotation(ClientMessage aClientMessage) throws IOException
    {
        CAS cas = getCasForDocument(aClientMessage.getUsername(), aClientMessage.getProject(),
                aClientMessage.getDocument());
        AnnotationFS annotation = selectAnnotationByAddr(cas,
                aClientMessage.getAnnotationAddress());
        AnnotationMessage message = new AnnotationMessage();
        message.setId(String.valueOf(annotation._id()));
        message.setBegin(annotation.getBegin());
        message.setEnd(annotation.getEnd());

        message.setType(annotation.getType().getShortName());
        message.setText(annotation.getCoveredText());
        annotationProcessAPI.handleSendSelectAnnotation(message, aClientMessage.getUsername());
    }

    @Override
    public void handleCreateAnnotation(ClientMessage aClientMessage) throws IOException
    {
        CAS cas = getCasForDocument(aClientMessage.getUsername(), aClientMessage.getProject(),
                aClientMessage.getDocument());

        // TODO createAnnotation
        System.out.println(Type.getType(aClientMessage.getAnnotationType()));

        //cas.createAnnotation(aClientMessage.getAnnotationType()), aClientMessage.getAnnotationOffsetBegin(), aClientMessage.getAnnotationOffsetEnd());
        // TODO retrieve desired content and fill AnnotationMessage
        AnnotationMessage message = new AnnotationMessage();
        annotationProcessAPI.handleSendUpdateAnnotation(message,
                String.valueOf(aClientMessage.getProject()),
                String.valueOf(aClientMessage.getDocument()), "1");
    }

    @Override
    public void handleDeleteAnnotation(ClientMessage aClientMessage) throws IOException
    {
        CAS cas = getCasForDocument(aClientMessage.getUsername(), aClientMessage.getProject(),
                aClientMessage.getDocument());

        // TODO deleteAnnotation
        // cas.remo
        AnnotationMessage message = new AnnotationMessage();
        // TODO retrieve desired content and fill AnnotationMessage
        message.setDelete(true);
        annotationProcessAPI.handleSendUpdateAnnotation(message,
                String.valueOf(aClientMessage.getProject()),
                String.valueOf(aClientMessage.getDocument()), "1");
    }

    @Override
    public CAS getCasForDocument(String aUser, long aProject, long aDocument)
    {
        try {
            CasStorageSession.open();
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(aProject, aDocument);

            CAS cas = documentService.readAnnotationCas(sourceDocument, aUser);
            CasStorageSession.get().close();
            return cas;
        }
        catch (Exception e) {
            e.printStackTrace();
            CasStorageSession.get().close();
            return null;
        }
    }

    @Override
    public Character[] getViewportText(ClientMessage aClientMessage, CAS aCas)
    {

        ArrayList<Character> visibleSentences = new ArrayList<>();

        String text = aCas.getDocumentText().replace("\n", "");

        switch (aClientMessage.getOffsetType()) {
        case OFFSET_TYPE_CHAR:

            char[] textInChars = aCas.getDocumentText().replace("\n", "").toCharArray();

            for (int i = 0; i < aClientMessage.getViewport().length; i++) {
                for (int j = aClientMessage.getViewport()[i][0]; j < aClientMessage
                        .getViewport()[i][1]; j++) {
                    visibleSentences.add(textInChars[j]);
                }
                visibleSentences.add('|');
            }
            return visibleSentences.toArray(new Character[0]);

        case OFFSET_TYPE_WORD:
            String seperatorWord = " ";
            String[] textInWords = text.split(seperatorWord);

            for (int i = 1; i < textInWords.length; i++) {
                textInWords[i] = seperatorWord + textInWords[i];
            }

            for (int i = 0; i < aClientMessage.getViewport().length; i++) {
                for (int j = aClientMessage.getViewport()[i][0]; j < aClientMessage
                        .getViewport()[i][1]; j++) {
                    for (Character c : textInWords[j].toCharArray()) {
                        visibleSentences.add(c);
                    }
                }
                visibleSentences.add('|');
            }
            return visibleSentences.toArray(new Character[0]);

        case OFFSET_TYPE_SENTENCE:
            String seperatorSentence = "\\.";
            String[] textInSentences = text.split(seperatorSentence);

            for (int i = 1; i < textInSentences.length; i++) {
                textInSentences[i] = textInSentences[i] + ".";
            }

            for (int i = 0; i < aClientMessage.getViewport().length; i++) {

                for (Character c : textInSentences[aClientMessage.getViewport()[i][0]].toCharArray()) {
                    visibleSentences.add(c);
                }

                visibleSentences.add('|');
            }
            return visibleSentences.toArray(new Character[0]);
        default:
            System.err.println("Offset type not found, only: ");
        }
        return null;
    }

    @Override
    public List<Annotation> getAnnotations(CAS aCas, long aProject)
    {
        List<AnnotationListItem> items = new ArrayList<>();
        List<AnnotationLayer> metadataLayers = annotationService
                .listAnnotationLayer(projectService.getProject(aProject));

        for (AnnotationLayer layer : metadataLayers) {
            if (layer.getUiName().equals("Token")) {
                // TODO: exception later when calling renderer.getFeatures "lemma"
                continue;
            }

            TypeAdapter adapter = annotationService.getAdapter(layer);

            for (FeatureStructure fs : selectFS(aCas, adapter.getAnnotationType(aCas))) {
                String labelText = "";
                items.add(new AnnotationListItem(WebAnnoCasUtil.getAddr(fs), labelText, layer));
            }
        }

        List<Annotation> annotations = new ArrayList<>();

        for (AnnotationListItem annotationListItem : items) {
            AnnotationFS annotation = WebAnnoCasUtil.selectAnnotationByAddr(aCas,
                    annotationListItem.getAddr());
            annotations.add(new Annotation(annotation._id(), annotation.getCoveredText(),
                    annotation.getBegin(), annotation.getEnd(),
                    annotation.getType().getShortName()));
        }

        return annotations;
    }
}
