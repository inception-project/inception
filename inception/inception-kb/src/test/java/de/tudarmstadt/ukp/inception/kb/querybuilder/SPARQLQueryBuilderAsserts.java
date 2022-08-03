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

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.sparql.ast.ParseException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class SPARQLQueryBuilderAsserts
{
    public static void assertThatChildrenOfExplicitRootCanBeRetrieved(KnowledgeBase aKB,
            Repository aRepository, String aRootClass, int aLimit)
    {
        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder.forClasses(aKB)
                .childrenOf(aRootClass).retrieveLabel().retrieveDescription());

        assertThat(results).isNotEmpty();

        if (aLimit > 0) {
            Random r = new Random();
            List<KBHandle> pool = results;
            int total = pool.size();
            results = new ArrayList<>();
            for (int i = 0; i < aLimit; i++) {
                results.add(pool.remove(r.nextInt(pool.size())));
            }
            System.out.printf("Validating %d of %d results randomly selected:%n", results.size(),
                    total);
        }
        else {
            System.out.printf("Validating all %d results:%n", results.size());
        }

        try {
            assertThat(results).allMatch(_child -> {
                System.out.printf("   %-70s ...", _child.getIdentifier());
                boolean result;
                long start = currentTimeMillis();
                try (RepositoryConnection conn = aRepository.getConnection()) {
                    result = SPARQLQueryBuilder.forClasses(aKB) //
                            .parentsOf(_child.getIdentifier()) //
                            .asHandles(conn, true).stream() //
                            .map(KBHandle::getIdentifier) //
                            .anyMatch(iri -> iri.equals(aRootClass));
                    System.out.printf(" OK   (%6d ms)%n", currentTimeMillis() - start);
                }
                catch (Exception e) {
                    System.out.printf(" FAIL (%6d ms)%n", currentTimeMillis() - start);
                    throw e;
                }
                return result;
            });
        }
        finally {
            System.out.println();
        }
    }

    public static List<KBHandle> asHandles(Repository aRepo, SPARQLQuery aBuilder)
    {
        try (RepositoryConnection conn = aRepo.getConnection()) {
            printQuery(aBuilder);

            long startTime = System.currentTimeMillis();

            List<KBHandle> results = aBuilder.asHandles(conn, true);

            System.out.printf("Results : %d in %dms%n", results.size(),
                    System.currentTimeMillis() - startTime);
            results.stream().limit(10).forEach(r -> System.out.printf("          %s%n", r));
            if (results.size() > 10) {
                System.out.printf("          ... and %d more ...%n", results.size() - 10);
            }

            return results;
        }
        catch (MalformedQueryException e) {
            throw handleParseException(aBuilder, e);
        }
    }

    public static boolean exists(Repository aRepo, SPARQLQuery aBuilder)
    {
        try (RepositoryConnection conn = aRepo.getConnection()) {
            printQuery(aBuilder);

            long startTime = System.currentTimeMillis();

            boolean result = aBuilder.exists(conn, true);

            System.out.printf("Results : %b in %dms%n", result,
                    System.currentTimeMillis() - startTime);

            return result;
        }
        catch (MalformedQueryException e) {
            throw handleParseException(aBuilder, e);
        }
    }

    private static void printQuery(SPARQLQuery aBuilder)
    {
        System.out.printf("Query   : %n");
        Arrays.stream(aBuilder.selectQuery().getQueryString().split("\n"))
                .forEachOrdered(l -> System.out.printf("          %s%n", l));
    }

    @SuppressWarnings("unchecked")
    private static <T extends RuntimeException> T handleParseException(SPARQLQuery aBuilder,
            T aException)
    {
        String[] queryStringLines = aBuilder.selectQuery().getQueryString().split("\n");

        if (aException.getCause() instanceof ParseException) {
            ParseException cause = (ParseException) aException.getCause();
            String message = String.format("Error: %s%n" + "Bad query part starting with: %s%n",
                    aException.getMessage(), queryStringLines[cause.currentToken.beginLine - 1]
                            .substring(cause.currentToken.beginColumn - 1));
            return (T) new MalformedQueryException(message);
        }

        return aException;
    }
}
