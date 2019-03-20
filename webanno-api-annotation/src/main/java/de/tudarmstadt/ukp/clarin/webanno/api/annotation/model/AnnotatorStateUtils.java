/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import java.io.IOException;
import java.util.Optional;

public class AnnotatorStateUtils
{
    public static void verifyAndUpdateDocumentTimestamp(AnnotatorState aState,
            Optional<Long> aDiskTimestamp)
        throws IOException
    {
        // If we have a timestamp, then use it to detect if there was a concurrent access
        Optional<Long> stateTimestamp = aState.getAnnotationDocumentTimestamp();
        if (stateTimestamp.isPresent() && aDiskTimestamp.isPresent()
                && aDiskTimestamp.get() > stateTimestamp.get()) {
            throw new IOException("There was a concurrent change to the document. Re-open the "
                    + "document to continue editing.");
        }

        if (aDiskTimestamp.isPresent()) {
            aState.setAnnotationDocumentTimestamp(aDiskTimestamp.get());
        }
    }

    public static void updateDocumentTimestampAfterWrite(AnnotatorState aState,
            Optional<Long> aDiskTimestamp)
        throws IOException
    {
        if (aDiskTimestamp.isPresent()) {
            aState.setAnnotationDocumentTimestamp(aDiskTimestamp.get());
        }
    }
}
