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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model;

import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.annotationDocumentStateToString;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;

public class RAnnotation
{
    public String user;
    public String state;
    public String timestamp;

    public RAnnotation(AnnotationDocument aAnnotationDocument)
    {
        user = aAnnotationDocument.getUser();
        state = annotationDocumentStateToString(aAnnotationDocument.getState());
        if (aAnnotationDocument.getTimestamp() != null) {
            timestamp = formatTimestamp(aAnnotationDocument.getTimestamp());
        }
    }

    public RAnnotation(String aUser, AnnotationDocumentState aState, Date aTimestamp)
    {
        super();
        user = aUser;
        state = annotationDocumentStateToString(aState);
        if (aTimestamp != null) {
            timestamp = formatTimestamp(aTimestamp);
        }
    }

    private String formatTimestamp(Date aTime)
    {
        return new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssZ").format(aTime);
    }
}
