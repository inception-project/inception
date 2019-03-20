/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroRemoteApiController;

public class RAnnotation
{
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssZ");
    
    public String user;
    public String state;
    public String timestamp;
    
    public RAnnotation(AnnotationDocument aAnnotationDocument)
    {
        user = aAnnotationDocument.getUser();
        state = AeroRemoteApiController
                .annotationDocumentStateToString(aAnnotationDocument.getState());
        if (aAnnotationDocument.getTimestamp() != null) {
            timestamp = FORMAT.format(aAnnotationDocument.getTimestamp());
        }
    }
    
    public RAnnotation(String aUser, AnnotationDocumentState aState, Date aTimestamp)
    {
        super();
        user = aUser;
        state = AeroRemoteApiController.annotationDocumentStateToString(aState);
        if (aTimestamp != null) {
            timestamp = FORMAT.format(aTimestamp);
        }
    }
}
