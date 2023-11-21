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
package de.tudarmstadt.ukp.inception.curation.merge;

import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

/**
 * Indicates that a merge operation could not be performed because the annotation had already been
 * merged before.
 */
public class AlreadyMergedException
    extends AnnotationException
{
    private static final long serialVersionUID = -6732300638977474716L;

    public AlreadyMergedException()
    {
        super();
    }

    public AlreadyMergedException(String message)
    {
        super(message);
    }

    public AlreadyMergedException(String aMessage, Throwable aCause)
    {
        super(aMessage, aCause);
    }

    public AlreadyMergedException(Throwable aCause)
    {
        super(aCause);
    }
}
