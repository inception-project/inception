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
package de.tudarmstadt.ukp.inception.project.initializers.doclabeling.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.config.DocumentMetadataLayerSupportAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.layer.document.sidebar.DocumentMetadataSidebarFactory;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.initializers.doclabeling.BasicDocumentLabelLayerInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.doclabeling.BasicDocumentLabelTagSetInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.doclabeling.BasicDocumentLabelingProjectInitializer;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@AutoConfigureAfter({ DocumentMetadataLayerSupportAutoConfiguration.class })
@Configuration
public class InceptionDocumentLabelingProjectInitializersAutoConfiguration
{
    @ConditionalOnBean(DocumentMetadataLayerSupport.class)
    @Bean
    public BasicDocumentLabelLayerInitializer basicDocumentTagLayerInitializer(
            AnnotationSchemaService aAnnotationSchemaService,
            DocumentMetadataLayerSupport aDocLayerSupport)
    {
        return new BasicDocumentLabelLayerInitializer(aAnnotationSchemaService, aDocLayerSupport);
    }

    @ConditionalOnBean(DocumentMetadataLayerSupport.class)
    @Bean
    BasicDocumentLabelingProjectInitializer basicDocumentLabelingProjectInitializer(
            PreferencesService aPreferencesService, DocumentMetadataSidebarFactory aDocMetaSidebar,
            WorkloadManagementService aWorkloadManagementService,
            MatrixWorkloadExtension aMatrixWorkloadExtension)
    {
        return new BasicDocumentLabelingProjectInitializer(aPreferencesService, aDocMetaSidebar,
                aWorkloadManagementService, aMatrixWorkloadExtension);
    }

    @ConditionalOnBean(DocumentMetadataLayerSupport.class)
    @Bean
    BasicDocumentLabelTagSetInitializer basicDocumentLabelTagSetInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new BasicDocumentLabelTagSetInitializer(aAnnotationSchemaService);
    }
}
