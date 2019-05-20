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

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_ID_RANK;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_VIRTUOSO;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.PREFIX_WIKIDATA_ENTITY;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.UKP_WIKIDATA_SPARQL_ENDPOINT;
import static de.tudarmstadt.ukp.inception.kb.RepositoryType.REMOTE;

import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

@Component
public class WikidataIdRankFeatureGenerator
    implements EntityRankingFeatureGenerator
{
    private @Autowired KnowledgeBaseService kbService;

    @Override
    public void apply(CandidateEntity aCandidate)
    {
        KnowledgeBase kb = aCandidate.getHandle().getKB();

        // For UKP Wikidata
        if (kb.getType() == REMOTE && FTS_VIRTUOSO.equals(kb.getFullTextSearchIri())) {
            RepositoryImplConfig cfg = kbService.getKnowledgeBaseConfig(kb);
            if (UKP_WIKIDATA_SPARQL_ENDPOINT
                    .equals(((SPARQLRepositoryConfig) cfg).getQueryEndpointUrl())) {
                String wikidataId = aCandidate.getIRI().replace(PREFIX_WIKIDATA_ENTITY, "");
                aCandidate.put(KEY_ID_RANK, Math.log(Double.parseDouble(wikidataId.substring(1))));
            }
        }
    }
}
