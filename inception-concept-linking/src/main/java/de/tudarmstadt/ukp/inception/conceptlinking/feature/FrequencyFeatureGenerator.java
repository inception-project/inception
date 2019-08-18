/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.conceptlinking.feature;

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_FREQUENCY;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.PREFIX_WIKIDATA_ENTITY;

import java.io.File;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;
import de.tudarmstadt.ukp.inception.conceptlinking.util.FileUtils;

/**
 * Assigns frequency priors from a pre-defined dictionary.
 */
// @Component
public class FrequencyFeatureGenerator
    implements EntityRankingFeatureGenerator
{
    private Map<String, Integer> entityFrequencyMap;

    public FrequencyFeatureGenerator(RepositoryProperties aRepoProperties)
    {
        entityFrequencyMap = FileUtils.loadEntityFrequencyMap(
                new File(aRepoProperties.getPath(), "/resources/wikidata_entity_freqs.map"));
    }

    @Override
    public void apply(CandidateEntity aCandidate)
    {
        // Set frequency
        if (entityFrequencyMap != null) {
            String key = aCandidate.getIRI();
            key = key.replace(PREFIX_WIKIDATA_ENTITY, "");
            Integer frequency = entityFrequencyMap.get(key);
            if (frequency != null) {
                aCandidate.put(KEY_FREQUENCY, frequency);
            }
        }
    }
}
