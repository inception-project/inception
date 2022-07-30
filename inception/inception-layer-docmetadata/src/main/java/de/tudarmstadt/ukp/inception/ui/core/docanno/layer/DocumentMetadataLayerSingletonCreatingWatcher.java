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
package de.tudarmstadt.ukp.inception.ui.core.docanno.layer;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.annotation.events.BeforeDocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.ui.core.docanno.config.DocumentMetadataLayerSupportAutoConfiguration;

/**
 * Initializes singleton document-level annotations if necessary when a new document is opened.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentMetadataLayerSupportAutoConfiguration#documentMetadataLayerSingletonCreatingWatcher}.
 * </p>
 */
public class DocumentMetadataLayerSingletonCreatingWatcher
{
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
                .collect(Collectors.toList());
    }

    @EventListener
    @Transactional
    public void onBeforeDocumentOpenedEvent(BeforeDocumentOpenedEvent aEvent)
        throws IOException, AnnotationException
    {
        if (!aEvent.isEditable()) {
            return;
        }

        CAS cas = aEvent.getCas();
        for (AnnotationLayer layer : listMetadataLayers(aEvent.getDocument().getProject())) {
            if (!getLayerSupport(layer).readTraits(layer).isSingleton()) {
                continue;
            }

            DocumentMetadataLayerAdapter adapter = (DocumentMetadataLayerAdapter) annotationService
                    .getAdapter(layer);
            if (cas.select(adapter.getAnnotationType(cas)).isEmpty()) {
                adapter.add(aEvent.getDocument(), aEvent.getUser(), cas);
            }
        }
    }

    private DocumentMetadataLayerSupport getLayerSupport(AnnotationLayer aLayer)
    {
        return (DocumentMetadataLayerSupport) layerSupportRegistry.getLayerSupport(aLayer);
    }

}
