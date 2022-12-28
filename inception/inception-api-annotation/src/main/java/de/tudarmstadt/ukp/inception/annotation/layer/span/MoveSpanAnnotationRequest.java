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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class MoveSpanAnnotationRequest
    extends SpanAnnotationRequest_ImplBase<MoveSpanAnnotationRequest>
{
    private final AnnotationFS annotation;

    public MoveSpanAnnotationRequest(SourceDocument aDocument, String aUsername, CAS aCas,
            AnnotationFS aAnnotation, int aBegin, int aEnd)
    {
        this(null, aDocument, aUsername, aCas, aAnnotation, aBegin, aEnd);
    }

    private MoveSpanAnnotationRequest(MoveSpanAnnotationRequest aOriginal, SourceDocument aDocument,
            String aUsername, CAS aCas, AnnotationFS aAnnotation, int aBegin, int aEnd)
    {
        super(null, aDocument, aUsername, aCas, aBegin, aEnd);
        annotation = aAnnotation;
    }

    public AnnotationFS getAnnotation()
    {
        return annotation;
    }

    @Override
    public MoveSpanAnnotationRequest changeSpan(int aBegin, int aEnd)
    {
        return new MoveSpanAnnotationRequest(this, getDocument(), getUsername(), getCas(),
                getAnnotation(), aBegin, aEnd);
    }
}
