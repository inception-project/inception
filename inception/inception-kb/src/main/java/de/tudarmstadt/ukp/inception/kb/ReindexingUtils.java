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
package de.tudarmstadt.ukp.inception.kb;

import static java.util.Collections.emptyList;
import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReindexingUtils
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Re-indexes the SAIL. In contrast to the {@link LuceneSail#reindex()}, this call maybe a bit
     * slower, but it does not require sorting the results of the reindexQuery - and thus it uses
     * considerably less memory on large knowledge bases as it does not need to read all the data
     * into memory to perform the sort.
     * 
     * @param aSail
     *            the SAIL to be reindexed.
     *
     * @throws SailException
     *             If the Sail could not be reindex
     */
    public static void reindex(LuceneSail aSail) throws SailException
    {
        try {
            var luceneIndex = aSail.getLuceneIndex();

            // Reindex query without ordering
            var reindexQuery = "SELECT ?s ?p ?o ?c WHERE {{?s ?p ?o} UNION {GRAPH ?c {?s ?p ?o.}}}";

            // clear
            LOG.info("Reindexing sail: clearing...");
            luceneIndex.clear();

            LOG.info("Reindexing sail: adding...");
            int statementCommitInterval = 100000;
            long statementsProcessed = 0;

            try {
                luceneIndex.begin();

                var repo = new SailRepository(new NotifyingSailWrapper(aSail.getBaseSail())
                {
                    @Override
                    public void init()
                    {
                        // don't re-initialize the Sail when we initialize the repo
                    }

                    @Override
                    public void shutDown()
                    {
                        // don't shutdown the underlying sail when we shutdown the repo.
                    }
                });

                try (var connection = repo.getConnection()) {
                    var query = connection.prepareTupleQuery(SPARQL, reindexQuery);
                    try (var res = query.evaluate()) {
                        var vf = aSail.getValueFactory();

                        var statements = new ArrayList<Statement>();

                        while (res.hasNext()) {
                            var set = res.next();
                            var r = (Resource) set.getValue("s");
                            var p = (IRI) set.getValue("p");
                            var o = set.getValue("o");
                            var c = (Resource) set.getValue("c");

                            var statement = vf.createStatement(r, p, o, c);
                            statements.add(statement);
                            statementsProcessed++;

                            if (statements.size() >= statementCommitInterval) {
                                luceneIndex.addRemoveStatements(statements, emptyList());
                                luceneIndex.commit();
                                LOG.debug("Committed {} statements to index", statements.size());
                                statements.clear();
                            }
                        }

                        // Commit any remaining statements
                        if (!statements.isEmpty()) {
                            luceneIndex.addRemoveStatements(statements, emptyList());
                            luceneIndex.commit();
                            LOG.debug("Committed {} statements to index", statements.size());
                            statements.clear();
                        }
                    }
                }
                finally {
                    repo.shutDown();
                }

                // commit the changes
                luceneIndex.commit();
                LOG.debug("Committed to index");

                LOG.info("Reindexing sail: done ({} statements processed).", statementsProcessed);
            }
            catch (Exception e) {
                LOG.error("Rolling back", e);
                luceneIndex.rollback();
                throw e;
            }
        }
        catch (Exception e) {
            throw new SailException("Could not reindex LuceneSail: " + e.getMessage(), e);
        }
    }
}
