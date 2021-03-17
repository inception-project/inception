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
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderAsserts.asHandles;
import static de.tudarmstadt.ukp.inception.kb.util.TestFixtures.buildKnowledgeBase;
import static de.tudarmstadt.ukp.inception.kb.util.TestFixtures.buildRepository;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import nl.ru.test.category.SlowTests;

@Category(SlowTests.class)
@RunWith(Parameterized.class)
public class SPARQLQueryBuilderGenericTest
{
    private static final List<String> SKIPPED_PROFILES = asList("babel_net", "zbw-gnd");

    @Parameterized.Parameters(name = "KB = {0}")
    public static List<Object[]> data() throws Exception
    {
        Map<String, KnowledgeBaseProfile> profiles = KnowledgeBaseProfile
                .readKnowledgeBaseProfiles();

        List<Object[]> dataList = new ArrayList<>();
        for (Entry<String, KnowledgeBaseProfile> entry : profiles.entrySet()) {
            if (SKIPPED_PROFILES.contains(entry.getKey())) {
                continue;
            }

            dataList.add(new Object[] { entry.getKey(), entry.getValue() });
        }
        return dataList;
    }

    private final String profileName;
    private final KnowledgeBaseProfile profile;

    private KnowledgeBase kb;
    private Repository repo;

    public SPARQLQueryBuilderGenericTest(String aProfileName, KnowledgeBaseProfile aProfile)
        throws Exception
    {
        profileName = aProfileName;
        profile = aProfile;
    }

    @BeforeEach
    public void setup() throws Exception
    {
        // Force POST request instead of GET request
        // System.setProperty(SPARQLProtocolSession.MAXIMUM_URL_LENGTH_PARAM, "100");

        kb = buildKnowledgeBase(profile);
        repo = buildRepository(profile);
    }

    @Test
    public void thatRootConceptsCanBeRetrieved()
    {
        List<KBHandle> roots = asHandles(repo, SPARQLQueryBuilder.forClasses(kb).roots());

        assertThat(roots).isNotEmpty();
    }

    @Test
    public void thatChildrenOfRootConceptHaveRootConceptAsParent()
    {
        List<KBHandle> roots = asHandles(repo, SPARQLQueryBuilder.forClasses(kb).roots().limit(3));
        Set<String> rootIdentifiers = roots.stream().map(KBHandle::getIdentifier).collect(toSet());

        assertThat(roots).extracting(KBHandle::getIdentifier).allMatch(_root -> {
            try (RepositoryConnection conn = repo.getConnection()) {
                System.out.printf("R: %s%n", _root);
                List<KBHandle> children = SPARQLQueryBuilder.forClasses(kb).childrenOf(_root)
                        .asHandles(conn, true);

                return children.stream().map(KBHandle::getIdentifier)
                        .allMatch(_child -> SPARQLQueryBuilder.forClasses(kb).parentsOf(_child)
                                .limit(5).asHandles(conn, true).stream()
                                .map(KBHandle::getIdentifier)
                                // .map(v -> {
                                // System.out.printf("C: %s%n", v);
                                // return v;
                                // })
                                .anyMatch(iri -> rootIdentifiers.contains(iri)));
            }
        });
    }

    @Test
    public void thatRegexMetaCharactersAreSafe()
    {
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

    @Test
    public void thatLineBreaksAndWhitespaceAreSafe_noFts()
    {
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
