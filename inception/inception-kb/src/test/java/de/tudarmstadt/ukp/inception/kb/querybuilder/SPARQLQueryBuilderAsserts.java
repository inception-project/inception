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
import static org.slf4j.event.Level.INFO;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.sparql.ast.ParseException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class SPARQLQueryBuilderAsserts
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void assertThatChildrenOfExplicitRootCanBeRetrieved(KnowledgeBase aKB,
            Repository aRepository, String aRootClass, int aLimit)
    {
        var results = asHandles(aRepository, SPARQLQueryBuilder.forClasses(aKB)
                .childrenOf(aRootClass).retrieveLabel().retrieveDescription());

        assertThat(results).isNotEmpty();

        var buf = new StringWriter();
        var out = new PrintWriter(buf);
        if (aLimit > 0) {
            var r = new Random();
            List<KBHandle> pool = results;
            int total = pool.size();
            results = new ArrayList<>();
            for (int i = 0; i < aLimit; i++) {
                results.add(pool.remove(r.nextInt(pool.size())));
            }
            out.printf("Validating %d of %d results randomly selected:%n", results.size(), total);
        }
        else {
            out.printf("Validating all %d results:%n", results.size());
        }

        assertThat(results).allMatch(_child -> {
            out.printf("   %-70s ...", _child.getIdentifier());
            boolean result;
            long start = currentTimeMillis();
            try (var conn = aRepository.getConnection()) {
                result = SPARQLQueryBuilder.forClasses(aKB) //
                        .parentsOf(_child.getIdentifier()) //
                        .asHandles(conn, true).stream() //
                        .map(KBHandle::getIdentifier) //
                        .anyMatch(iri -> iri.equals(aRootClass));
                out.printf(" OK   (%6d ms)%n", currentTimeMillis() - start);
                LOG.info("{}", buf);
            }
            catch (Exception e) {
                out.printf(" FAIL (%6d ms)%n", currentTimeMillis() - start);
                LOG.error("{}", buf);
                throw e;
            }
            return result;
        });
    }

    public static List<KBHandle> asHandles(Repository aRepo, SPARQLQuery aBuilder)
    {
        try (var conn = aRepo.getConnection()) {
            printQuery(conn, aBuilder);

            var startTime = currentTimeMillis();

            var results = aBuilder.asHandles(conn, true);

            var buf = new StringWriter();
            var out = new PrintWriter(buf);

            out.printf("Results : %d in %dms%n", results.size(), currentTimeMillis() - startTime);
            results.stream().limit(10).forEach(r -> out.printf("          %s%n", r));
            if (results.size() > 10) {
                out.printf("          ... and %d more ...%n", results.size() - 10);
            }

            LOG.info("{}", buf);

            return results;
        }
        catch (MalformedQueryException e) {
            throw handleParseException(aBuilder, e);
        }
    }

    public static boolean exists(Repository aRepo, SPARQLQuery aBuilder)
    {
        try (var conn = aRepo.getConnection()) {
            printQuery(conn, aBuilder);

            var startTime = currentTimeMillis();

            var result = aBuilder.exists(conn, true);

            LOG.info("Results : {} in {}ms", result, currentTimeMillis() - startTime);

            return result;
        }
        catch (MalformedQueryException e) {
            throw handleParseException(aBuilder, e);
        }
    }

    private static void printQuery(RepositoryConnection aConn, SPARQLQuery aBuilder)
    {
        LOG.info("Query   :");
        aBuilder.logQueryString(LOG, INFO, "          ");
    }

    @SuppressWarnings("unchecked")
    private static <T extends RuntimeException> T handleParseException(SPARQLQuery aBuilder,
            T aException)
    {
        if (aException.getCause() instanceof ParseException cause
                && aBuilder instanceof SPARQLQueryBuilder builder) {
            var queryStringLines = builder.selectQuery().getQueryString().split("\n");
            var message = String.format("Error: %s%n" + "Bad query part starting with: %s%n",
                    aException.getMessage(), queryStringLines[cause.currentToken.beginLine - 1]
                            .substring(cause.currentToken.beginColumn - 1));
            return (T) new MalformedQueryException(message);
        }

        return aException;
    }
}
