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

import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_MARKLOGIC;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderLocalTestScenarios.initRdfsMapping;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Mode;

public class SPARQLQueryBuilderTest
{
    @Test
    public void thatLineBreaksAreSanitized() throws Exception
    {
        var kb = new KnowledgeBase();
        var sut = new SPARQLQueryBuilder(kb, Mode.CLASS);
        sut.caseSensitive(true);

        assertThat(sut.sanitizeQueryString_FTS("Green\n\rGoblin")) //
                .isEqualTo("Green Goblin");
    }

    /**
     * When the label properties have been pre-resolved, labels must be retrieved via the concrete
     * properties used as constant predicates - <i>not</i> by binding a property variable and
     * matching {@code ?subj ?pVar ?m}, and <i>not</i> via the {@code subPropertyOf*} property path.
     * A predicate variable / property path inside the label OPTIONAL is pathologically slow on some
     * engines (e.g. it makes QLever try to allocate gigabytes). Also, with no language configured,
     * no language filter must be emitted at all.
     */
    @Test
    public void thatPreResolvedLabelsAreRetrievedViaConstantPredicates() throws Exception
    {
        var kb = new KnowledgeBase();
        initRdfsMapping(kb);
        kb.setDefaultLanguage(null);
        kb.setMaxResults(100);

        var sut = new SPARQLQueryBuilder(kb, Mode.CLASS);
        sut.withPrefLabelProperties(of("http://www.w3.org/2000/01/rdf-schema#label"));
        sut.roots();
        sut.retrieveLabel().retrieveDescription().retrieveDeprecation();

        var query = sut.selectQuery().getQueryString();

        assertThat(query) //
                .as("label is retrieved via the label IRI as a constant predicate") //
                .contains("<http://www.w3.org/2000/01/rdf-schema#label> ?");
        assertThat(query) //
                .as("no subPropertyOf* property path is embedded into the query") //
                .doesNotContain("subPropertyOf");
        assertThat(query) //
                .as("no language filter is emitted when no language is configured") //
                .doesNotContain("LANGMATCHES").doesNotContain("LANG(");
    }

    private SPARQLQueryBuilder markLogicBuilder()
    {
        var kb = new KnowledgeBase();
        initRdfsMapping(kb);
        kb.setDefaultLanguage("en");
        kb.setMaxResults(100);
        kb.setFullTextSearchIri(FTS_MARKLOGIC.stringValue());
        var sut = new SPARQLQueryBuilder(kb, Mode.ITEM);
        sut.withPrefLabelProperties(of("http://www.w3.org/2000/01/rdf-schema#label"));
        return sut;
    }

    @Test
    public void thatMarkLogicContainingEmitsCtsContainsPerToken() throws Exception
    {
        var query = markLogicBuilder() //
                .withLabelContainingAnyOf("Green Goblin") //
                .selectQuery().getQueryString();

        assertThat(query) //
                .as("the MarkLogic cts namespace is declared") //
                .contains("PREFIX cts: <http://marklogic.com/cts#>");
        assertThat(query) //
                .as("a substring cts:contains / cts:word-query filter is emitted per token") //
                .contains("cts:contains( ?m, cts:word-query( \"*green*\", \"wildcarded\" ) )") //
                .contains("cts:contains( ?m, cts:word-query( \"*goblin*\", \"wildcarded\" ) )");
    }

    @Test
    public void thatMarkLogicStartingWithWildcardsTheLastToken() throws Exception
    {
        var query = markLogicBuilder() //
                .withLabelStartingWith("Green Gob") //
                .selectQuery().getQueryString();

        assertThat(query) //
                .as("the unfinished last token becomes a wildcarded prefix query") //
                .contains("cts:contains( ?m, cts:word-query( \"green\" ) )") //
                .contains("cts:word-query( \"gob*\", \"wildcarded\" )");
    }
}
