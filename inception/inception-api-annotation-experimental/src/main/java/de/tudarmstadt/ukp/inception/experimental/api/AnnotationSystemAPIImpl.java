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

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.experimental.api.message.AnnotationMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.DocumentMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.ViewportMessage;
import de.tudarmstadt.ukp.inception.experimental.api.websocket.AnnotationProcessAPI;

@Component
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true")
public class AnnotationSystemAPIImpl
    implements AnnotationSystemAPI
{

    private final ProjectService projectService;
    private final DocumentService documentService;
    private final UserDao userDao;
    private final RepositoryProperties repositoryProperties;
    private final AnnotationProcessAPI annotationProcessAPI;


    public AnnotationSystemAPIImpl(ProjectService aProjectService, DocumentService aDocumentService,
            UserDao aUserDao, RepositoryProperties aRepositoryProperties,
            AnnotationProcessAPI aAnnotationProcessAPI)
    {

        projectService = aProjectService;
        documentService = aDocumentService;
        userDao = aUserDao;
        repositoryProperties = aRepositoryProperties;
        annotationProcessAPI = aAnnotationProcessAPI;
    }

    @Override
    public void handleDocument(String[] aData) throws IOException
    {
        // TODO receive random new document
        CAS cas = getCasForDocument(aData[0], Long.parseLong(aData[1]), 41709);
        DocumentMessage message = new DocumentMessage();
        message.setName("Doc4");
        String[] sentences = cas.getDocumentText().split("/.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Integer.parseInt(aData[2]); i++) {
            sb.append(sentences[i]);
        }
        message.setViewportText(sb.toString());
        annotationProcessAPI.handleSendDocumentRequest(message, aData[0]);
    }

    @Override
    public void handleViewport(String[] aData) throws IOException
    {
        CAS cas = getCasForDocument(aData[0], Long.parseLong(aData[1]), Long.parseLong(aData[2]));
        ViewportMessage message = new ViewportMessage(Integer.parseInt(aData[3]),
                Integer.parseInt(aData[4]));
        String[] sentences = cas.getDocumentText().split("/.");
        StringBuilder sb = new StringBuilder();
        if (Integer.parseInt(aData[3]) < 0) {
            for (int i = sentences.length + Integer.parseInt(aData[3]); i < sentences.length
                    - 1; i++) {
                sb.append(sentences[i]);
            }
        }
        else {
            if (sentences.length >= Integer.parseInt(aData[4])) {
                for (int i = Integer.parseInt(aData[3]); i < Integer.parseInt(aData[4]); i++) {
                    sb.append(sentences[i]);
                }
            }
            else {
                System.out.println("Requested sentences do not exist in the document");
            }
        }
        message.setText(sb.toString());
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
}
