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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS;

import java.io.IOException;
import java.text.SimpleDateFormat;
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            throw new IOException("There was a concurrent change to the document. Re-open the "
                    + "document to continue editing (expected: " + sdf.format(stateTimestamp.get())
                    + " actual on storage: " + sdf.format(aDiskTimestamp.get()) + ", delta: "
                    + formatDurationHMS(aDiskTimestamp.get() - stateTimestamp.get()) + ")");
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
