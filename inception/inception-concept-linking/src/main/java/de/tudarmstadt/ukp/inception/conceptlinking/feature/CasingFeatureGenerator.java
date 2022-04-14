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
package de.tudarmstadt.ukp.inception.conceptlinking.feature;

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY_IS_LOWER_CASE;
import static java.lang.Character.isAlphabetic;
import static java.lang.Character.isLowerCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link EntityLinkingServiceAutoConfiguration#casingFeatureGenerator()}.
 * </p>
 */
public class CasingFeatureGenerator
    implements EntityRankingFeatureGenerator
{
    @Override
    public void apply(CandidateEntity aCandidate)
    {
        aCandidate.get(KEY_QUERY) //
                .map(query -> allAlphabeticCharactersAreLowerCase(query)) //
                .ifPresent(isLower -> aCandidate.put(KEY_QUERY_IS_LOWER_CASE, isLower));
    }

    public static boolean allAlphabeticCharactersAreLowerCase(final CharSequence cs)
    {
        if (isEmpty(cs)) {
            return false;
        }

        int len = cs.length();
        for (int i = 0; i < len; i++) {
            if (isAlphabetic(i) && !isLowerCase(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }
}
