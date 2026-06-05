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
package de.tudarmstadt.ukp.clarin.webanno.model;

/**
 * A marker appended to an {@link AnnotationSet#displayName()} to flag a special status of the data
 * owner - e.g. that they still have annotation data on record but no longer hold the annotator
 * permission in the project.
 */
public enum AnnotationSetMarker
{
    FORMER_ANNOTATOR("former annotator"), //
    DEACTIVATED("deactivated"), //
    MISSING("missing!");

    private final String label;

    AnnotationSetMarker(String aLabel)
    {
        label = aLabel;
    }

    public String getLabel()
    {
        return label;
    }
}
