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
import static org.apache.uima.fit.util.CasUtil.selectFS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.MDC;
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
import de.tudarmstadt.ukp.inception.experimental.api.message.DocumentMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.ViewportMessage;
import de.tudarmstadt.ukp.inception.experimental.api.websocket.AnnotationProcessAPI;
import de.tudarmstadt.ukp.inception.revieweditor.AnnotationListItem;

@Component
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true")
public class AnnotationSystemAPIImpl
    implements AnnotationSystemAPI
{
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
    public void handleDocument(String[] aData) throws IOException
    {
        // TODO receive random new document
        CAS cas = getCasForDocument(aData[0], Long.parseLong(aData[1]), 41714);
        DocumentMessage message = new DocumentMessage();
        message.setId(41714);
        String[] sentences = cas.getDocumentText().split("\\.");
        ArrayList<String> visibleSentences = new ArrayList<>();

        if (sentences.length < Integer.parseInt(aData[2])) {
            aData[2] = String.valueOf(sentences.length - 1);
        }

        for (int i = 0; i < Integer.parseInt(aData[2]); i++) {
            visibleSentences.add(sentences[i].replace("\n", ""));
        }

        message.setViewportText(visibleSentences.toArray(new String[0]));

        message.setAnnotations(getAnnotations(cas, Long.parseLong(aData[1])));

        annotationProcessAPI.handleSendDocumentRequest(message, aData[0]);
    }

    @Override
    public void handleViewport(String[] aData) throws IOException
    {
        CAS cas = getCasForDocument(aData[0], Long.parseLong(aData[1]), Long.parseLong(aData[2]));

        String[] sentences = cas.getDocumentText().split("\\.");
        ArrayList<String> visibleSentences = new ArrayList<>();

        ViewportMessage message = null;

        if (aData[3].equals("-100")) {
            aData[3] = String.valueOf(sentences.length - (Integer.parseInt(aData[4])));
            aData[4] = String.valueOf(sentences.length - 1);
        }
        if (sentences.length >= Integer.parseInt(aData[4])) {
            for (int i = Integer.parseInt(aData[3]); i <= Integer.parseInt(aData[4]); i++) {
                visibleSentences.add(sentences[i].replace("\n", ""));
            }
            message = new ViewportMessage(Integer.parseInt(aData[3]), (Integer.parseInt(aData[4])));
        }
        else {
            for (int i = sentences.length
                    - (Integer.parseInt(aData[4]) - Integer.parseInt(aData[3]))
                    - 1; i < sentences.length; i++) {
                visibleSentences.add(sentences[i].replace("\n", ""));
                message = new ViewportMessage(sentences.length
                        - (Integer.parseInt(aData[4]) - Integer.parseInt(aData[3])) - 1,
                        sentences.length - 1);
            }
        }

        message.setText(visibleSentences.toArray(new String[0]));
        message.setAnnotations(getAnnotations(cas, Long.parseLong(aData[1])));

        annotationProcessAPI.handleSendViewportRequest(message, aData[0]);

    }

    @Override
    public void handleSelectAnnotation(String[] aData) throws IOException
    {
        CAS cas = getCasForDocument(aData[0], Long.parseLong(aData[1]), Long.parseLong(aData[2]));
        AnnotationFS annotation = selectAnnotationByAddr(cas, Integer.parseInt(aData[3]));
        AnnotationMessage message = new AnnotationMessage();
        message.setType(annotation.getType().getShortName());
        message.setText(annotation.getCoveredText());
        annotationProcessAPI.handleSendSelectAnnotation(message, aData[0]);
    }

    @Override
    public void handleCreateAnnotation(String[] aData) throws IOException
    {
        CAS cas = getCasForDocument(aData[0], Long.parseLong(aData[1]), Long.parseLong(aData[2]));
        // TODO createAnnotation
        AnnotationMessage message = new AnnotationMessage();
        // TODO retrieve desired content and fill AnnotationMessage
        annotationProcessAPI.handleSendUpdateAnnotation(message, aData[1], aData[2], "1");
    }

    @Override
    public void handleDeleteAnnotation(String[] aData) throws IOException
    {
        CAS cas = getCasForDocument(aData[0], Long.parseLong(aData[1]), Long.parseLong(aData[2]));

        // TODO deleteAnnotation
        AnnotationMessage message = new AnnotationMessage();
        // TODO retrieve desired content and fill AnnotationMessage
        message.setDelete(true);
        annotationProcessAPI.handleSendUpdateAnnotation(message, aData[1], aData[2], "1");
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
            return null;
        }
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
