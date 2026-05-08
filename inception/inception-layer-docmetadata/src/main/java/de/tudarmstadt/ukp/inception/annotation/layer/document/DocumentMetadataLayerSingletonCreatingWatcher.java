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
package de.tudarmstadt.ukp.inception.annotation.layer.document;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.annotation.events.BeforeDocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.config.DocumentMetadataLayerSupportAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;

/**
 * Initializes singleton document-level annotations if necessary when a new document is opened.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentMetadataLayerSupportAutoConfiguration#documentMetadataLayerSingletonCreatingWatcher}.
 * </p>
 */
public class DocumentMetadataLayerSingletonCreatingWatcher
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AnnotationSchemaService annotationService;
    private final LayerSupportRegistry layerSupportRegistry;

    public DocumentMetadataLayerSingletonCreatingWatcher(DocumentService aDocumentService,
            AnnotationSchemaService aAnnotationService, LayerSupportRegistry aLayerRegistry)
    {
        super();
        annotationService = aAnnotationService;
        layerSupportRegistry = aLayerRegistry;
    }

    private List<AnnotationLayer> listMetadataLayers(Project aProject)
    {
        return annotationService.listAnnotationLayer(aProject).stream()
                .filter(layer -> DocumentMetadataLayerSupport.TYPE.equals(layer.getType())
                        && layer.isEnabled())
                .toList();
    }

    @EventListener
    @Transactional
    public void onBeforeDocumentOpenedEvent(BeforeDocumentOpenedEvent aEvent)
        throws IOException, AnnotationException
    {
        if (!aEvent.isEditable()) {
            return;
        }

        var cas = aEvent.getCas();
        for (var layer : listMetadataLayers(aEvent.getDocument().getProject())) {
            if (!getLayerSupport(layer).readTraits(layer).isSingleton()) {
                continue;
            }

            var adapter = (DocumentMetadataLayerAdapter) annotationService.getAdapter(layer);
            var maybeType = adapter.getAnnotationType(cas);
            if (maybeType.isPresent()) {
                if (cas.select(maybeType.get()).isEmpty()) {
                    adapter.add(aEvent.getDocument(), aEvent.getSessionOwner(), cas);
                }
            }
            else {
                LOG.warn("CAS does not contain type [{}] - not adding singleton!", layer.getName());
            }
        }
    }

    private DocumentMetadataLayerSupport getLayerSupport(AnnotationLayer aLayer)
    {
        return (DocumentMetadataLayerSupport) layerSupportRegistry.getLayerSupport(aLayer);
    }

}
