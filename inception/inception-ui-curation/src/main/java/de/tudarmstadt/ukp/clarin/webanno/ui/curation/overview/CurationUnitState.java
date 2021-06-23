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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview;

/**
 * An ennumeration to differentiate sentences in a document with different colors so as to easily
 * identify
 *
 */
public enum CurationUnitState
{
    /**
     * No conflicts of annotation in this sentence, no color - null- white
     */
    AGREE("agree"),

    /**
     * Stacked annotations
     */
    STACKED("stacked"),

    /**
     * Incomplete annotations
     */
    INCOMPLETE("incomplete"),

    /**
     * Conflicts of annotation found in this sentence, mark background in red
     */
    DISAGREE("disagree"),

    /**
     * Confirmed annotation.
     */
    CURATED("curated");

    private String cssClass;

    CurationUnitState(String aCssClass)
    {
        cssClass = aCssClass;
    }

    public String getCssClass()
    {
        return cssClass;
    }
}
