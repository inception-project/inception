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
package de.tudarmstadt.ukp.clarin.webanno.support.wicket;

import java.io.IOException;
import java.io.InputStream;

import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

public class InputStreamResourceStream
    extends AbstractResourceStream
{
    private static final long serialVersionUID = 1L;
    
    private InputStream inputStream;

    public InputStreamResourceStream(InputStream aInputStream)
    {
        inputStream = aInputStream;
    }

    @Override
    public InputStream getInputStream() throws ResourceStreamNotFoundException
    {
        return inputStream;
    }

    @Override
    public void close() throws IOException
    {
        inputStream.close();
    }
}
