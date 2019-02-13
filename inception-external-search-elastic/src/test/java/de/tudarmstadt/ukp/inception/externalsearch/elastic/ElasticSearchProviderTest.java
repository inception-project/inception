/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.externalsearch.elastic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.traits.ElasticSearchProviderTraits;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class ElasticSearchProviderTest
{
    private ElasticSearchProvider sut;
    private DocumentRepository repo;
    private ElasticSearchProviderTraits traits;
    
    @Before
    public void setup()
    {
        sut = new ElasticSearchProvider();
        
        repo = new DocumentRepository("dummy", null);
        
        traits = new ElasticSearchProviderTraits();
        traits.setRemoteUrl("http://bart.ukp.informatik.tu-darmstadt.de:9200");
        traits.setIndexName("common-crawl-en");
        traits.setSearchPath("_search");
        traits.setObjectType("texts");
    }
    
    @Test
    public void thatQueryWorks() throws Exception
    {
        List<ExternalSearchResult> results = sut.executeQuery(repo, traits, "shiny");
        
        System.out.println(results);
        
        assertThat(results).isNotEmpty();
    }
    
    @Test
    public void thatDocumentTextCanBeRetrieved() throws Exception
    {
        sut.getDocumentById(repo, traits, "common-crawl-en", "TcBhiGABg9im2MD5uAjq");
    }
}
