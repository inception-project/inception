/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.curation.casmerge;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;

/**
 * Indiates that a merge operation could not be performed because the annotation had already been 
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
