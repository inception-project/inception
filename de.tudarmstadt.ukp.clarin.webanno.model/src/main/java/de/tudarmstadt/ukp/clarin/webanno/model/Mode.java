/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.model;

/**
 * Subjects of interest, either annotation or curation.
 *
 * @author Seid Muhie Yimam
 *
 */
public enum Mode
{
    /**
     * This mode is used when the user is in the annotation page of WebAnno and perform any of the
     * tasks such as creating annotation document, setting annotation preference, exporting file and
     * so on.
     *
     */
    ANNOTATION,
    /**
     * This mode is used when the user is in the curation page of WebAnno and perform any of the
     * tasks such as creating curation document, setting annotation preference, exporting file and
     * so on.
     *
     */
    CURATION,
    /**
     * This mode is used when the user is in the curation page of WebAnno but makes an explicit
     * annotation in addition to adjudicating annotations from users.
     */
    MERGE;
}
