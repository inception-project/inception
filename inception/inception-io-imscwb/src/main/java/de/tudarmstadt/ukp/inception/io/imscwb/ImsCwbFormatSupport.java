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
package de.tudarmstadt.ukp.inception.io.imscwb;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.dkpro.core.io.imscwb.ImsCwbReader;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.io.imscwb.config.ImsCwbFormatSupportAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ImsCwbFormatSupportAutoConfiguration#imsCwbFormatSupport()}.
 * </p>
 */
public class ImsCwbFormatSupport
    implements FormatSupport
{
    public static final String ID = "imscwb";
    public static final String NAME = "Corpus Workbench Format (aka VRT)";

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

    // @Override
    // public boolean isWritable()
    // {
    // return true;
    // }

    @Override
    public CollectionReaderDescription getReaderDescription(Project aProject,
            TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        return createReaderDescription(ImsCwbReader.class, aTSD);
    }

    // @Override
    // public AnalysisEngineDescription getWriterDescription(Project aProject, CAS aCAS)
    // throws ResourceInitializationException
    // {
    // return createEngineDescription(ImsCwbWriter.class);
    // }
}
