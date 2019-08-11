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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior;

/**
 * Represents a logical position in the text. All annotations considered to be at the same
 * logical position in the document are collected under this. Within the position, there are
 * groups that represent the different configurations of the annotation made by different users.
 */
public interface Position extends Comparable<Position>
{
    /**
     * @return the CAS id.
     */
    int getCasId();
    
    /**
     * @return the type.
     */
    String getType();
    
    /**
     * @return the feature if this is a sub-position for a link feature.
     */
    String getFeature();
    
    String getRole();
    
    int getLinkTargetBegin();
    
    int getLinkTargetEnd();
    
    /**
     * Get the way in which links are compared and labels for links are generated.
     */
    LinkCompareBehavior getLinkCompareBehavior();
    
    String getCollectionId();
    String getDocumentId();
    
    String toMinimalString();
}
