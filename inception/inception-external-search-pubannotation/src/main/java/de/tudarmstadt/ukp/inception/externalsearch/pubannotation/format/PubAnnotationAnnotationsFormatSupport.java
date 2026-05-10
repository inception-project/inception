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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.clarin.webanno.api.format.UimaReaderWriterFormatSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

/**
 * Support for the PubAnnotation annotated-document JSON format. Read and write a document
 * containing text plus denotations / relations / attributes.
 */
public class PubAnnotationAnnotationsFormatSupport
    extends UimaReaderWriterFormatSupport_ImplBase
{
    public static final String ID = "pubannotation-annotations";
    public static final String NAME = "PubAnnotation Document with Annotations (JSON)";

    private final AnnotationSchemaService schemaService;

    public PubAnnotationAnnotationsFormatSupport(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public boolean isReadable()
    {
        return true;
    }

    @Override
    public boolean isWritable()
    {
        return true;
    }

    @Override
    public CollectionReaderDescription getReaderDescription(Project aProject,
            TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        return createReaderDescription(PubAnnotationAnnotationsReader.class, aTSD);
    }

    @Override
    public AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        var spanTypes = schemaService.listAnnotationLayer(aProject).stream()
                .filter(layer -> SpanLayerSupport.TYPE.equals(layer.getType()))
                .filter(layer -> layer.isEnabled()) //
                .map(layer -> layer.getName()) //
                .toArray(String[]::new);

        var relationTypes = schemaService.listAnnotationLayer(aProject).stream()
                .filter(layer -> RelationLayerSupport.TYPE.equals(layer.getType()))
                .filter(layer -> layer.isEnabled()) //
                .map(layer -> layer.getName()) //
                .toArray(String[]::new);

        return createEngineDescription(PubAnnotationWriter.class, aTSD, //
                PubAnnotationWriter.PARAM_SPAN_TYPES, spanTypes, //
                PubAnnotationWriter.PARAM_RELATION_TYPES, relationTypes);
    }
}
