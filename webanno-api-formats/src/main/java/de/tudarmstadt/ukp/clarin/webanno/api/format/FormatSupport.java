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
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface FormatSupport
{
    /**
     * Returns the format identifier which is stored in in {@link SourceDocument#setFormat(String)}.
     * 
     * @return the format identifier.
     */
    String getId();

    /**
     * @return a format name displayed in the UI.
     */
    String getName();

    /**
     * @return whether the format can be reader (i.e. {@link #getReaderDescription()} is
     *         implemented).
     */
    default boolean isReadable()
    {
        return false;
    }

    /**
     * @return whether the format can be written (i.e. {@link #getWriterDescription()} is
     *         implemented).
     */
    default boolean isWritable()
    {
        return false;
    }

    /**
     * @return a UIMA reader description.
     */
    default CollectionReaderDescription getReaderDescription()
        throws ResourceInitializationException
    {
        throw new UnsupportedOperationException("The format [" + getName() + "] cannot be read");
    }

    /**
     * @return a UIMA reader description.
     */
    default AnalysisEngineDescription getWriterDescription(Project aProject, CAS aCAS)
        throws ResourceInitializationException
    {
        throw new UnsupportedOperationException("The format [" + getName() + "] cannot be written");
    }
}
