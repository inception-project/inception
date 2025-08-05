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
package de.tudarmstadt.ukp.inception.io.brat;

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.io.brat.config.BratAutoConfiguration;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.BratReader;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.BratWriter;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

/**
 * Support for brat format.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link BratAutoConfiguration#bratCustomFormatSupport}.
 * </p>
 */
public class BratCustomFormatSupport
    implements FormatSupport
{
    public static final String ID = "bratCustom";
    public static final String NAME = "brat custom (experimental)";

    private final AnnotationSchemaService schemaService;

    public BratCustomFormatSupport(AnnotationSchemaService aSchemaService)
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
        return createReaderDescription( //
                BratReader.class, aTSD, //
                BratReader.PARAM_LENIENT, true);
    }

    @Override
    public AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        var layers = schemaService.listAnnotationLayer(aProject);

        var relationLayerMappings = layers.stream() //
                .filter(layer -> RelationLayerSupport.TYPE.equals(layer.getType()))
                .map(layer -> layer.getName() + ":" + FEAT_REL_SOURCE + ":" + FEAT_REL_TARGET)
                .toList();

        return createEngineDescription( //
                BratWriter.class, aTSD, //
                BratWriter.PARAM_SHORT_TYPE_NAMES, true, //
                BratWriter.PARAM_SHORT_ATTRIBUTE_NAMES, true, //
                BratWriter.PARAM_RELATION_TYPES, relationLayerMappings);
    }
}
