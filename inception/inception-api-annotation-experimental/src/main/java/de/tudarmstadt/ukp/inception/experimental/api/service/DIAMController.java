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
package de.tudarmstadt.ukp.inception.experimental.api.service;

import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_REPOSITORY_PATH;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_USERNAME;
import static java.lang.Integer.MAX_VALUE;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.experimental.api.model.MViewportInit;
import de.tudarmstadt.ukp.inception.experimental.api.model.MViewportUpdate;

/**
 * Differential INCEpTION Annotation Messaging (DIAM) protocol controller.
 */
@Controller
public class DIAMController
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SimpMessagingTemplate msgTemplate;
    private final PreRenderer preRenderer;
    private final DocumentService documentService;
    private final RepositoryProperties repositoryProperties;
    private final AnnotationSchemaService schemaService;

    private final LoadingCache<ViewportDefinition, ViewportState> activeViewports;

    public DIAMController(SimpMessagingTemplate aMsgTemplate, PreRenderer aPreRenderer,
            DocumentService aDocumentService, RepositoryProperties aRepositoryProperties,
            AnnotationSchemaService aSchemaService)
    {
        msgTemplate = aMsgTemplate;
        preRenderer = aPreRenderer;
        documentService = aDocumentService;
        repositoryProperties = aRepositoryProperties;
        schemaService = aSchemaService;

        activeViewports = Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(30))
                .build(this::initState);
    }

    @SubscribeMapping("/project/{projectId}/document/{documentId}/user/{user}/from/{from}/to/{to}")
    public JsonNode onSubscribeToAnnotationDocument(Principal aPrincipal,
            @DestinationVariable("projectId") long aProjectId,
            @DestinationVariable("documentId") long aDocumentId,
            @DestinationVariable("user") String aUser,
            @DestinationVariable("from") int aViewportBegin,
            @DestinationVariable("to") int aViewportEnd)
        throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            MDC.put(KEY_USERNAME, aPrincipal.getName());

            ViewportDefinition vpd = new ViewportDefinition(aProjectId, aDocumentId, aUser,
                    aViewportBegin, aViewportEnd);

            // Ensure that the viewport is registered
            ViewportState vps = activeViewports.get(vpd);

            VDocument vdoc = render(aProjectId, aDocumentId, aUser, aViewportBegin, aViewportEnd);

            JsonNode newJson = JSONUtil.getObjectMapper().valueToTree(new MViewportInit(vdoc));
            vps.setJson(newJson);

            return newJson;
        }
        finally {
            MDC.remove(KEY_REPOSITORY_PATH);
            MDC.remove(KEY_USERNAME);
        }
    }

    private VDocument render(long aProjectId, long aDocumentId, String aUser, int aViewportBegin,
            int aViewportEnd)
        throws IOException
    {
        SourceDocument doc = documentService.getSourceDocument(aProjectId, aDocumentId);
        CAS cas = documentService.readAnnotationCas(doc, aUser);

        List<AnnotationLayer> layers = schemaService.listSupportedLayers(null).stream()
                .filter(AnnotationLayer::isEnabled).collect(toList());

        VDocument vdoc = new VDocument();
        preRenderer.render(vdoc, aViewportBegin, aViewportEnd, cas, layers);
        return vdoc;
    }

    private ViewportState initState(ViewportDefinition aVpd)
    {
        return new ViewportState(aVpd);
    }

    public void sendUpdate(AnnotationDocument aDoc)
    {
        sendUpdate(aDoc.getProject().getId(), aDoc.getDocument().getId(), aDoc.getUser(), 0,
                MAX_VALUE);
    }

    public void sendUpdate(AnnotationDocument aDoc, int aUpdateBegin, int aUpdateEnd)
    {
        sendUpdate(aDoc.getProject().getId(), aDoc.getDocument().getId(), aDoc.getUser(),
                aUpdateBegin, aUpdateEnd);
    }

    public void sendUpdate(long aProjectId, long aDocumentId, String aUser, int aUpdateBegin,
            int aUpdateEnd)
    {
        for (Entry<ViewportDefinition, ViewportState> e : activeViewports.asMap().entrySet()) {
            var vpd = e.getKey();
            var vps = e.getValue();

            if (!vpd.matches(aDocumentId, aUser, aUpdateBegin, aUpdateEnd)) {
                continue;
            }

            // MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            try (CasStorageSession session = CasStorageSession.open()) {
                VDocument vdoc = render(vpd.getProjectId(), vpd.getDocumentId(), vpd.getUser(),
                        vpd.getBegin(), vpd.getEnd());

                MViewportInit fullRender = new MViewportInit(vdoc);

                JsonNode newJson = JSONUtil.getObjectMapper().valueToTree(fullRender);

                vps.setJson(newJson);

                JsonNode diff = JsonDiff.asJson(vps.getJson(), newJson);

                msgTemplate.convertAndSend("/topic" + vpd.getTopic(),
                        new MViewportUpdate(aUpdateBegin, aUpdateEnd, diff));
            }
            catch (Exception ex) {
                log.error("Unable to render update", ex);
            }

            // finally {
            // MDC.remove(KEY_REPOSITORY_PATH);
            // }
        }
    }
}
