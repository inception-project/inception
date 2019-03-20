/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.format;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public interface FormatSupport
{
    String getId();

    String getName();

    default boolean isReadable()
    {
        return false;
    }

    default boolean isWritable()
    {
        return false;
    }

    default CollectionReaderDescription getReaderDescription()
        throws ResourceInitializationException
    {
        throw new UnsupportedOperationException("The format [" + getName() + "] cannot be read");
    }

    default AnalysisEngineDescription getWriterDescription(Project aProject, CAS aCAS)
        throws ResourceInitializationException
    {
        throw new UnsupportedOperationException("The format [" + getName() + "] cannot be written");
    }
}
