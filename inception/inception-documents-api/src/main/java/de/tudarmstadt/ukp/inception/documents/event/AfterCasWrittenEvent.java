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
package de.tudarmstadt.ukp.inception.documents.event;

import org.apache.uima.cas.CAS;
import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;

public class AfterCasWrittenEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 686641613168415460L;

    private final AnnotationDocument document;
    private final CAS cas;

    public AfterCasWrittenEvent(Object aSource, AnnotationDocument aDocument, CAS aCas)
    {
        super(aSource);
        document = aDocument;
        cas = aCas;
    }

    public AnnotationDocument getDocument()
    {
        return document;
    }

    public CAS getCas()
    {
        return cas;
    }
}
