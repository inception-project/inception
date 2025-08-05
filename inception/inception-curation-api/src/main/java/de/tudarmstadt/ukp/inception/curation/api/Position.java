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
package de.tudarmstadt.ukp.inception.curation.api;

import java.io.Serializable;

import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;

/**
 * Represents a logical position in the text. All annotations considered to be at the same logical
 * position in the document are collected under this. Within the position, there are groups that
 * represent the different configurations of the annotation made by different users.
 */
public interface Position
    extends Comparable<Position>, Serializable
{
    /**
     * @return the type.
     */
    String getType();

    String getCollectionId();

    String getDocumentId();

    String toMinimalString();

    boolean isLinkFeaturePosition();

    /**
     * @return the feature if this is a sub-position for a link feature.
     */
    String getLinkFeature();

    String getLinkRole();

    int getLinkTargetBegin();

    int getLinkTargetEnd();

    /**
     * @return the way in which links are compared and labels for links are generated.
     */
    LinkFeatureMultiplicityMode getLinkFeatureMultiplicityMode();
}
