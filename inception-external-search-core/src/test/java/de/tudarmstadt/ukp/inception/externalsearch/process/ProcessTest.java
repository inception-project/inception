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
package de.tudarmstadt.ukp.inception.externalsearch.process;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.readAllBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.cluster.ExternalSearchSentenceClusterer;
import de.tudarmstadt.ukp.inception.externalsearch.cluster.ExternalSearchSentenceExtractor;
import de.tudarmstadt.ukp.inception.externalsearch.cluster.ExtractedUnit;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class ProcessTest
{
    private @Mock ExternalSearchService externalSearchService;
    private @Mock DocumentRepository documentRepository;
    private List<ExternalSearchResult> externalSearchResultList = new ArrayList<>();
    private ExternalSearchSentenceExtractor extractor;
    
    
    @Before
    public void setUp() throws Exception {
        initMocks(this);
    
        // Create artificial list of external results
        DirectoryStream<Path> directoryStream = newDirectoryStream(
                Paths.get("src/test/resources/texts"));
        
        for (Path p : directoryStream) {
            ExternalSearchResult externalSearchResult =
                    new ExternalSearchResult(documentRepository, "", p.toString());
            externalSearchResultList.add(externalSearchResult);
            when(externalSearchService.getDocumentText(documentRepository, "", p.toString())).
                    thenReturn(new String(readAllBytes(p), UTF_8));
        }
    
        String query = "test";
        extractor = new ExternalSearchSentenceExtractor(
                externalSearchResultList, externalSearchService, query);
    }
    
    @Test
    public void test() throws Exception {
        // Extract sentences relevant to query from external results
        List<ExtractedUnit> relevantSentences = extractor.extractSentences();
    
        // Assertions checking that relevant sentences have been extracted correctly
        assertThat(relevantSentences).hasSize(5);
        assertThat(relevantSentences).extracting(ExtractedUnit::getScore).
                allMatch(score -> score > 0.0);
    
        // Assertion checking that clustering of relevant sentences works correctly
        assertThat(new ExternalSearchSentenceClusterer().
                getSentenceClusters(relevantSentences).size()).isGreaterThan(0);
    }
}
