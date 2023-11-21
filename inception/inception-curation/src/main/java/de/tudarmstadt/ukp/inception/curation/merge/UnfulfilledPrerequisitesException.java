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
 * Indicates that the prerequisites to perform a merge operation are not fulfilled. E.g. on an
 * attempt to merge a relation if no suitable end-points for the relations are present in the target
 * CAS.
 */
public class UnfulfilledPrerequisitesException
    extends AnnotationException
{
    private static final long serialVersionUID = -6592130672804779018L;

    public UnfulfilledPrerequisitesException()
    {
        super();
    }

    public UnfulfilledPrerequisitesException(String message)
    {
        super(message);
    }

    public UnfulfilledPrerequisitesException(String aMessage, Throwable aCause)
    {
        super(aMessage, aCause);
    }

    public UnfulfilledPrerequisitesException(Throwable aCause)
    {
        super(aCause);
    }
}
