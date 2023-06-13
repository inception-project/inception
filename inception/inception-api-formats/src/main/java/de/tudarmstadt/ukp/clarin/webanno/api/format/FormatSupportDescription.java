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
package de.tudarmstadt.ukp.clarin.webanno.api.format;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class FormatSupportDescription
    implements FormatSupport
{
    private final String id;
    private final String name;
    private final String readerClass;
    private final String writerClass;

    public FormatSupportDescription(String aId, String aName, String aReaderClass,
            String aWriterClass)
    {
        id = aId;
        name = aName;
        readerClass = aReaderClass;
        writerClass = aWriterClass;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isReadable()
    {
        return readerClass != null;
    }

    @Override
    public boolean isWritable()
    {
        return writerClass != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CollectionReaderDescription getReaderDescription(Project aProject,
            TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        if (!isReadable()) {
            throw new UnsupportedOperationException(
                    "The format [" + getName() + "] cannot be read");
        }

        Class<? extends CollectionReader> readerClazz;
        try {
            readerClazz = (Class<? extends CollectionReader>) Class.forName(readerClass);
        }
        catch (ClassNotFoundException e) {
            throw new ResourceInitializationException(e);
        }

        return createReaderDescription(readerClazz, aTSD);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        if (!isReadable()) {
            throw new UnsupportedOperationException(
                    "The format [" + getName() + "] cannot be written");
        }

        Class<? extends AnalysisComponent> writerClazz;
        try {
            writerClazz = (Class<? extends AnalysisComponent>) Class.forName(writerClass);
        }
        catch (ClassNotFoundException e) {
            throw new ResourceInitializationException(e);
        }

        return createEngineDescription(writerClazz, aTSD);
    }
}
