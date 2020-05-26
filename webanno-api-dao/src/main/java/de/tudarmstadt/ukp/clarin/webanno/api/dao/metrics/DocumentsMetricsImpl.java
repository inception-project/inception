/*
 * Copyright 2020
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;

@ManagedResource
@Service
@ConditionalOnProperty(prefix = "monitoring.metrics", name = "enabled", havingValue = "true")
public class DocumentsMetricsImpl
    implements DocumentsMetrics
{
    private final DocumentService documentService;

    @Autowired
    public DocumentsMetricsImpl(DocumentService aDocumentService)
    {
        documentService = aDocumentService;
    }

    @Override
    @ManagedAttribute
    public long getDocumentsTotal()
    {
        return documentService.countSourceDocuments();
    }

    @Override
    @ManagedAttribute
    public long getAnnotationDocumentsTotal()
    {
        return documentService.countAnnotationDocuments();
    }

}
