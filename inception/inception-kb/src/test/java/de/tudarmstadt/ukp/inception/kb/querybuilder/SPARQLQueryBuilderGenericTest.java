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
package de.tudarmstadt.ukp.inception.kb.querybuilder;

import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_NONE;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.restoreSslVerification;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.suspendSslVerification;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderAsserts.asHandles;
import static de.tudarmstadt.ukp.inception.kb.util.TestFixtures.buildKnowledgeBase;
import static de.tudarmstadt.ukp.inception.kb.util.TestFixtures.buildRepository;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

@Tag("slow")
public class SPARQLQueryBuilderGenericTest
{
    private static final List<String> SKIPPED_PROFILES = asList("babel_net");

    public static List<KnowledgeBaseProfile> data() throws Exception
    {
        Map<String, KnowledgeBaseProfile> profiles = KnowledgeBaseProfile
                .readKnowledgeBaseProfiles();

        List<KnowledgeBaseProfile> dataList = new ArrayList<>();
        for (Entry<String, KnowledgeBaseProfile> entry : profiles.entrySet()) {
            if (SKIPPED_PROFILES.contains(entry.getKey())) {
                continue;
            }

            dataList.add(entry.getValue());
        }
        return dataList;
    }

    @ParameterizedTest(name = "{index}: profile {0}")
    @MethodSource("data")
    public void thatRootConceptsCanBeRetrieved(KnowledgeBaseProfile aProfile) throws IOException
    {
        KnowledgeBase kb = buildKnowledgeBase(aProfile);
        Repository repo = buildRepository(aProfile);

        List<KBHandle> roots = asHandles(repo, SPARQLQueryBuilder.forClasses(kb).roots());

        assertThat(roots).isNotEmpty();
    }

    @BeforeEach
    public void setup()
    {
        suspendSslVerification();
    }

    @AfterEach
    public void tearDown()
    {
        restoreSslVerification();
    }

    @ParameterizedTest(name = "{index}: profile {0}")
    @MethodSource("data")
    public void thatChildrenOfRootConceptHaveRootConceptAsParent(KnowledgeBaseProfile aProfile)
        throws IOException
    {
        KnowledgeBase kb = buildKnowledgeBase(aProfile);
        Repository repo = buildRepository(aProfile);

        List<KBHandle> roots = asHandles(repo, SPARQLQueryBuilder.forClasses(kb).roots().limit(3));
        Set<String> rootIdentifiers = roots.stream().map(KBHandle::getIdentifier).collect(toSet());

        assertThat(roots).extracting(KBHandle::getIdentifier).allMatch(_root -> {
            try (RepositoryConnection conn = repo.getConnection()) {
                System.out.printf("R: %s%n", _root);
                List<KBHandle> children = SPARQLQueryBuilder.forClasses(kb).childrenOf(_root)
                        .asHandles(conn, true);

                return children.stream().map(KBHandle::getIdentifier)
                        .allMatch(_child -> SPARQLQueryBuilder.forClasses(kb) //
                                .parentsOf(_child) //
                                .limit(5) //
                                .asHandles(conn, true) //
                                .stream() //
                                .map(KBHandle::getIdentifier)
                                // .map(v -> {
                                // System.out.printf("C: %s%n", v);
                                // return v;
                                // })
                                .anyMatch(rootIdentifiers::contains));
            }
        });
    }

    @ParameterizedTest(name = "{index}: profile: {0}")
    @MethodSource("data")
    public void thatRegexMetaCharactersAreSafe(KnowledgeBaseProfile aProfile) throws IOException
    {
        KnowledgeBase kb = buildKnowledgeBase(aProfile);
        Repository repo = buildRepository(aProfile);

        try (RepositoryConnection conn = repo.getConnection()) {
            SPARQLQueryOptionalElements builder = SPARQLQueryBuilder.forItems(kb)
                    .withLabelMatchingExactlyAnyOf(".[]*+{}()lala").limit(3);

            System.out.printf("Query   : %n");
            Arrays.stream(builder.selectQuery().getQueryString().split("\n"))
                    .forEachOrdered(l -> System.out.printf("          %s%n", l));

            builder.asHandles(conn, true);

            // We don't need an assertion here since we do not expect any results - it is only
            // important that the query does not crash
        }
    }

    @ParameterizedTest(name = "{index}: profile: {0}")
    @MethodSource("data")
    public void thatLineBreaksAndWhitespaceAreSafe_noFts(KnowledgeBaseProfile aProfile)
        throws IOException
    {
        KnowledgeBase kb = buildKnowledgeBase(aProfile);
        Repository repo = buildRepository(aProfile);

        String originalFtsIri = kb.getFullTextSearchIri();

        try (RepositoryConnection conn = repo.getConnection()) {
            kb.setFullTextSearchIri(FTS_NONE.stringValue());

            SPARQLQueryOptionalElements builder = SPARQLQueryBuilder //
                    .forItems(kb) //
                    .withLabelMatchingExactlyAnyOf("Lord\n\r\tLady") //
                    .limit(3);

            System.out.printf("Query   : %n");
            Arrays.stream(builder.selectQuery().getQueryString().split("\n"))
                    .forEachOrdered(l -> System.out.printf("          %s%n", l));

            builder.asHandles(conn, true);

            // We don't need an assertion here since we do not expect any results - it is only
            // important that the query does not crash
        }
        finally {
            kb.setFullTextSearchIri(originalFtsIri);
        }
    }
}
