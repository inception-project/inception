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
package de.tudarmstadt.ukp.inception.io.rdf;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.io.rdf.config.RdfFormatAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.ui.kb.feature.ConceptFeatureSupport;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RdfFormatAutoConfiguration#uimaRdfCasFormatSupport}.
 * </p>
 */
public class UimaRdfCasFormatSupport
    implements FormatSupport
{
    public static final String ID = "rdfcas";
    public static final String NAME = "UIMA CAS RDF";

    private final AnnotationSchemaService schemaService;

    public UimaRdfCasFormatSupport(AnnotationSchemaService aSchemaService)
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
        return createReaderDescription(RdfReader.class, aTSD);
    }

    @Override
    public AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        var iriFeatures = schemaService.listAnnotationFeature(aProject).stream()
                .filter(f -> f.getType().startsWith(ConceptFeatureSupport.PREFIX))
                .map(f -> f.getLayer().getName() + ":" + f.getName()).collect(toUnmodifiableSet());

        return createEngineDescription(RdfWriter.class, aTSD, //
                RdfWriter.PARAM_IRI_FEATURES, iriFeatures);
    }
}
