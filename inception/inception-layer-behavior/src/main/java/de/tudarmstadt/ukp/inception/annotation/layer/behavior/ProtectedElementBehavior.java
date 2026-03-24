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
package de.tudarmstadt.ukp.inception.annotation.layer.behavior;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.matchesAny;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.config.LayerBehaviorAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.MoveSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAnnotationRequest_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link LayerBehaviorAutoConfiguration#protectedElementBehavior}.
 * </p>
 */
@Order(90)
public class ProtectedElementBehavior
    extends SpanLayerBehavior
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DocumentImportExportService documentImportExportService;
    private final DocumentService documentService;

    public ProtectedElementBehavior(DocumentService aDocumentService,
            DocumentImportExportService aDocumentImportExportService)
    {
        documentImportExportService = aDocumentImportExportService;
        documentService = aDocumentService;
    }

    @Override
    public boolean accepts(LayerSupport<?, ?> aLayerType)
    {
        return super.accepts(aLayerType) || aLayerType instanceof ChainLayerSupport;
    }

    @Override
    public CreateSpanAnnotationRequest onCreate(TypeAdapter aAdapter,
            CreateSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        return onRequest(aAdapter, aRequest);
    }

    @Override
    public MoveSpanAnnotationRequest onMove(TypeAdapter aAdapter,
            MoveSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        return onRequest(aAdapter, aRequest);
    }

    private <T extends SpanAnnotationRequest_ImplBase<T>> T onRequest(TypeAdapter aAdapter,
            T aRequest)
        throws AnnotationException
    {
        if (Token.class.getName().equals(aAdapter.getAnnotationTypeName())
                || Sentence.class.getName().equals(aAdapter.getAnnotationTypeName())) {
            return aRequest;
        }

        if (aRequest.getBegin() == aRequest.getEnd()) {
            if (!aAdapter.getLayer().getAnchoringMode().isZeroSpanAllowed()) {
                throw new IllegalPlacementException(
                        "Cannot create zero-width annotation on layers that lock to token boundaries.");
            }

            return aRequest;
        }

        var protectedElements = documentImportExportService
                .getProtectedElements(aRequest.getDocument());
        if (protectedElements.isEmpty()) {
            return aRequest;
        }

        try (var session = CasStorageSession.openNested()) {
            var initialCas = documentService.createOrReadInitialCas(aRequest.getDocument(),
                    AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);

            // If the document does not contain XML structure, nothing to do here
            if (initialCas.select(XmlDocument.class).isEmpty()) {
                return aRequest;
            }

            var originalRange = new int[] { aRequest.getBegin(), aRequest.getEnd() };

            var adjustedRange = adjust(initialCas, protectedElements, originalRange);

            if (Arrays.equals(adjustedRange, originalRange)) {
                return aRequest;
            }

            return aRequest.changeSpan(adjustedRange[0], adjustedRange[1],
                    aAdapter.getLayer().getAnchoringMode());
        }
        catch (IOException e) {
            LOG.error("Unable to access XML structure in INITIAL_CAS", e);
            return aRequest;
        }
    }

    static int[] adjust(CAS aCas, Set<String> aProtectedElements, int[] aOriginalRange)
    {
        var adjustedRange = new int[] { aOriginalRange[0], aOriginalRange[1] };

        for (var element : aCas.select(XmlElement.class).covering(aOriginalRange[0],
                aOriginalRange[1])) {

            // It is quite inefficient to collect the mappings again for each element, but
            // currently we call adjust only on create/move actions and those usually do not
            // have to be very fast. Two options:
            // - Make collection more efficient
            // - Skip collection if none of the protected elements include a namespace ("{")
            var namespaceMappings = XmlNodeUtils.allPrefixMappings(element);

            if (matchesAny(element, namespaceMappings, aProtectedElements)) {
                // Choose the element with the largest span (smallest begin, largest end)
                if (element.getBegin() < adjustedRange[0] || element.getEnd() > adjustedRange[1]) {
                    adjustedRange[0] = element.getBegin();
                    adjustedRange[1] = element.getEnd();
                }
            }
        }

        return adjustedRange;
    }
}
