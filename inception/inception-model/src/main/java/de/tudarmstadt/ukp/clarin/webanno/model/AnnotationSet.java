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

import java.util.Objects;

import org.apache.commons.lang3.Validate;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public record AnnotationSet(String id, String displayName) {
    public static final AnnotationSet EXPORT_SET = special("exportCas", "Export");
    public static final AnnotationSet PREDICTION_SET = special("predictionCas", "Prediction");
    public static final AnnotationSet INITIAL_SET = special(INITIAL_CAS_PSEUDO_USER, "Source");
    public static final AnnotationSet CURATION_SET = special(CURATION_USER, "Curator");

    /**
     * @deprecated Use one of the factory methods, e.g. {@link #forUser(User)}
     */
    @Deprecated
    public AnnotationSet(String id)
    {
        this(id, id);

        Validate.notBlank(id, "id must not be blank");
    }

    @Override
    public final boolean equals(Object arg0)
    {
        if (arg0 instanceof AnnotationSet set) {
            return Objects.equals(id, set.id);
        }

        return false;
    }

    @Override
    public final int hashCode()
    {
        return Objects.hash(id);
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
        return new AnnotationSet(aUser.getUsername(), aUser.getUiName());
    }

    public static AnnotationSet forTest(String aName)
    {
        return new AnnotationSet(aName);
    }

    private static AnnotationSet special(String aPurpose, String aDisplayName)
    {
        return new AnnotationSet(aPurpose, aDisplayName);
    }
}
