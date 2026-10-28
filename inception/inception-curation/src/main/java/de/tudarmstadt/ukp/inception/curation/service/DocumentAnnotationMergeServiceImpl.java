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
package de.tudarmstadt.ukp.inception.curation.service;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.curation.api.DocumentAnnotationMergeService;
import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.merge.CasMerge;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationServiceAutoConfiguration#documentAnnotationMergeService}.
 * </p>
 */
public class DocumentAnnotationMergeServiceImpl
    implements DocumentAnnotationMergeService
{
    private final AnnotationSchemaService annotationService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DocumentAnnotationMergeServiceImpl(AnnotationSchemaService aAnnotationService,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        annotationService = aAnnotationService;
        applicationEventPublisher = aApplicationEventPublisher;
    }

    @Override
    public void mergeDocumentAnnotation(SourceDocument aDocument, String aTargetDataOwner,
            AnnotationLayer aLayer, CAS aTargetCas, AnnotationBase aSourceAnnotation)
        throws AnnotationException
    {
        var casMerge = new CasMerge(annotationService, applicationEventPublisher);
        casMerge.mergeDocumentAnnotation(aDocument, aTargetDataOwner, aLayer, aTargetCas,
                aSourceAnnotation);
    }
}
