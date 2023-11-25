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
package de.tudarmstadt.ukp.inception.schema.api.adapter;

import de.tudarmstadt.ukp.inception.support.wicket.CommonException;

public class AnnotationException
    extends CommonException
{
    private static final long serialVersionUID = 1280015349963924638L;

    public AnnotationException()
    {
        super();
    }

    public AnnotationException(String message)
    {
        super(message);
    }

    public AnnotationException(String aMessage, Throwable aCause)
    {
        super(aMessage, aCause);
    }

    public AnnotationException(Throwable aCause)
    {
        super(aCause);
    }
}
