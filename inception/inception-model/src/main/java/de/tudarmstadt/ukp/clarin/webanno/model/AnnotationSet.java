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

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;

import org.apache.commons.lang3.Validate;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public record AnnotationSet(String id) {
    public static final AnnotationSet EXPORT_SET = forSpecialPurpose("exportCas");
    public static final AnnotationSet PREDICTION_SET = forSpecialPurpose("predictionCas");
    public static final AnnotationSet INITIAL_SET = forSpecialPurpose(INITIAL_CAS_PSEUDO_USER);
    public static final AnnotationSet CURATION_SET = forSpecialPurpose(CURATION_USER);

    @Deprecated
    public AnnotationSet(String id)
    {
        Validate.notBlank(id, "id must not be blank");

        this.id = id;
    }

    @Override
    public final String toString()
    {
        return id;
    }

    public static AnnotationSet forUser(String aUsername)
    {
        return new AnnotationSet(aUsername);
    }

    public static AnnotationSet forUser(User aUser)
    {
        return forUser(aUser.getUsername());
    }

    public static AnnotationSet forTest(String aName)
    {
        return new AnnotationSet(aName);
    }

    public static AnnotationSet forSpecialPurpose(String aPurpose)
    {
        return new AnnotationSet(aPurpose);
    }
}
