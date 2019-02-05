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

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.cluster.ExternalSearchSentenceClusterer;
import de.tudarmstadt.ukp.inception.externalsearch.cluster.ExternalSearchSentenceExtractor;

public class ProcessTest
{
    @Test
    public void test() throws Exception {
        // Parameters
        String queryword = "test";
    
        // Create artificial list of external results
        List<ExternalSearchResult> externalSearchResultList = new ArrayList<>();
        DirectoryStream<Path> directoryStream = newDirectoryStream(
                Paths.get("src/test/resources/texts"));
        for (Path p : directoryStream) {
            ExternalSearchResult externalSearchResult = new ExternalSearchResult();
            externalSearchResult.setText(new String(readAllBytes(p), UTF_8));
            externalSearchResultList.add(externalSearchResult);
        }
        
        // Extract relevant sentences (relevant to query) from external results
        ExternalSearchSentenceExtractor extractor =
                new ExternalSearchSentenceExtractor(externalSearchResultList, queryword);
        List<Triple<String, Double, String>> relevantSentences = extractor.extractSentences();
    
        // Assertions checking that relevant sentences have been extracted correctly
        assertThat(relevantSentences).hasSize(5);
        assertThat(relevantSentences).extracting(Triple::getMiddle).allMatch(score -> score > 0.0);
    
        // Cluster relevant sentences
        ExternalSearchSentenceClusterer clusterer = new ExternalSearchSentenceClusterer();
        clusterer.cluster(relevantSentences);
    
        // Assertion checking that clustering of relevant sentences works correctly
        assertThat(clusterer.getSentenceClusters().size()).isGreaterThan(0);
    }
}
