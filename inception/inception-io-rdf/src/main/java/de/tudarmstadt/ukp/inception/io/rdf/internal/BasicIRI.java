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
package de.tudarmstadt.ukp.inception.io.rdf.internal;

import org.eclipse.rdf4j.model.base.AbstractIRI;

public class BasicIRI
    extends AbstractIRI
{
    private static final long serialVersionUID = 4794310809421877727L;

    private final String namespace;
    private final String localName;

    public BasicIRI(String aNamespace, String aLocalName)
    {
        namespace = aNamespace;
        localName = aLocalName;
    }

    @Override
    public String getNamespace()
    {
        return namespace;
    }

    @Override
    public String getLocalName()
    {
        return localName;
    }
}
