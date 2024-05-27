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
package de.tudarmstadt.ukp.inception.support.wicket;

import java.io.IOException;
import java.io.InputStream;

import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

public class InputStreamResourceStream
    extends AbstractResourceStream
{
    private static final long serialVersionUID = 1L;

    private final InputStream inputStream;
    private final String name;
    private String contentType;

    public InputStreamResourceStream(InputStream aInputStream)
    {
        inputStream = aInputStream;
        name = null;
    }

    public InputStreamResourceStream(InputStream aInputStream, String aName)
    {
        inputStream = aInputStream;
        name = aName;
    }

    public InputStreamResourceStream(InputStream aInputStream, String aName, String aContentType)
    {
        inputStream = aInputStream;
        name = aName;
        contentType = aContentType;
    }

    @Override
    public InputStream getInputStream() throws ResourceStreamNotFoundException
    {
        return inputStream;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public void close() throws IOException
    {
        inputStream.close();
    }

    public InputStreamResourceStream setContentType(String aContentType)
    {
        contentType = aContentType;
        return this;
    }

    @Override
    public String getContentType()
    {
        if (contentType != null) {
            return contentType;
        }

        return super.getContentType();
    }
}
