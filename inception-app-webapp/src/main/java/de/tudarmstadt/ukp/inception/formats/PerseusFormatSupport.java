/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.formats;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;

@Component
public class PerseusFormatSupport
    implements FormatSupport
{
    public static final String ID = "perseus_2.1";
    public static final String NAME = "Perseus treebank XML (2.1)";
    
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
        return false;
    }

    @Override
    public CollectionReaderDescription getReaderDescription() throws ResourceInitializationException
    {
        return createReaderDescription(PerseusReader.class);
    }
}
